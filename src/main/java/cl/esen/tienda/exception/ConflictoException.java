package cl.esen.tienda.exception;

/** Se traduce a HTTP 409: el recurso choca con otro existente (por ejemplo, email duplicado). */
public class ConflictoException extends RuntimeException {
    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
