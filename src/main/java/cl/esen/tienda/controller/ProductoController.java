package cl.esen.tienda.controller;

import cl.esen.tienda.config.OpenApiConfig;
import cl.esen.tienda.dto.ProductoRequest;
import cl.esen.tienda.dto.ProductoResponse;
import cl.esen.tienda.service.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/productos")
@Tag(name = "Productos", description = "Lectura pública; creación, edición y eliminación solo ADMIN")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping
    @Operation(summary = "Lista productos, con filtros opcionales por categoría y nombre",
            description = "Los productos inactivos solo se incluyen si quien consulta es ADMIN o VENDEDOR "
                    + "y envía incluirInactivos=true.")
    public List<ProductoResponse> listar(@RequestParam(required = false) Long categoriaId,
                                         @RequestParam(required = false) String q,
                                         @RequestParam(defaultValue = "false") boolean incluirInactivos,
                                         Authentication authentication) {
        return productoService.listar(categoriaId, q, incluirInactivos && esPersonalInterno(authentication));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene el detalle de un producto")
    public ProductoResponse obtener(@PathVariable Long id) {
        return productoService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea un producto (ADMIN)", security = @SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT))
    public ProductoResponse crear(@Valid @RequestBody ProductoRequest request) {
        return productoService.crear(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza un producto (ADMIN)", security = @SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT))
    public ProductoResponse actualizar(@PathVariable Long id, @Valid @RequestBody ProductoRequest request) {
        return productoService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina un producto; si tiene ventas solo lo desactiva (ADMIN)",
            security = @SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT))
    public void eliminar(@PathVariable Long id) {
        productoService.eliminar(id);
    }

    private boolean esPersonalInterno(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_VENDEDOR"));
    }
}
