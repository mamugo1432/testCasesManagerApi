package es.test.manager.application.dto.testcase;

/**
 * Instruction of a test step received in create and update requests. Bean Validation constraints
 * are added in Fase 2 (RF-013, RF-049).
 */
public record StepRequest(String instruction) {}
