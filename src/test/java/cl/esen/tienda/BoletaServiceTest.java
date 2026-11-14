package cl.esen.tienda;

import cl.esen.tienda.dto.BoletaRequest;
import cl.esen.tienda.dto.BoletaResponse;
import cl.esen.tienda.dto.ItemCompraRequest;
import cl.esen.tienda.exception.ReglaNegocioException;
import cl.esen.tienda.model.Boleta;
import cl.esen.tienda.model.Categoria;
import cl.esen.tienda.model.EstadoBoleta;
import cl.esen.tienda.model.Producto;
import cl.esen.tienda.model.Rol;
import cl.esen.tienda.model.Usuario;
import cl.esen.tienda.repository.BoletaRepository;
import cl.esen.tienda.repository.ProductoRepository;
import cl.esen.tienda.repository.UsuarioRepository;
import cl.esen.tienda.service.BoletaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pruebas unitarias de la logica de negocio de ventas (sin base de datos). */
@ExtendWith(MockitoExtension.class)
class BoletaServiceTest {

    @Mock
    private BoletaRepository boletaRepository;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @InjectMocks
    private BoletaService boletaService;

    private Usuario cliente;
    private Producto hoodie;

    @BeforeEach
    void preparar() {
        cliente = new Usuario();
        cliente.setId(1L);
        cliente.setNombre("Camila");
        cliente.setApellido("Soto");
        cliente.setEmail("cliente@esen.cl");
        cliente.setRol(Rol.CLIENTE);

        Categoria categoria = new Categoria("Hoodies", null);
        categoria.setId(1L);
        hoodie = new Producto();
        hoodie.setId(10L);
        hoodie.setNombre("Hoodie Esencial");
        hoodie.setPrecio(11900);
        hoodie.setStock(5);
        hoodie.setCategoria(categoria);
    }

    @Test
    void crearCalculaTotalConPrecioDeLaBaseYDescuentaStock() {
        when(usuarioRepository.findByEmailIgnoreCase("cliente@esen.cl")).thenReturn(Optional.of(cliente));
        when(productoRepository.buscarParaVenta(10L)).thenReturn(Optional.of(hoodie));
        when(boletaRepository.save(any(Boleta.class))).thenAnswer(inv -> {
            Boleta b = inv.getArgument(0);
            b.setId(7L);
            return b;
        });

        BoletaResponse respuesta = boletaService.crear(
                new BoletaRequest("Los Leones 220", List.of(new ItemCompraRequest(10L, 2))), "cliente@esen.cl");

        assertEquals(23800, respuesta.total());
        assertEquals(20000, respuesta.neto());
        assertEquals(3800, respuesta.iva());
        assertEquals("ESN-000007", respuesta.numero());
        assertEquals(EstadoBoleta.PAGADA, respuesta.estado());
        assertEquals(3, hoodie.getStock());
    }

    @Test
    void crearSumaCantidadesDelMismoProducto() {
        when(usuarioRepository.findByEmailIgnoreCase("cliente@esen.cl")).thenReturn(Optional.of(cliente));
        when(productoRepository.buscarParaVenta(10L)).thenReturn(Optional.of(hoodie));
        when(boletaRepository.save(any(Boleta.class))).thenAnswer(inv -> {
            Boleta b = inv.getArgument(0);
            b.setId(8L);
            return b;
        });

        BoletaResponse respuesta = boletaService.crear(new BoletaRequest("Los Leones 220",
                List.of(new ItemCompraRequest(10L, 1), new ItemCompraRequest(10L, 2))), "cliente@esen.cl");

        assertEquals(1, respuesta.detalles().size());
        assertEquals(3, respuesta.detalles().get(0).cantidad());
        assertEquals(2, hoodie.getStock());
    }

    @Test
    void crearRechazaLaCompraSiNoHayStockSuficiente() {
        when(usuarioRepository.findByEmailIgnoreCase("cliente@esen.cl")).thenReturn(Optional.of(cliente));
        when(productoRepository.buscarParaVenta(10L)).thenReturn(Optional.of(hoodie));

        BoletaRequest request = new BoletaRequest("Los Leones 220", List.of(new ItemCompraRequest(10L, 6)));

        assertThrows(ReglaNegocioException.class, () -> boletaService.crear(request, "cliente@esen.cl"));
        assertEquals(5, hoodie.getStock());
        verify(boletaRepository, never()).save(any());
    }

    @Test
    void crearRechazaProductosInactivos() {
        hoodie.setActivo(false);
        when(usuarioRepository.findByEmailIgnoreCase("cliente@esen.cl")).thenReturn(Optional.of(cliente));
        when(productoRepository.buscarParaVenta(10L)).thenReturn(Optional.of(hoodie));

        BoletaRequest request = new BoletaRequest("Los Leones 220", List.of(new ItemCompraRequest(10L, 1)));

        assertThrows(ReglaNegocioException.class, () -> boletaService.crear(request, "cliente@esen.cl"));
    }
}
