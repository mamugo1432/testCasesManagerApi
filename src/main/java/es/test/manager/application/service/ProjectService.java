package es.test.manager.application.service;

import es.test.manager.application.dto.project.CreateProjectRequest;
import es.test.manager.application.dto.project.ProjectResponse;
import es.test.manager.domain.model.AuthenticatedUser;
import es.test.manager.domain.model.Project;
import es.test.manager.domain.port.in.CreateProjectUseCase;
import es.test.manager.domain.port.out.ProjectLimitProvider;
import es.test.manager.domain.port.out.ProjectRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Application service implementing the project use cases. */
@Service
@RequiredArgsConstructor
public class ProjectService implements CreateProjectUseCase {

    private static final DateTimeFormatter RESPONSE_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ProjectRepository projectRepository;
    private final ProjectLimitProvider projectLimitProvider;

    /** RF-002, RF-005, RF-006, RF-009, RF-010. */
    @Override
    public ProjectResponse create(CreateProjectRequest request, AuthenticatedUser user) {
        Project project = Project.builder()
                .userId(user.getId())
                .title(request.title())
                .createdAt(LocalDateTime.now())
                .build();

        return toResponse(projectRepository.save(project));
    }

    private ProjectResponse toResponse(Project project) {
        return new ProjectResponse(
                project.getId(), project.getTitle(), project.getCreatedAt().format(RESPONSE_DATE_FORMAT));
    }
}
