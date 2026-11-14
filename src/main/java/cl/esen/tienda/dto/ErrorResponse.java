package cl.esen.tienda.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

/** Formato unico de error del API. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        LocalDateTime fecha,
        int estado,
        String error,
        String mensaje,
        String ruta,
        Map<String, String> campos) {

    public static ErrorResponse de(int estado, String error, String mensaje, String ruta, Map<String, String> campos) {
        return new ErrorResponse(LocalDateTime.now(), estado, error, mensaje, ruta, campos);
    }
}
