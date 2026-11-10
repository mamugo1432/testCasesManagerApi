package es.test.manager.application.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import es.test.manager.application.dto.project.ProjectResponse;
import es.test.manager.domain.model.Project;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

/** MapStruct mapping from the project domain model to its public DTO (RF-009, RF-016, RF-020). */
class ProjectDtoMapperTest {

    private final ProjectDtoMapper mapper = Mappers.getMapper(ProjectDtoMapper.class);

    @Test
    void mapsTheDomainProjectToTheResponse() {
        Project project = Project.builder()
                .id(3L)
                .userId(7L)
                .title("  Mi proyecto  ")
                .createdAt(LocalDateTime.of(2026, 11, 9, 10, 15))
                .build();

        ProjectResponse response = mapper.toResponse(project);

        assertThat(response.id()).isEqualTo(3L);
        assertThat(response.title()).isEqualTo("  Mi proyecto  ");
        assertThat(response.createdAt()).isEqualTo("09/11/2026");
    }

    @Test
    void padsDayAndMonthWithLeadingZeros() {
        Project project = Project.builder()
                .id(1L)
                .title("Enero")
                .createdAt(LocalDateTime.of(2026, 1, 5, 0, 0))
                .build();

        assertThat(mapper.toResponse(project).createdAt()).isEqualTo("05/01/2026");
    }

    @Test
    void mapsAListPreservingTheOrder() {
        Project first = Project.builder()
                .id(1L)
                .title("Primero")
                .createdAt(LocalDateTime.of(2026, 11, 9, 12, 0))
                .build();
        Project second = Project.builder()
                .id(2L)
                .title("Segundo")
                .createdAt(LocalDateTime.of(2026, 11, 8, 12, 0))
                .build();

        assertThat(mapper.toResponses(List.of(first, second)))
                .extracting(ProjectResponse::id)
                .containsExactly(1L, 2L);
    }
}
