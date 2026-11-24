package es.test.manager.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Ordered step of a test case. It is a child entity of {@link TestCase}; {@code position} starts at
 * 1 and has no gaps, and the id is never exposed by the API (RF-013, RF-014, RF-049 a RF-057).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestStep {

    private Long id;
    private Long testCaseId;
    private String instruction;
    private Integer position;
}
