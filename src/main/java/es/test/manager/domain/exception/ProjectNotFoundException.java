package es.test.manager.domain.exception;

/**
 * The project does not exist or belongs to another user. A single generic message is used for both
 * cases so the API never reveals whether a foreign project exists (RF-004, RF-021, RF-025).
 */
public class ProjectNotFoundException extends RuntimeException {

    public ProjectNotFoundException() {
        super("Proyecto no encontrado");
    }
}
