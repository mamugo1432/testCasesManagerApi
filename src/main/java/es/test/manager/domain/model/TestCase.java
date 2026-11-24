package es.test.manager.domain.model;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Manual test case of a project. Pure POJO with Lombok: no JPA or framework annotations. The owner
 * is resolved from the project, and {@code projectId} and {@code createdAt} are immutable after
 * creation (RF-007 a RF-048, RF-067 a RF-072).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCase {

    private Long id;
    private Long projectId;
    private String title;
    private String description;
    private String expectedResult;
    private Priority priority;
    private Status status;
    private LocalDateTime createdAt;

    @Builder.Default
    private List<TestStep> steps = List.of();

    @Builder.Default
    private List<Tag> tags = List.of();
}
