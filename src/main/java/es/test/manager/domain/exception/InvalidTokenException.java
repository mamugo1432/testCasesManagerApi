package es.test.manager.domain.exception;

/** Raised when a token is missing, malformed, tampered with or expired. */
public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException() {
        super("Token inválido o caducado");
    }
}
