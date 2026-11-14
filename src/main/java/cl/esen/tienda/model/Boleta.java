package cl.esen.tienda.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "boleta")
public class Boleta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Folio visible para el cliente, por ejemplo ESN-000012. */
    @Column(unique = true, length = 20)
    private String numero;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private Integer neto;

    @Column(nullable = false)
    private Integer iva;

    @Column(nullable = false)
    private Integer total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoBoleta estado;

    @Column(name = "direccion_envio", nullable = false, length = 200)
    private String direccionEnvio;

    @OneToMany(mappedBy = "boleta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleBoleta> detalles = new ArrayList<>();

    public void agregarDetalle(DetalleBoleta detalle) {
        detalle.setBoleta(this);
        detalles.add(detalle);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public Integer getNeto() { return neto; }
    public void setNeto(Integer neto) { this.neto = neto; }
    public Integer getIva() { return iva; }
    public void setIva(Integer iva) { this.iva = iva; }
    public Integer getTotal() { return total; }
    public void setTotal(Integer total) { this.total = total; }
    public EstadoBoleta getEstado() { return estado; }
    public void setEstado(EstadoBoleta estado) { this.estado = estado; }
    public String getDireccionEnvio() { return direccionEnvio; }
    public void setDireccionEnvio(String direccionEnvio) { this.direccionEnvio = direccionEnvio; }
    public List<DetalleBoleta> getDetalles() { return detalles; }
}
