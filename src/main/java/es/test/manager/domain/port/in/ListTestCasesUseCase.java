package es.test.manager.domain.port.in;

import es.test.manager.application.dto.testcase.TestCasePageResponse;

/** Inbound port: list the test cases of a project owned by the authenticated user (RF-021 a RF-027). */
public interface ListTestCasesUseCase {

    TestCasePageResponse list(Long projectId, int page);
}
