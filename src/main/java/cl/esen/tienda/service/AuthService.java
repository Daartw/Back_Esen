package cl.esen.tienda.service;

import cl.esen.tienda.dto.AuthResponse;
import cl.esen.tienda.dto.LoginRequest;
import cl.esen.tienda.dto.RegistroRequest;
import cl.esen.tienda.dto.UsuarioResponse;
import cl.esen.tienda.exception.ConflictoException;
import cl.esen.tienda.model.Rol;
import cl.esen.tienda.model.Usuario;
import cl.esen.tienda.repository.UsuarioRepository;
import cl.esen.tienda.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // El mismo mensaje para email inexistente y clave erronea: no revela que cuentas existen.
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(request.email().trim())
                .filter(Usuario::isActivo)
                .filter(u -> passwordEncoder.matches(request.password(), u.getPassword()))
                .orElseThrow(() -> new BadCredentialsException("Email o contraseña incorrectos"));
        return emitirTokens(usuario);
    }

    @Transactional
    public AuthResponse registrar(RegistroRequest request) {
        String email = request.email().trim().toLowerCase();
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictoException("Ya existe una cuenta con ese email");
        }
        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre().trim());
        usuario.setApellido(request.apellido().trim());
        usuario.setEmail(email);
        usuario.setPassword(passwordEncoder.encode(request.password()));
        usuario.setDireccion(request.direccion());
        usuario.setRol(Rol.CLIENTE);
        return emitirTokens(usuarioRepository.save(usuario));
    }

    /** Renovacion: con un refresh token valido entrega un par de tokens nuevo. */
    @Transactional(readOnly = true)
    public AuthResponse renovar(String refreshToken) {
        try {
            Claims claims = jwtService.validar(refreshToken, JwtService.TIPO_REFRESH);
            Usuario usuario = usuarioRepository.findByEmailIgnoreCase(claims.getSubject())
                    .filter(Usuario::isActivo)
                    .orElseThrow(() -> new BadCredentialsException("La sesión ya no es válida"));
            return emitirTokens(usuario);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BadCredentialsException("La sesión expiró, inicia sesión nuevamente");
        }
    }

    @Transactional(readOnly = true)
    public UsuarioResponse perfil(String email) {
        return usuarioRepository.findByEmailIgnoreCase(email)
                .map(UsuarioResponse::de)
                .orElseThrow(() -> new BadCredentialsException("La sesión ya no es válida"));
    }

    private AuthResponse emitirTokens(Usuario usuario) {
        return new AuthResponse(
                jwtService.generarAccessToken(usuario),
                jwtService.generarRefreshToken(usuario),
                "Bearer",
                jwtService.segundosVidaAccess(),
                UsuarioResponse.de(usuario));
    }
}
