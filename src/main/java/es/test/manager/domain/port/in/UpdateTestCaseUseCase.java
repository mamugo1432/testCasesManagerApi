package es.test.manager.domain.port.in;

import es.test.manager.application.dto.testcase.TestCaseDetailResponse;
import es.test.manager.application.dto.testcase.UpdateTestCaseRequest;

/** Inbound port: fully replace a test case owned by the authenticated user (RF-032 a RF-044). */
public interface UpdateTestCaseUseCase {

    TestCaseDetailResponse update(Long projectId, Long testCaseId, UpdateTestCaseRequest request);
}
