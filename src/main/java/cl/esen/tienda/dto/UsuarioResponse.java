package cl.esen.tienda.dto;

import cl.esen.tienda.model.Rol;
import cl.esen.tienda.model.Usuario;

import java.time.LocalDateTime;

/** Nunca expone la contraseña. */
public record UsuarioResponse(
        Long id,
        String nombre,
        String apellido,
        String email,
        Rol rol,
        String direccion,
        boolean activo,
        LocalDateTime fechaCreacion) {

    public static UsuarioResponse de(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNombre(), u.getApellido(), u.getEmail(),
                u.getRol(), u.getDireccion(), u.isActivo(), u.getFechaCreacion());
    }
}
