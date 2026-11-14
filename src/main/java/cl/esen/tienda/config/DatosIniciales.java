package cl.esen.tienda.config;

import cl.esen.tienda.model.Categoria;
import cl.esen.tienda.model.Producto;
import cl.esen.tienda.model.Rol;
import cl.esen.tienda.model.Usuario;
import cl.esen.tienda.repository.CategoriaRepository;
import cl.esen.tienda.repository.ProductoRepository;
import cl.esen.tienda.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carga datos de demostracion la primera vez que la aplicacion arranca con la base vacia:
 * un usuario por rol, las categorias y el catalogo. Se desactiva con SEED_ENABLED=false.
 */
@Component
public class DatosIniciales implements CommandLineRunner {

    private static final String FOTO = "https://images.unsplash.com/photo-%s?auto=format&fit=crop&w=900&q=80";

    private final UsuarioRepository usuarioRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.enabled}")
    private boolean habilitado;
    @Value("${app.seed.admin-password}")
    private String claveAdmin;
    @Value("${app.seed.vendedor-password}")
    private String claveVendedor;
    @Value("${app.seed.cliente-password}")
    private String claveCliente;

    public DatosIniciales(UsuarioRepository usuarioRepository, CategoriaRepository categoriaRepository,
                          ProductoRepository productoRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.categoriaRepository = categoriaRepository;
        this.productoRepository = productoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!habilitado) {
            return;
        }
        if (usuarioRepository.count() == 0) {
            usuario("Admin", "ESEN", "admin@esen.cl", claveAdmin, Rol.ADMIN, "Av. Providencia 1234, Santiago");
            usuario("Valentina", "Rojas", "vendedor@esen.cl", claveVendedor, Rol.VENDEDOR, "Av. Apoquindo 4500, Las Condes");
            usuario("Camila", "Soto", "cliente@esen.cl", claveCliente, Rol.CLIENTE, "Los Leones 220, Providencia");
        }
        if (categoriaRepository.count() == 0) {
            Categoria hoodies = categoria("Hoodies", "Polerones con capucha de algodón pesado y calce relajado.");
            Categoria polerones = categoria("Polerones", "Cuello redondo y tejidos para todos los días.");
            Categoria poleras = categoria("Poleras", "Básicos de algodón en tonos neutros.");
            Categoria conjuntos = categoria("Conjuntos", "Sets coordinados para vestir sin pensar.");

            producto("Hoodie Esencial Blanco", "Hoodie de felpa de algodón 400 g con capucha doble y bolsillo canguro. Calce oversize.",
                    54990, 25, "Blanco", "1620799140188-3b2a02fd9a77", hoodies);
            producto("Hoodie Esencial Gris", "Felpa cepillada por dentro, puños acanalados y capucha con cordón plano.",
                    54990, 18, "Gris melange", "1556821840-3a63f95609a7", hoodies);
            producto("Hoodie Zip Negro", "Hoodie con cierre completo, algodón pesado y terminación lavada.",
                    59990, 12, "Negro", "1622567893612-a5345baa5c9a", hoodies);
            producto("Hoodie Esencial Café", "Tono tierra teñido en prenda. Hombro caído y largo regular.",
                    54990, 9, "Café", "1578768079052-aa76e52ff62e", hoodies);
            producto("Hoodie Esencial Crema", "El clásico de la colección en tono crema. Algodón peinado y calce amplio.",
                    54990, 3, "Crema", "1615397587950-3cbb55f95b77", hoodies);

            producto("Crewneck Esencial Blanco", "Polerón cuello redondo de french terry, costuras reforzadas y calce recto.",
                    44990, 20, "Blanco", "1620799140408-edc6dcb6d633", polerones);
            producto("Crewneck Off-White", "Algodón orgánico de gramaje medio. Ideal para capas.",
                    44990, 14, "Off-white", "1693901257178-b5fcb8f036a8", polerones);
            producto("Sweater Tejido Arena", "Tejido de punto grueso con cuello redondo y caída suave.",
                    49990, 0, "Arena", "1631541909061-71e349d1f203", polerones);

            producto("Polera Esencial Blanca", "Polera de algodón 220 g, cuello acanalado firme y calce relajado.",
                    19990, 40, "Blanco", "1622445275463-afa2ab738c34", poleras);
            producto("Polera Manga Larga Blanca", "Jersey de algodón con puño acanalado. Básico de media estación.",
                    24990, 22, "Blanco", "1580682312385-e94d8de1cf3c", poleras);
            producto("Polo Piqué Blanco", "Polo de piqué con cuello tejido y botones al tono.",
                    29990, 16, "Blanco", "1621773881532-fe65715b5137", poleras);

            producto("Set Lounge Gris", "Conjunto de polera y pantalón en jersey gris. Se vende como set completo.",
                    64990, 8, "Gris", "1588117260148-b47818741c74", conjuntos);
        }
    }

    private void usuario(String nombre, String apellido, String email, String clave, Rol rol, String direccion) {
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setApellido(apellido);
        u.setEmail(email);
        u.setPassword(passwordEncoder.encode(clave));
        u.setRol(rol);
        u.setDireccion(direccion);
        usuarioRepository.save(u);
    }

    private Categoria categoria(String nombre, String descripcion) {
        return categoriaRepository.save(new Categoria(nombre, descripcion));
    }

    private void producto(String nombre, String descripcion, int precio, int stock, String color,
                          String idFoto, Categoria categoria) {
        Producto p = new Producto();
        p.setNombre(nombre);
        p.setDescripcion(descripcion);
        p.setPrecio(precio);
        p.setStock(stock);
        p.setColor(color);
        p.setImagenUrl(String.format(FOTO, idFoto));
        p.setCategoria(categoria);
        productoRepository.save(p);
    }
}
