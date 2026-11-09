package es.test.manager.infraestructure.mapper;

import es.test.manager.domain.model.Project;
import es.test.manager.infraestructure.adapter.out.persistence.ProjectJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/** MapStruct mapper between the project JPA entity and the domain model. */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ProjectMapper {

    Project toDomain(ProjectJpaEntity entity);

    ProjectJpaEntity toEntity(Project project);
}
