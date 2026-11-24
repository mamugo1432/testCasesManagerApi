package es.test.manager.domain.exception;

/**
 * A tag name is invalid: empty after the Unicode trim, or its normalized form is empty or longer
 * than 50 UTF-16 units. The index identifies the offending element as {@code tags[i]} (RF-015,
 * RF-060, RF-061, RF-075).
 */
public class InvalidTagNameException extends RuntimeException {

    private final int index;

    public InvalidTagNameException(int index, String message) {
        super(message);
        this.index = index;
    }

    public int getIndex() {
        return index;
    }
}
