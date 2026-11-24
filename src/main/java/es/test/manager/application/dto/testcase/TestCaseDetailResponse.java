package es.test.manager.application.dto.testcase;

import java.util.List;

/**
 * Complete test case representation used by creation, detail and update: it adds
 * {@code expectedResult} and ordered steps to the summary (RF-028 a RF-031, RF-044).
 */
public record TestCaseDetailResponse(
        Long id,
        Long projectId,
        String title,
        String description,
        String expectedResult,
        String priority,
        String status,
        List<TagResponse> tags,
        List<TestCaseStepResponse> steps,
        String createdAt) {}
