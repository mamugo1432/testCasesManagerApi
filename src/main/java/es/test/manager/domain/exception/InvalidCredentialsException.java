package es.test.manager.domain.exception;

/**
 * Generic authentication failure. A single message is used both for unknown identifiers and wrong
 * passwords so registered users cannot be enumerated.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Credenciales inválidas");
    }
}
