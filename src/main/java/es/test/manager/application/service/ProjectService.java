package es.test.manager.application.service;

import es.test.manager.application.dto.project.CreateProjectRequest;
import es.test.manager.application.dto.project.ProjectResponse;
import es.test.manager.domain.exception.ProjectLimitExceededException;
import es.test.manager.domain.exception.ProjectNotFoundException;
import es.test.manager.domain.model.AuthenticatedUser;
import es.test.manager.domain.model.Project;
import es.test.manager.domain.port.in.CreateProjectUseCase;
import es.test.manager.domain.port.in.DeleteProjectUseCase;
import es.test.manager.domain.port.in.GetProjectUseCase;
import es.test.manager.domain.port.in.ListProjectsUseCase;
import es.test.manager.domain.port.out.ProjectLimitProvider;
import es.test.manager.domain.port.out.ProjectRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service implementing the project use cases. */
@Service
@RequiredArgsConstructor
public class ProjectService
        implements CreateProjectUseCase, ListProjectsUseCase, GetProjectUseCase, DeleteProjectUseCase {

    private static final DateTimeFormatter RESPONSE_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ProjectRepository projectRepository;
    private final ProjectLimitProvider projectLimitProvider;

    /**
     * RF-002, RF-005, RF-006, RF-009, RF-010, RF-012, RF-013, RF-014. The limit check is
     * best-effort: it runs in the same transaction as the insert, but two concurrent creations at
     * the limit could both pass the count and exceed it.
     */
    @Override
    @Transactional
    public ProjectResponse create(CreateProjectRequest request, AuthenticatedUser user) {
        int limit = projectLimitProvider.maxProjectsPerUser();
        if (projectRepository.countByUserId(user.getId()) >= limit) {
            throw new ProjectLimitExceededException(limit);
        }

        Project project = Project.builder()
                .userId(user.getId())
                .title(request.title())
                .createdAt(LocalDateTime.now())
                .build();

        return toResponse(projectRepository.save(project));
    }

    /** RF-016, RF-017, RF-018, RF-019. The port already filters and orders. */
    @Override
    public List<ProjectResponse> list(AuthenticatedUser user) {
        return projectRepository.findAllByUserIdOrderByCreatedAtDescIdDesc(user.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    /** RF-020, RF-021. A foreign project and a missing one produce the same exception. */
    @Override
    public ProjectResponse get(Long id, AuthenticatedUser user) {
        return projectRepository
                .findByIdAndUserId(id, user.getId())
                .map(this::toResponse)
                .orElseThrow(ProjectNotFoundException::new);
    }

    /**
     * RF-022, RF-025, RF-026, RF-027. Only the project row is deleted; the database removes its
     * test cases, steps and tag associations through {@code ON DELETE CASCADE}.
     */
    @Override
    @Transactional
    public void delete(Long id, AuthenticatedUser user) {
        Project project = projectRepository
                .findByIdAndUserId(id, user.getId())
                .orElseThrow(ProjectNotFoundException::new);
        projectRepository.deleteById(project.getId());
    }

    private ProjectResponse toResponse(Project project) {
        return new ProjectResponse(
                project.getId(), project.getTitle(), project.getCreatedAt().format(RESPONSE_DATE_FORMAT));
    }
}
