package cl.esen.tienda.controller;

import cl.esen.tienda.config.OpenApiConfig;
import cl.esen.tienda.dto.CategoriaRequest;
import cl.esen.tienda.dto.CategoriaResponse;
import cl.esen.tienda.service.CategoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categorias")
@Tag(name = "Categorías", description = "Lectura pública; creación, edición y eliminación solo ADMIN")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @GetMapping
    @Operation(summary = "Lista todas las categorías")
    public List<CategoriaResponse> listar() {
        return categoriaService.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene una categoría por id")
    public CategoriaResponse obtener(@PathVariable Long id) {
        return categoriaService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea una categoría (ADMIN)", security = @SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT))
    public CategoriaResponse crear(@Valid @RequestBody CategoriaRequest request) {
        return categoriaService.crear(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza una categoría (ADMIN)", security = @SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT))
    public CategoriaResponse actualizar(@PathVariable Long id, @Valid @RequestBody CategoriaRequest request) {
        return categoriaService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina una categoría sin productos (ADMIN)",
            security = @SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT))
    public void eliminar(@PathVariable Long id) {
        categoriaService.eliminar(id);
    }
}
