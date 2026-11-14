package cl.esen.tienda.dto;

import cl.esen.tienda.model.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Alta y edicion de usuarios por el administrador.
 * La contraseña es obligatoria al crear y opcional al editar (si llega vacia se conserva la actual).
 */
public record UsuarioRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 60, message = "El nombre admite hasta 60 caracteres")
        String nombre,

        @NotBlank(message = "El apellido es obligatorio")
        @Size(max = 60, message = "El apellido admite hasta 60 caracteres")
        String apellido,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato válido")
        @Size(max = 120, message = "El email admite hasta 120 caracteres")
        String email,

        @Size(max = 72, message = "La contraseña admite hasta 72 caracteres")
        String password,

        @NotNull(message = "El rol es obligatorio")
        Rol rol,

        @Size(max = 200, message = "La dirección admite hasta 200 caracteres")
        String direccion,

        Boolean activo) {
}
