package cl.esen.tienda.service;

import cl.esen.tienda.dto.UsuarioRequest;
import cl.esen.tienda.dto.UsuarioResponse;
import cl.esen.tienda.exception.ConflictoException;
import cl.esen.tienda.exception.RecursoNoEncontradoException;
import cl.esen.tienda.exception.ReglaNegocioException;
import cl.esen.tienda.model.Rol;
import cl.esen.tienda.model.Usuario;
import cl.esen.tienda.repository.BoletaRepository;
import cl.esen.tienda.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioService {

    private static final int LARGO_MINIMO_CLAVE = 8;

    private final UsuarioRepository usuarioRepository;
    private final BoletaRepository boletaRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, BoletaRepository boletaRepository,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.boletaRepository = boletaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll().stream().map(UsuarioResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtener(Long id) {
        return UsuarioResponse.de(buscar(id));
    }

    @Transactional
    public UsuarioResponse crear(UsuarioRequest request) {
        String email = request.email().trim().toLowerCase();
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictoException("Ya existe una cuenta con ese email");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new ReglaNegocioException("La contraseña es obligatoria al crear un usuario");
        }
        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setPassword(codificar(request.password()));
        copiarDatos(request, usuario);
        return UsuarioResponse.de(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponse actualizar(Long id, UsuarioRequest request, String emailSolicitante) {
        Usuario usuario = buscar(id);
        String email = request.email().trim().toLowerCase();
        if (!usuario.getEmail().equalsIgnoreCase(email) && usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictoException("Ya existe una cuenta con ese email");
        }
        boolean esElMismo = usuario.getEmail().equalsIgnoreCase(emailSolicitante);
        boolean quedaSinAcceso = request.rol() != Rol.ADMIN || Boolean.FALSE.equals(request.activo());
        if (esElMismo && quedaSinAcceso) {
            // Evita que el administrador se quite a si mismo el acceso por error.
            throw new ReglaNegocioException("No puedes quitarte el rol de administrador ni desactivar tu propia cuenta");
        }
        usuario.setEmail(email);
        if (request.password() != null && !request.password().isBlank()) {
            usuario.setPassword(codificar(request.password()));
        }
        copiarDatos(request, usuario);
        return UsuarioResponse.de(usuario);
    }

    /**
     * Si el usuario tiene boletas no se borra (se perderia el historial de ventas):
     * se desactiva y ya no puede iniciar sesion.
     */
    @Transactional
    public void eliminar(Long id, String emailSolicitante) {
        Usuario usuario = buscar(id);
        if (usuario.getEmail().equalsIgnoreCase(emailSolicitante)) {
            throw new ReglaNegocioException("No puedes eliminar tu propia cuenta");
        }
        if (boletaRepository.existsByUsuarioId(id)) {
            usuario.setActivo(false);
        } else {
            usuarioRepository.delete(usuario);
        }
    }

    private void copiarDatos(UsuarioRequest request, Usuario usuario) {
        usuario.setNombre(request.nombre().trim());
        usuario.setApellido(request.apellido().trim());
        usuario.setRol(request.rol());
        usuario.setDireccion(request.direccion());
        usuario.setActivo(request.activo() == null || request.activo());
    }

    private String codificar(String clave) {
        if (clave.length() < LARGO_MINIMO_CLAVE) {
            throw new ReglaNegocioException("La contraseña debe tener al menos " + LARGO_MINIMO_CLAVE + " caracteres");
        }
        return passwordEncoder.encode(clave);
    }

    private Usuario buscar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario con id " + id));
    }
}
