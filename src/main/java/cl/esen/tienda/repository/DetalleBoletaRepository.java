package cl.esen.tienda.repository;

import cl.esen.tienda.model.DetalleBoleta;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetalleBoletaRepository extends JpaRepository<DetalleBoleta, Long> {

    boolean existsByProductoId(Long productoId);
}
