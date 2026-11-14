package cl.esen.tienda.exception;

/** Se traduce a HTTP 400: la solicitud es valida en formato pero rompe una regla del negocio. */
public class ReglaNegocioException extends RuntimeException {
    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
