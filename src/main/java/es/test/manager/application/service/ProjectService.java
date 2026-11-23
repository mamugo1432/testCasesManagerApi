package es.test.manager.application.service;

import es.test.manager.application.dto.project.CreateProjectRequest;
import es.test.manager.application.dto.project.ProjectResponse;
import es.test.manager.application.mapper.ProjectDtoMapper;
import es.test.manager.domain.exception.ProjectLimitExceededException;
import es.test.manager.domain.exception.ProjectNotFoundException;
import es.test.manager.domain.model.AuthenticatedUser;
import es.test.manager.domain.model.Project;
import es.test.manager.domain.port.in.CreateProjectUseCase;
import es.test.manager.domain.port.in.DeleteProjectUseCase;
import es.test.manager.domain.port.in.GetProjectUseCase;
import es.test.manager.domain.port.in.ListProjectsUseCase;
import es.test.manager.domain.port.out.CurrentUserProvider;
import es.test.manager.domain.port.out.ProjectLimitProvider;
import es.test.manager.domain.port.out.ProjectRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service implementing the project use cases. */
@Service
@RequiredArgsConstructor
public class ProjectService
        implements CreateProjectUseCase, ListProjectsUseCase, GetProjectUseCase, DeleteProjectUseCase {

    private final ProjectRepository projectRepository;
    private final ProjectLimitProvider projectLimitProvider;
    private final CurrentUserProvider currentUserProvider;
    private final ProjectDtoMapper projectDtoMapper;

    /**
     * RF-002, RF-005, RF-006, RF-009, RF-010, RF-012, RF-013, RF-014. The current user owns the
     * new project and feeds the per-user limit. The limit check is best-effort: it runs in the same
     * transaction as the insert, but two concurrent creations at the limit could both pass the
     * count and exceed it.
     */
    @Override
    @Transactional
    public ProjectResponse create(CreateProjectRequest request) {
        AuthenticatedUser user = currentUserProvider.getCurrentUser();
        int limit = projectLimitProvider.maxProjectsPerUser();
        if (projectRepository.countByUserId(user.getId()) >= limit) {
            throw new ProjectLimitExceededException(limit);
        }

        Project project = Project.builder()
                .userId(user.getId())
                .title(request.title())
                .createdAt(LocalDateTime.now())
                .build();

        return projectDtoMapper.toResponse(projectRepository.save(project));
    }

    /** RF-016, RF-017, RF-018, RF-019. Only the projects owned by the current user are returned. */
    @Override
    public List<ProjectResponse> list() {
        AuthenticatedUser user = currentUserProvider.getCurrentUser();
        return projectDtoMapper.toResponses(projectRepository.findAllByUserIdOrderByCreatedAtDescIdDesc(user.getId()));
    }

    /** RF-020, RF-021. A missing or foreign project produces the same 404. */
    @Override
    public ProjectResponse get(Long id) {
        AuthenticatedUser user = currentUserProvider.getCurrentUser();
        return projectRepository
                .findByIdAndUserId(id, user.getId())
                .map(projectDtoMapper::toResponse)
                .orElseThrow(ProjectNotFoundException::new);
    }

    /**
     * RF-022, RF-025, RF-026, RF-027. The project is resolved by id and owner, so a foreign project
     * produces the same 404 as a missing one. Only the project row is deleted; the database removes
     * its test cases, steps and tag associations through {@code ON DELETE CASCADE}.
     */
    @Override
    @Transactional
    public void delete(Long id) {
        AuthenticatedUser user = currentUserProvider.getCurrentUser();
        Project project =
                projectRepository.findByIdAndUserId(id, user.getId()).orElseThrow(ProjectNotFoundException::new);
        projectRepository.deleteById(project.getId());
    }
}
