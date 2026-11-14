package cl.esen.tienda.repository;

import cl.esen.tienda.model.Boleta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoletaRepository extends JpaRepository<Boleta, Long> {

    List<Boleta> findAllByOrderByFechaDesc();

    List<Boleta> findByUsuarioEmailIgnoreCaseOrderByFechaDesc(String email);

    boolean existsByUsuarioId(Long usuarioId);
}
