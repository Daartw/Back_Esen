package cl.esen.tienda.security;

import cl.esen.tienda.model.Usuario;
import cl.esen.tienda.repository.UsuarioRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Se ejecuta una vez por peticion: lee el header "Authorization: Bearer <token>",
 * valida el JWT y deja al usuario autenticado en el contexto de seguridad.
 * Si no hay token o es invalido la peticion sigue como anonima y
 * SecurityConfig decide si la ruta lo permite (si no, responde 401).
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String PREFIJO = "Bearer ";

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    public JwtAuthFilter(JwtService jwtService, UsuarioRepository usuarioRepository) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(PREFIJO)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            autenticar(header.substring(PREFIJO.length()));
        }
        chain.doFilter(request, response);
    }

    private void autenticar(String token) {
        try {
            Claims claims = jwtService.validar(token, JwtService.TIPO_ACCESS);
            // Se consulta la base para tomar el rol vigente y rechazar cuentas desactivadas,
            // aunque el token todavia no haya expirado.
            Optional<Usuario> usuario = usuarioRepository.findByEmailIgnoreCase(claims.getSubject());
            if (usuario.isPresent() && usuario.get().isActivo()) {
                var autoridad = new SimpleGrantedAuthority("ROLE_" + usuario.get().getRol().name());
                var auth = new UsernamePasswordAuthenticationToken(usuario.get().getEmail(), null, List.of(autoridad));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } catch (JwtException | IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();
        }
    }
}
