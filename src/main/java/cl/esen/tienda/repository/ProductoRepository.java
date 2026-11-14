package cl.esen.tienda.repository;

import cl.esen.tienda.model.Producto;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    /**
     * Busqueda del catalogo. Filtros opcionales: categoriaId en null y texto vacio no filtran.
     * JOIN FETCH trae la categoria en la misma consulta y evita el problema N+1.
     */
    @Query("""
            SELECT p FROM Producto p JOIN FETCH p.categoria c
            WHERE (:incluirInactivos = true OR p.activo = true)
              AND (:categoriaId IS NULL OR c.id = :categoriaId)
              AND LOWER(p.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))
            ORDER BY p.id
            """)
    List<Producto> buscar(@Param("categoriaId") Long categoriaId,
                          @Param("texto") String texto,
                          @Param("incluirInactivos") boolean incluirInactivos);

    boolean existsByCategoriaId(Long categoriaId);

    /**
     * Lee el producto bloqueando su fila hasta que termine la transaccion.
     * Asi dos compras simultaneas no pueden vender la misma ultima unidad.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Producto p WHERE p.id = :id")
    Optional<Producto> buscarParaVenta(@Param("id") Long id);
}
