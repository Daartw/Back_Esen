package cl.esen.tienda.dto;

import cl.esen.tienda.model.Boleta;
import cl.esen.tienda.model.EstadoBoleta;

import java.time.LocalDateTime;
import java.util.List;

public record BoletaResponse(
        Long id,
        String numero,
        LocalDateTime fecha,
        EstadoBoleta estado,
        Long clienteId,
        String clienteNombre,
        String clienteEmail,
        String direccionEnvio,
        Integer neto,
        Integer iva,
        Integer total,
        List<DetalleBoletaResponse> detalles) {

    public static BoletaResponse de(Boleta b) {
        return new BoletaResponse(b.getId(), b.getNumero(), b.getFecha(), b.getEstado(),
                b.getUsuario().getId(),
                b.getUsuario().getNombre() + " " + b.getUsuario().getApellido(),
                b.getUsuario().getEmail(),
                b.getDireccionEnvio(), b.getNeto(), b.getIva(), b.getTotal(),
                b.getDetalles().stream().map(DetalleBoletaResponse::de).toList());
    }
}
