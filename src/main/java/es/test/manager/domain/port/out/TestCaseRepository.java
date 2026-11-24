package es.test.manager.domain.port.out;

import es.test.manager.domain.model.PageResult;
import es.test.manager.domain.model.TestCase;
import java.util.Optional;

/**
 * Outbound port: test case persistence. Every lookup is scoped by project so a foreign or
 * mismatched test case is never visible (RF-003, RF-004, RF-021 a RF-048).
 */
public interface TestCaseRepository {

    /** Creates a test case together with its steps and tag associations in one transaction. */
    TestCase save(TestCase testCase);

    /** Finds a test case by id and project, so one from another project is treated as missing. */
    Optional<TestCase> findByIdAndProjectId(Long id, Long projectId);

    /** Page of test cases of a project, ordered by createdAt DESC, id DESC (RF-021 a RF-026). */
    PageResult<TestCase> findPageByProjectId(Long projectId, int page, int size);

    /** Deletes only the test_cases row; MySQL cascades steps and tag associations (RF-046). */
    void deleteById(Long id);

    /**
     * Replaces the steps and tag associations of an existing test case, preserving its row,
     * {@code projectId} and {@code createdAt} (RF-038 a RF-042).
     */
    TestCase replaceChildren(TestCase testCase);
}
