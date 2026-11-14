package cl.esen.tienda.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductoRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre admite hasta 120 caracteres")
        String nombre,

        @Size(max = 1000, message = "La descripción admite hasta 1000 caracteres")
        String descripcion,

        @NotNull(message = "El precio es obligatorio")
        @Min(value = 1, message = "El precio debe ser mayor a 0")
        Integer precio,

        @NotNull(message = "El stock es obligatorio")
        @Min(value = 0, message = "El stock no puede ser negativo")
        Integer stock,

        @Size(max = 40, message = "El color admite hasta 40 caracteres")
        String color,

        @Size(max = 500, message = "La URL de la imagen admite hasta 500 caracteres")
        String imagenUrl,

        @NotNull(message = "La categoría es obligatoria")
        Long categoriaId,

        Boolean activo) {
}
