package es.test.manager.domain.exception;

/** Raised when the email of a new account is already taken (compared case-insensitively). */
public class InvalidEmailException extends RuntimeException {

    public static final String FIELD = "email";

    public InvalidEmailException() {
        super("El email ya está en uso");
    }
}
