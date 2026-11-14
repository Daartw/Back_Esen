package cl.esen.tienda.service;

import cl.esen.tienda.dto.ProductoRequest;
import cl.esen.tienda.dto.ProductoResponse;
import cl.esen.tienda.exception.RecursoNoEncontradoException;
import cl.esen.tienda.model.Producto;
import cl.esen.tienda.repository.DetalleBoletaRepository;
import cl.esen.tienda.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final DetalleBoletaRepository detalleBoletaRepository;
    private final CategoriaService categoriaService;

    public ProductoService(ProductoRepository productoRepository, DetalleBoletaRepository detalleBoletaRepository,
                           CategoriaService categoriaService) {
        this.productoRepository = productoRepository;
        this.detalleBoletaRepository = detalleBoletaRepository;
        this.categoriaService = categoriaService;
    }

    /**
     * @param incluirInactivos true solo para el panel interno; la tienda siempre ve productos activos.
     */
    @Transactional(readOnly = true)
    public List<ProductoResponse> listar(Long categoriaId, String texto, boolean incluirInactivos) {
        String filtro = texto == null ? "" : texto.trim();
        return productoRepository.buscar(categoriaId, filtro, incluirInactivos).stream()
                .map(ProductoResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductoResponse obtener(Long id) {
        return ProductoResponse.de(buscar(id));
    }

    @Transactional
    public ProductoResponse crear(ProductoRequest request) {
        Producto producto = new Producto();
        copiarDatos(request, producto);
        return ProductoResponse.de(productoRepository.save(producto));
    }

    @Transactional
    public ProductoResponse actualizar(Long id, ProductoRequest request) {
        Producto producto = buscar(id);
        copiarDatos(request, producto);
        return ProductoResponse.de(producto);
    }

    /**
     * Un producto que ya fue vendido aparece en boletas, por lo que no se borra
     * fisicamente: se desactiva y deja de mostrarse en la tienda.
     */
    @Transactional
    public void eliminar(Long id) {
        Producto producto = buscar(id);
        if (detalleBoletaRepository.existsByProductoId(id)) {
            producto.setActivo(false);
        } else {
            productoRepository.delete(producto);
        }
    }

    private void copiarDatos(ProductoRequest request, Producto producto) {
        producto.setNombre(request.nombre().trim());
        producto.setDescripcion(request.descripcion());
        producto.setPrecio(request.precio());
        producto.setStock(request.stock());
        producto.setColor(request.color());
        producto.setImagenUrl(request.imagenUrl());
        producto.setActivo(request.activo() == null || request.activo());
        producto.setCategoria(categoriaService.buscar(request.categoriaId()));
    }

    private Producto buscar(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el producto con id " + id));
    }
}
