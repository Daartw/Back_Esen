package cl.esen.tienda.dto;

import cl.esen.tienda.model.Producto;

public record ProductoResponse(
        Long id,
        String nombre,
        String descripcion,
        Integer precio,
        Integer stock,
        String color,
        String imagenUrl,
        boolean activo,
        Long categoriaId,
        String categoriaNombre) {

    public static ProductoResponse de(Producto p) {
        return new ProductoResponse(p.getId(), p.getNombre(), p.getDescripcion(), p.getPrecio(), p.getStock(),
                p.getColor(), p.getImagenUrl(), p.isActivo(),
                p.getCategoria().getId(), p.getCategoria().getNombre());
    }
}
