package cl.esen.tienda.controller;

import cl.esen.tienda.config.OpenApiConfig;
import cl.esen.tienda.dto.UsuarioRequest;
import cl.esen.tienda.dto.UsuarioResponse;
import cl.esen.tienda.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/usuarios")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT)
@Tag(name = "Usuarios", description = "Mantenedor de usuarios, solo ADMIN")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    @Operation(summary = "Lista todos los usuarios")
    public List<UsuarioResponse> listar() {
        return usuarioService.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un usuario por id")
    public UsuarioResponse obtener(@PathVariable Long id) {
        return usuarioService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea un usuario con cualquier rol")
    public UsuarioResponse crear(@Valid @RequestBody UsuarioRequest request) {
        return usuarioService.crear(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza un usuario; la contraseña vacía conserva la actual")
    public UsuarioResponse actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioRequest request,
                                      Authentication authentication) {
        return usuarioService.actualizar(id, request, authentication.getName());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina un usuario; si tiene boletas solo lo desactiva")
    public void eliminar(@PathVariable Long id, Authentication authentication) {
        usuarioService.eliminar(id, authentication.getName());
    }
}
