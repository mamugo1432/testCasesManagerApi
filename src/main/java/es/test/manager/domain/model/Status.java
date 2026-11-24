package es.test.manager.domain.model;

/**
 * Progress status of a test case. A new test case always starts in {@code IN_PROGRESS} and only
 * {@code IN_PROGRESS} and {@code COMPLETED} exist (RF-035, RF-068, RF-072).
 */
public enum Status {
    IN_PROGRESS,
    COMPLETED
}
