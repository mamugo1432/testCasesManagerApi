package es.test.manager.application.dto.testcase;

import java.util.List;

/**
 * Body of the full-replacement test case update request. It adds the required status to the
 * creation fields (RF-032 a RF-044).
 */
public record UpdateTestCaseRequest(
        String title,
        String description,
        String expectedResult,
        String priority,
        List<StepRequest> steps,
        List<String> tags,
        String status) {}
