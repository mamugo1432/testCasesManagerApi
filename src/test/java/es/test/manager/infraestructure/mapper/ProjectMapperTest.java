package es.test.manager.infraestructure.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import es.test.manager.domain.model.Project;
import es.test.manager.infraestructure.adapter.out.persistence.ProjectJpaEntity;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

/** MapStruct mapping between the project JPA entity and the domain model (RF-003, RF-009). */
class ProjectMapperTest {

    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 11, 9, 10, 15);

    private final ProjectMapper mapper = Mappers.getMapper(ProjectMapper.class);

    @Test
    void mapsAnEntityToTheDomainModel() {
        ProjectJpaEntity entity = new ProjectJpaEntity();
        entity.setId(3L);
        entity.setUserId(7L);
        entity.setTitle("  Mi proyecto  ");
        entity.setCreatedAt(CREATED_AT);

        Project project = mapper.toDomain(entity);

        assertThat(project.getId()).isEqualTo(3L);
        assertThat(project.getUserId()).isEqualTo(7L);
        assertThat(project.getTitle()).isEqualTo("  Mi proyecto  ");
        assertThat(project.getCreatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void mapsTheDomainModelToAnEntity() {
        Project project = Project.builder()
                .id(4L)
                .userId(8L)
                .title("Proyecto")
                .createdAt(CREATED_AT)
                .build();

        ProjectJpaEntity entity = mapper.toEntity(project);

        assertThat(entity.getId()).isEqualTo(4L);
        assertThat(entity.getUserId()).isEqualTo(8L);
        assertThat(entity.getTitle()).isEqualTo("Proyecto");
        assertThat(entity.getCreatedAt()).isEqualTo(CREATED_AT);
    }
}
