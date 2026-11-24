package es.test.manager.domain.port.in;

/** Inbound port: delete a test case owned by the authenticated user (RF-045 a RF-048). */
public interface DeleteTestCaseUseCase {

    void delete(Long projectId, Long testCaseId);
}
