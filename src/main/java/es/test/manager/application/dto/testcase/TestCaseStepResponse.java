package es.test.manager.application.dto.testcase;

/** Step exposed as {@code {position, instruction}}, without id or testCaseId (RF-029, RF-051). */
public record TestCaseStepResponse(Integer position, String instruction) {}
