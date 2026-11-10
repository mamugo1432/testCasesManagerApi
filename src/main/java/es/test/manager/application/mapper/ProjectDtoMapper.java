package es.test.manager.application.mapper;

import es.test.manager.application.dto.project.ProjectResponse;
import es.test.manager.domain.model.Project;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * MapStruct mapper from the project domain model to its public DTO. The date is formatted here
 * because the response contract exposes it as {@code dd/MM/yyyy} (RF-009).
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ProjectDtoMapper {

    @Mapping(target = "createdAt", dateFormat = "dd/MM/yyyy")
    ProjectResponse toResponse(Project project);

    List<ProjectResponse> toResponses(List<Project> projects);
}
