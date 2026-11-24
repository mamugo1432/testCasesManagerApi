package es.test.manager.domain.model;

/**
 * Priority of a test case. It is mandatory when creating and editing and is serialized in uppercase
 * (RF-012, RF-067, RF-072).
 */
public enum Priority {
    HIGH,
    MEDIUM,
    LOW
}
