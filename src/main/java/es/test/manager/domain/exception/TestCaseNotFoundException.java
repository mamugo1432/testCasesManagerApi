package es.test.manager.domain.exception;

/**
 * The test case does not exist, belongs to another project or belongs to another user. A single
 * generic message is used for every case so the API never reveals foreign resources (RF-004,
 * RF-030, RF-041, RF-047, RF-080).
 */
public class TestCaseNotFoundException extends RuntimeException {

    public TestCaseNotFoundException() {
        super("Test case no encontrado");
    }
}
