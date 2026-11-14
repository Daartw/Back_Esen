package cl.esen.tienda.security;

import cl.esen.tienda.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Respuestas JSON de seguridad:
 * 401 cuando falta el token o no es valido, 403 cuando el rol no tiene permiso.
 */
@Component
public class RespuestasSeguridad implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RespuestasSeguridad(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        escribir(request, response, HttpStatus.UNAUTHORIZED, "Debes iniciar sesión para acceder a este recurso");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
            throws IOException {
        escribir(request, response, HttpStatus.FORBIDDEN, "No tienes permisos para realizar esta acción");
    }

    private void escribir(HttpServletRequest request, HttpServletResponse response, HttpStatus estado, String mensaje)
            throws IOException {
        response.setStatus(estado.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(),
                ErrorResponse.de(estado.value(), estado.getReasonPhrase(), mensaje, request.getRequestURI(), null));
    }
}
