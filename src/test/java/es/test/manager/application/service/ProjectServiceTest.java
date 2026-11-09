package es.test.manager.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import es.test.manager.application.dto.project.CreateProjectRequest;
import es.test.manager.application.dto.project.ProjectResponse;
import es.test.manager.domain.model.AuthenticatedUser;
import es.test.manager.domain.model.Project;
import es.test.manager.domain.model.Role;
import es.test.manager.domain.port.out.ProjectLimitProvider;
import es.test.manager.domain.port.out.ProjectRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Behavior of the project application service (RF-002, RF-005, RF-006, RF-009, RF-010). */
@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    private static final AuthenticatedUser USER =
            new AuthenticatedUser(7L, "ana", "ana@example.com", Role.USER);

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectLimitProvider projectLimitProvider;

    private ProjectService projectService;

    @BeforeEach
    void setUp() {
        projectService = new ProjectService(projectRepository, projectLimitProvider);
    }

    @Test
    void createStoresTheTitleExactlyAsReceivedAndTheOwnerFromThePrincipal() {
        given(projectRepository.save(any(Project.class))).willAnswer(invocation -> {
            Project project = invocation.getArgument(0);
            project.setId(1L);
            return project;
        });

        ProjectResponse response = projectService.create(new CreateProjectRequest("  Mi proyecto  "), USER);

        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("  Mi proyecto  ");
        assertThat(captor.getValue().getUserId()).isEqualTo(7L);
        assertThat(captor.getValue().getCreatedAt()).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
    }

    @Test
    void createAcceptsRepeatedTitles() {
        given(projectRepository.save(any(Project.class))).willAnswer(invocation -> {
            Project project = invocation.getArgument(0);
            project.setId(1L);
            return project;
        });

        projectService.create(new CreateProjectRequest("Repetido"), USER);
        projectService.create(new CreateProjectRequest("Repetido"), USER);

        verify(projectRepository, times(2)).save(any(Project.class));
    }

    @Test
    void createReturnsTheCreationDateAsDayMonthYear() {
        given(projectRepository.save(any(Project.class))).willAnswer(invocation -> {
            Project project = invocation.getArgument(0);
            project.setId(3L);
            project.setCreatedAt(LocalDateTime.of(2026, 11, 9, 10, 15));
            return project;
        });

        ProjectResponse response = projectService.create(new CreateProjectRequest("Proyecto"), USER);

        assertThat(response.createdAt()).isEqualTo("09/11/2026");
    }
}
