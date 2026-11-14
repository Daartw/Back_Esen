package cl.esen.tienda.controller;

import cl.esen.tienda.config.OpenApiConfig;
import cl.esen.tienda.dto.AuthResponse;
import cl.esen.tienda.dto.LoginRequest;
import cl.esen.tienda.dto.RefreshRequest;
import cl.esen.tienda.dto.RegistroRequest;
import cl.esen.tienda.dto.UsuarioResponse;
import cl.esen.tienda.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticación", description = "Inicio de sesión, registro y renovación de tokens JWT")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Inicia sesión y entrega access token y refresh token")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra un cliente nuevo y lo deja con sesión iniciada")
    public AuthResponse registro(@Valid @RequestBody RegistroRequest request) {
        return authService.registrar(request);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renueva los tokens a partir de un refresh token válido")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.renovar(request.refreshToken());
    }

    @GetMapping("/me")
    @Operation(summary = "Devuelve el usuario dueño del token",
            security = @SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT))
    public UsuarioResponse me(Authentication authentication) {
        return authService.perfil(authentication.getName());
    }
}
