package es.test.manager.domain.exception;

/**
 * A COMPLETED test case has no non-blank expected result. It backs the Bean Validation rule as a
 * domain defence and is answered as a 400 on {@code expectedResult} (RF-036, RF-070, RF-076).
 */
public class InvalidTestCaseStateException extends RuntimeException {

    public InvalidTestCaseStateException() {
        super("El resultado esperado es obligatorio cuando el estado es COMPLETED");
    }
}
