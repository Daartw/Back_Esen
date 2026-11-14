package cl.esen.tienda.config;

import cl.esen.tienda.security.JwtAuthFilter;
import cl.esen.tienda.security.RespuestasSeguridad;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Reglas de acceso del API segun los roles del cliente:
 * - ADMIN: acceso total.
 * - VENDEDOR: solo lectura de productos y de ordenes (boletas).
 * - CLIENTE: tienda (catalogo publico, comprar y ver sus propias compras).
 */
@Configuration
public class SecurityConfig {

    private static final String ADMIN = "ADMIN";
    private static final String VENDEDOR = "VENDEDOR";
    private static final String CLIENTE = "CLIENTE";

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter,
                                           RespuestasSeguridad respuestas) throws Exception {
        http
                // API sin estado: no hay cookies de sesion, por eso CSRF no aplica.
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(respuestas)
                        .accessDeniedHandler(respuestas))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Publico: documentacion, estado y autenticacion
                        .requestMatchers("/", "/error", "/api/v1/health").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/auth/login", "/api/v1/auth/registro", "/api/v1/auth/refresh").permitAll()

                        // Publico: catalogo de la tienda (solo lectura)
                        .requestMatchers(HttpMethod.GET, "/api/v1/productos/**", "/api/v1/categorias/**").permitAll()

                        // Mantenedores: solo ADMIN
                        .requestMatchers("/api/v1/productos/**", "/api/v1/categorias/**").hasRole(ADMIN)
                        .requestMatchers("/api/v1/usuarios/**").hasRole(ADMIN)

                        // Boletas
                        .requestMatchers(HttpMethod.GET, "/api/v1/boletas/mias").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/boletas").hasAnyRole(CLIENTE, ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/v1/boletas", "/api/v1/boletas/*")
                                .hasAnyRole(ADMIN, VENDEDOR)
                        .requestMatchers("/api/v1/boletas/**").hasRole(ADMIN)

                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") String origenes) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(Arrays.stream(origenes.split(",")).map(String::trim).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
