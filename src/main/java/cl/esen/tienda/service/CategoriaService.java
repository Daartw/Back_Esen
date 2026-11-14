package cl.esen.tienda.service;

import cl.esen.tienda.dto.CategoriaRequest;
import cl.esen.tienda.dto.CategoriaResponse;
import cl.esen.tienda.exception.ConflictoException;
import cl.esen.tienda.exception.RecursoNoEncontradoException;
import cl.esen.tienda.model.Categoria;
import cl.esen.tienda.repository.CategoriaRepository;
import cl.esen.tienda.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;

    public CategoriaService(CategoriaRepository categoriaRepository, ProductoRepository productoRepository) {
        this.categoriaRepository = categoriaRepository;
        this.productoRepository = productoRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listar() {
        return categoriaRepository.findAll().stream().map(CategoriaResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public CategoriaResponse obtener(Long id) {
        return CategoriaResponse.de(buscar(id));
    }

    @Transactional
    public CategoriaResponse crear(CategoriaRequest request) {
        String nombre = request.nombre().trim();
        if (categoriaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new ConflictoException("Ya existe una categoría con ese nombre");
        }
        return CategoriaResponse.de(categoriaRepository.save(new Categoria(nombre, request.descripcion())));
    }

    @Transactional
    public CategoriaResponse actualizar(Long id, CategoriaRequest request) {
        Categoria categoria = buscar(id);
        String nombre = request.nombre().trim();
        if (!categoria.getNombre().equalsIgnoreCase(nombre) && categoriaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new ConflictoException("Ya existe una categoría con ese nombre");
        }
        categoria.setNombre(nombre);
        categoria.setDescripcion(request.descripcion());
        return CategoriaResponse.de(categoria);
    }

    @Transactional
    public void eliminar(Long id) {
        Categoria categoria = buscar(id);
        if (productoRepository.existsByCategoriaId(id)) {
            throw new ConflictoException("No se puede eliminar: la categoría tiene productos asociados");
        }
        categoriaRepository.delete(categoria);
    }

    Categoria buscar(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la categoría con id " + id));
    }
}
