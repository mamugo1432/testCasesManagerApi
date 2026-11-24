package es.test.manager.domain.exception;

/**
 * A new tag collided with UNIQUE(normalized_name) because another transaction created it first.
 * The request answers 409 and neither retries nor re-reads the tag (RF-019, RF-039, RF-066).
 */
public class TagConflictException extends RuntimeException {

    public TagConflictException() {
        super("Conflicto al crear la etiqueta");
    }
}
