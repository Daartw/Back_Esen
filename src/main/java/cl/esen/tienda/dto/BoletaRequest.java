package cl.esen.tienda.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Compra enviada por el frontend. Solo viaja que producto y cuantas unidades:
 * los precios y el total los calcula el backend para que no puedan manipularse.
 */
public record BoletaRequest(
        @NotBlank(message = "La dirección de envío es obligatoria")
        @Size(max = 200, message = "La dirección admite hasta 200 caracteres")
        String direccionEnvio,

        @NotEmpty(message = "El carrito está vacío")
        @Valid
        List<ItemCompraRequest> items) {
}
