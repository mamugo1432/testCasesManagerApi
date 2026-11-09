package es.test.manager.domain.exception;

/** Raised when the username of a new account is already taken (compared case-insensitively). */
public class InvalidUsernameException extends RuntimeException {

    public static final String FIELD = "username";

    public InvalidUsernameException() {
        super("El username ya está en uso");
    }
}
