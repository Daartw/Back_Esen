package cl.esen.tienda.security;

import cl.esen.tienda.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Genera y valida los tokens JWT (firma HS256).
 * - Access token: vida corta, se envia en cada peticion.
 * - Refresh token: vida larga, solo sirve para pedir un access token nuevo.
 */
@Service
public class JwtService {

    public static final String TIPO_ACCESS = "access";
    public static final String TIPO_REFRESH = "refresh";

    private final SecretKey clave;
    private final Duration vidaAccess;
    private final Duration vidaRefresh;

    public JwtService(@Value("${app.jwt.secret}") String secreto,
                      @Value("${app.jwt.access-minutes}") long minutosAccess,
                      @Value("${app.jwt.refresh-days}") long diasRefresh) {
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.vidaAccess = Duration.ofMinutes(minutosAccess);
        this.vidaRefresh = Duration.ofDays(diasRefresh);
    }

    public String generarAccessToken(Usuario usuario) {
        return generar(usuario, TIPO_ACCESS, vidaAccess);
    }

    public String generarRefreshToken(Usuario usuario) {
        return generar(usuario, TIPO_REFRESH, vidaRefresh);
    }

    public long segundosVidaAccess() {
        return vidaAccess.toSeconds();
    }

    /**
     * Verifica firma y expiracion y devuelve los datos del token.
     * Lanza JwtException si el token es invalido, esta vencido o no es del tipo esperado.
     */
    public Claims validar(String token, String tipoEsperado) {
        Claims claims = Jwts.parser()
                .verifyWith(clave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        if (!tipoEsperado.equals(claims.get("tipo", String.class))) {
            throw new JwtException("El token no es de tipo " + tipoEsperado);
        }
        return claims;
    }

    private String generar(Usuario usuario, String tipo, Duration vida) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(usuario.getEmail())
                .claim("rol", usuario.getRol().name())
                .claim("tipo", tipo)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(vida)))
                .signWith(clave)
                .compact();
    }
}
