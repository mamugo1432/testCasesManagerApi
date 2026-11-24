package es.test.manager.application.dto.testcase;

import java.util.List;

/**
 * Paged listing wrapper with the fixed page size of 15 and the requested page number (RF-021 a
 * RF-026).
 */
public record TestCasePageResponse(
        List<TestCaseSummaryResponse> testCases, int page, int size, long totalElements, int totalPages) {}
