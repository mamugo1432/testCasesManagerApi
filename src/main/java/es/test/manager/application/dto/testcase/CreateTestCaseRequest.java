package es.test.manager.application.dto.testcase;

import java.util.List;

/**
 * Body of the test case creation request. It declares only contract fields; the server resolves id,
 * project, status and creation date (RF-007 a RF-020).
 */
public record CreateTestCaseRequest(
        String title,
        String description,
        String expectedResult,
        String priority,
        List<StepRequest> steps,
        List<String> tags) {}
