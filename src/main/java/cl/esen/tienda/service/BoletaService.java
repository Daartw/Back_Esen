package cl.esen.tienda.service;

import cl.esen.tienda.dto.BoletaRequest;
import cl.esen.tienda.dto.BoletaResponse;
import cl.esen.tienda.dto.ItemCompraRequest;
import cl.esen.tienda.exception.RecursoNoEncontradoException;
import cl.esen.tienda.exception.ReglaNegocioException;
import cl.esen.tienda.model.Boleta;
import cl.esen.tienda.model.DetalleBoleta;
import cl.esen.tienda.model.EstadoBoleta;
import cl.esen.tienda.model.Producto;
import cl.esen.tienda.model.Usuario;
import cl.esen.tienda.repository.BoletaRepository;
import cl.esen.tienda.repository.ProductoRepository;
import cl.esen.tienda.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Logica de negocio de las ventas: stock, totales, IVA y estados de la boleta. */
@Service
public class BoletaService {

    /** IVA chileno. Los precios de la tienda ya lo incluyen. */
    private static final double TASA_IVA = 0.19;

    private final BoletaRepository boletaRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;

    public BoletaService(BoletaRepository boletaRepository, ProductoRepository productoRepository,
                         UsuarioRepository usuarioRepository) {
        this.boletaRepository = boletaRepository;
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Registra una compra. Todo ocurre en una sola transaccion: si un producto no
     * tiene stock se revierte la compra completa y no se descuenta nada.
     */
    @Transactional
    public BoletaResponse crear(BoletaRequest request, String emailComprador) {
        Usuario comprador = usuarioRepository.findByEmailIgnoreCase(emailComprador)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario autenticado"));

        Boleta boleta = new Boleta();
        boleta.setUsuario(comprador);
        boleta.setFecha(LocalDateTime.now());
        boleta.setEstado(EstadoBoleta.PAGADA);
        boleta.setDireccionEnvio(request.direccionEnvio().trim());

        int total = 0;
        for (Map.Entry<Long, Integer> item : agruparPorProducto(request.items()).entrySet()) {
            Producto producto = productoRepository.buscarParaVenta(item.getKey())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "No existe el producto con id " + item.getKey()));
            int cantidad = item.getValue();

            if (!producto.isActivo()) {
                throw new ReglaNegocioException("El producto \"" + producto.getNombre() + "\" ya no está disponible");
            }
            if (producto.getStock() < cantidad) {
                throw new ReglaNegocioException("Stock insuficiente para \"" + producto.getNombre()
                        + "\": quedan " + producto.getStock() + " unidades");
            }
            producto.setStock(producto.getStock() - cantidad);

            // El precio se toma de la base de datos, nunca del frontend.
            DetalleBoleta detalle = new DetalleBoleta();
            detalle.setProducto(producto);
            detalle.setCantidad(cantidad);
            detalle.setPrecioUnitario(producto.getPrecio());
            detalle.setSubtotal(producto.getPrecio() * cantidad);
            boleta.agregarDetalle(detalle);
            total += detalle.getSubtotal();
        }

        int neto = (int) Math.round(total / (1 + TASA_IVA));
        boleta.setTotal(total);
        boleta.setNeto(neto);
        boleta.setIva(total - neto);

        Boleta guardada = boletaRepository.save(boleta);
        guardada.setNumero(String.format("ESN-%06d", guardada.getId()));
        return BoletaResponse.de(guardada);
    }

    @Transactional(readOnly = true)
    public List<BoletaResponse> listar() {
        return boletaRepository.findAllByOrderByFechaDesc().stream().map(BoletaResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public List<BoletaResponse> listarDeUsuario(String email) {
        return boletaRepository.findByUsuarioEmailIgnoreCaseOrderByFechaDesc(email).stream()
                .map(BoletaResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public BoletaResponse obtener(Long id) {
        return BoletaResponse.de(buscar(id));
    }

    /** Cambia el estado. Al anular se devuelve el stock de cada producto. */
    @Transactional
    public BoletaResponse cambiarEstado(Long id, EstadoBoleta nuevoEstado) {
        Boleta boleta = buscar(id);
        if (boleta.getEstado() == EstadoBoleta.ANULADA) {
            throw new ReglaNegocioException("Una boleta anulada no puede cambiar de estado");
        }
        if (nuevoEstado == EstadoBoleta.ANULADA) {
            if (boleta.getEstado() == EstadoBoleta.ENTREGADA) {
                throw new ReglaNegocioException("No se puede anular una boleta ya entregada");
            }
            for (DetalleBoleta detalle : boleta.getDetalles()) {
                Producto producto = detalle.getProducto();
                producto.setStock(producto.getStock() + detalle.getCantidad());
            }
        }
        boleta.setEstado(nuevoEstado);
        return BoletaResponse.de(boleta);
    }

    /** Solo se eliminan boletas anuladas: una venta vigente es un registro contable. */
    @Transactional
    public void eliminar(Long id) {
        Boleta boleta = buscar(id);
        if (boleta.getEstado() != EstadoBoleta.ANULADA) {
            throw new ReglaNegocioException("Solo se pueden eliminar boletas anuladas");
        }
        boletaRepository.delete(boleta);
    }

    /** Si el mismo producto llega repetido en el carrito, se suman sus cantidades. */
    private Map<Long, Integer> agruparPorProducto(List<ItemCompraRequest> items) {
        Map<Long, Integer> cantidades = new LinkedHashMap<>();
        for (ItemCompraRequest item : items) {
            cantidades.merge(item.productoId(), item.cantidad(), Integer::sum);
        }
        return cantidades;
    }

    private Boleta buscar(Long id) {
        return boletaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la boleta con id " + id));
    }
}
