package cl.esen.tienda.controller;

import cl.esen.tienda.config.OpenApiConfig;
import cl.esen.tienda.dto.BoletaRequest;
import cl.esen.tienda.dto.BoletaResponse;
import cl.esen.tienda.dto.EstadoBoletaRequest;
import cl.esen.tienda.service.BoletaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/boletas")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT)
@Tag(name = "Boletas", description = "Órdenes de compra con su detalle")
public class BoletaController {

    private final BoletaService boletaService;

    public BoletaController(BoletaService boletaService) {
        this.boletaService = boletaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra una compra y descuenta stock (CLIENTE, ADMIN)")
    public BoletaResponse crear(@Valid @RequestBody BoletaRequest request, Authentication authentication) {
        return boletaService.crear(request, authentication.getName());
    }

    @GetMapping("/mias")
    @Operation(summary = "Lista las compras del usuario autenticado")
    public List<BoletaResponse> misBoletas(Authentication authentication) {
        return boletaService.listarDeUsuario(authentication.getName());
    }

    @GetMapping
    @Operation(summary = "Lista todas las boletas (ADMIN, VENDEDOR)")
    public List<BoletaResponse> listar() {
        return boletaService.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene una boleta con su detalle (ADMIN, VENDEDOR)")
    public BoletaResponse obtener(@PathVariable Long id) {
        return boletaService.obtener(id);
    }

    @PutMapping("/{id}/estado")
    @Operation(summary = "Cambia el estado de la boleta; al anular repone el stock (ADMIN)")
    public BoletaResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody EstadoBoletaRequest request) {
        return boletaService.cambiarEstado(id, request.estado());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina una boleta anulada (ADMIN)")
    public void eliminar(@PathVariable Long id) {
        boletaService.eliminar(id);
    }
}
