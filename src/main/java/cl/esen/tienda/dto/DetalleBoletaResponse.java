package cl.esen.tienda.dto;

import cl.esen.tienda.model.DetalleBoleta;

public record DetalleBoletaResponse(
        Long id,
        Long productoId,
        String productoNombre,
        String productoImagenUrl,
        Integer cantidad,
        Integer precioUnitario,
        Integer subtotal) {

    public static DetalleBoletaResponse de(DetalleBoleta d) {
        return new DetalleBoletaResponse(d.getId(), d.getProducto().getId(), d.getProducto().getNombre(),
                d.getProducto().getImagenUrl(), d.getCantidad(), d.getPrecioUnitario(), d.getSubtotal());
    }
}
