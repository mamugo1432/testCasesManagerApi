package es.test.manager.application.dto.testcase;

import java.util.List;

/**
 * Test case representation used by the listing: it has no {@code expectedResult} or {@code steps}
 * and formats {@code createdAt} as {@code dd/MM/yyyy} (RF-021 a RF-027).
 */
public record TestCaseSummaryResponse(
        Long id,
        Long projectId,
        String title,
        String description,
        String priority,
        String status,
        List<TagResponse> tags,
        String createdAt) {}
