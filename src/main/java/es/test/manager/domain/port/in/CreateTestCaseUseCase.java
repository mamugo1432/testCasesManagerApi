package es.test.manager.domain.port.in;

import es.test.manager.application.dto.testcase.CreateTestCaseRequest;
import es.test.manager.application.dto.testcase.TestCaseDetailResponse;

/** Inbound port: create a test case inside a project owned by the authenticated user (RF-007 a RF-020). */
public interface CreateTestCaseUseCase {

    TestCaseDetailResponse create(Long projectId, CreateTestCaseRequest request);
}
