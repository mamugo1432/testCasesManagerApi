package es.test.manager.domain.port.in;

import es.test.manager.application.dto.testcase.TestCaseDetailResponse;

/** Inbound port: get the detail of a test case owned by the authenticated user (RF-028 a RF-031). */
public interface GetTestCaseUseCase {

    TestCaseDetailResponse get(Long projectId, Long testCaseId);
}
