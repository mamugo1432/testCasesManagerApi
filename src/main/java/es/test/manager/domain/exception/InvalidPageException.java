package es.test.manager.domain.exception;

/** The requested page is negative. It is answered as a 400 on field {@code page} (RF-025, RF-075). */
public class InvalidPageException extends RuntimeException {

    public InvalidPageException() {
        super("La página no es válida");
    }
}
