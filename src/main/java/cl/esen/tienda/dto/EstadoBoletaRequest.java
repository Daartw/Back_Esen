package cl.esen.tienda.dto;

import cl.esen.tienda.model.EstadoBoleta;
import jakarta.validation.constraints.NotNull;

public record EstadoBoletaRequest(
        @NotNull(message = "El estado es obligatorio")
        EstadoBoleta estado) {
}
