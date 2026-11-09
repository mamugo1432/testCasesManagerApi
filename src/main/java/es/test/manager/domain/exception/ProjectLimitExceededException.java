package es.test.manager.domain.exception;

/**
 * The authenticated user already owns the maximum number of projects allowed by configuration
 * (RF-012). The message is part of the API contract, so it includes the current limit.
 */
public class ProjectLimitExceededException extends RuntimeException {

    public ProjectLimitExceededException(int limit) {
        super("No puedes crear más de " + limit + " proyectos. Elimina uno para crear otro.");
    }
}
