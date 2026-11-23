package es.test.manager.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import es.test.manager.application.dto.project.CreateProjectRequest;
import es.test.manager.application.dto.project.ProjectResponse;
import es.test.manager.application.mapper.ProjectDtoMapper;
import es.test.manager.domain.exception.ProjectLimitExceededException;
import es.test.manager.domain.exception.ProjectNotFoundException;
import es.test.manager.domain.model.AuthenticatedUser;
import es.test.manager.domain.model.Project;
import es.test.manager.domain.model.Role;
import es.test.manager.domain.port.out.CurrentUserProvider;
import es.test.manager.domain.port.out.ProjectLimitProvider;
import es.test.manager.domain.port.out.ProjectRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Behavior of the project application service (RF-002, RF-005, RF-006, RF-009, RF-010). */
@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    private static final AuthenticatedUser USER = new AuthenticatedUser(7L, "ana", "ana@example.com", Role.USER);

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectLimitProvider projectLimitProvider;

    @Mock
    private CurrentUserProvider currentUserProvider;

    private ProjectService projectService;

    @BeforeEach
    void setUp() {
        projectService = new ProjectService(
                projectRepository,
                projectLimitProvider,
                currentUserProvider,
                Mappers.getMapper(ProjectDtoMapper.class));
    }

    private void allowCreation() {
        given(currentUserProvider.getCurrentUser()).willReturn(USER);
        given(projectLimitProvider.maxProjectsPerUser()).willReturn(10);
        given(projectRepository.countByUserId(7L)).willReturn(0L);
    }

    private static Project project(Long id, String title, LocalDateTime createdAt) {
        return Project.builder()
                .id(id)
                .userId(7L)
                .title(title)
                .createdAt(createdAt)
                .build();
    }

    @Test
    void createStoresTheTitleExactlyAsReceivedAndTheOwnerFromTheCurrentUser() {
        allowCreation();
        given(projectRepository.save(any(Project.class))).willAnswer(invocation -> {
            Project project = invocation.getArgument(0);
            project.setId(1L);
            return project;
        });

        ProjectResponse response = projectService.create(new CreateProjectRequest("  Mi proyecto  "));

        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("  Mi proyecto  ");
        assertThat(captor.getValue().getUserId()).isEqualTo(7L);
        assertThat(captor.getValue().getCreatedAt()).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
    }

    @Test
    void createAcceptsRepeatedTitles() {
        allowCreation();
        given(projectRepository.save(any(Project.class))).willAnswer(invocation -> {
            Project project = invocation.getArgument(0);
            project.setId(1L);
            return project;
        });

        projectService.create(new CreateProjectRequest("Repetido"));
        projectService.create(new CreateProjectRequest("Repetido"));

        verify(projectRepository, times(2)).save(any(Project.class));
    }

    @Test
    void createReturnsTheCreationDateAsDayMonthYear() {
        allowCreation();
        given(projectRepository.save(any(Project.class))).willAnswer(invocation -> {
            Project project = invocation.getArgument(0);
            project.setId(3L);
            project.setCreatedAt(LocalDateTime.of(2026, 11, 9, 10, 15));
            return project;
        });

        ProjectResponse response = projectService.create(new CreateProjectRequest("Proyecto"));

        assertThat(response.createdAt()).isEqualTo("09/11/2026");
    }

    @Test
    void createThrowsWhenTheLimitIsReachedAndDoesNotSave() {
        given(currentUserProvider.getCurrentUser()).willReturn(USER);
        given(projectLimitProvider.maxProjectsPerUser()).willReturn(10);
        given(projectRepository.countByUserId(7L)).willReturn(10L);

        assertThatThrownBy(() -> projectService.create(new CreateProjectRequest("Proyecto")))
                .isInstanceOf(ProjectLimitExceededException.class)
                .hasMessage("No puedes crear más de 10 proyectos. Elimina uno para crear otro.");

        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    void createAllowsTheTenthProjectWhenNineExist() {
        given(currentUserProvider.getCurrentUser()).willReturn(USER);
        given(projectLimitProvider.maxProjectsPerUser()).willReturn(10);
        given(projectRepository.countByUserId(7L)).willReturn(9L);
        given(projectRepository.save(any(Project.class))).willAnswer(invocation -> {
            Project project = invocation.getArgument(0);
            project.setId(10L);
            return project;
        });

        ProjectResponse response = projectService.create(new CreateProjectRequest("Décimo"));

        assertThat(response.id()).isEqualTo(10L);
        verify(projectRepository).save(any(Project.class));
    }

    @Test
    void createRejectsAUserAlreadyAboveTheLimit() {
        given(currentUserProvider.getCurrentUser()).willReturn(USER);
        given(projectLimitProvider.maxProjectsPerUser()).willReturn(10);
        given(projectRepository.countByUserId(7L)).willReturn(11L);

        assertThatThrownBy(() -> projectService.create(new CreateProjectRequest("Proyecto")))
                .isInstanceOf(ProjectLimitExceededException.class);

        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    void listReturnsTheProjectsOfTheCurrentUserInTheOrderDeliveredByThePort() {
        Project first = project(1L, "Primero", LocalDateTime.of(2026, 11, 9, 12, 0));
        Project second = project(2L, "Segundo", LocalDateTime.of(2026, 11, 8, 12, 0));
        given(currentUserProvider.getCurrentUser()).willReturn(USER);
        given(projectRepository.findAllByUserIdOrderByCreatedAtDescIdDesc(7L)).willReturn(List.of(first, second));

        List<ProjectResponse> response = projectService.list();

        assertThat(response).extracting(ProjectResponse::id).containsExactly(1L, 2L);
        assertThat(response.get(0).title()).isEqualTo("Primero");
        assertThat(response.get(0).createdAt()).isEqualTo("09/11/2026");
    }

    @Test
    void listQueriesTheRepositoryWithTheCurrentUserId() {
        given(currentUserProvider.getCurrentUser()).willReturn(USER);
        given(projectRepository.findAllByUserIdOrderByCreatedAtDescIdDesc(7L)).willReturn(List.of());

        projectService.list();

        verify(projectRepository).findAllByUserIdOrderByCreatedAtDescIdDesc(7L);
    }

    @Test
    void listReturnsAnEmptyListWhenTheCurrentUserHasNoProjects() {
        given(currentUserProvider.getCurrentUser()).willReturn(USER);
        given(projectRepository.findAllByUserIdOrderByCreatedAtDescIdDesc(7L)).willReturn(List.of());

        assertThat(projectService.list()).isEmpty();
    }

    @Test
    void getReturnsAProjectOwnedByTheCurrentUser() {
        Project project = project(4L, "Mío", LocalDateTime.of(2026, 11, 7, 9, 30));
        given(currentUserProvider.getCurrentUser()).willReturn(USER);
        given(projectRepository.findByIdAndUserId(4L, 7L)).willReturn(Optional.of(project));

        ProjectResponse response = projectService.get(4L);

        assertThat(response.id()).isEqualTo(4L);
        assertThat(response.title()).isEqualTo("Mío");
        assertThat(response.createdAt()).isEqualTo("07/11/2026");
    }

    @Test
    void getThrowsNotFoundForAProjectOwnedByAnotherUser() {
        given(currentUserProvider.getCurrentUser()).willReturn(USER);
        given(projectRepository.findByIdAndUserId(5L, 7L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.get(5L))
                .isInstanceOf(ProjectNotFoundException.class)
                .hasMessage("Proyecto no encontrado");
    }

    @Test
    void getThrowsTheSameNotFoundForAMissingProject() {
        given(currentUserProvider.getCurrentUser()).willReturn(USER);
        given(projectRepository.findByIdAndUserId(99L, 7L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.get(99L))
                .isInstanceOf(ProjectNotFoundException.class)
                .hasMessage("Proyecto no encontrado");
    }

    @Test
    void deleteRemovesAProjectOwnedByTheCurrentUser() {
        Project project = project(4L, "Mío", LocalDateTime.of(2026, 11, 7, 9, 30));
        given(currentUserProvider.getCurrentUser()).willReturn(USER);
        given(projectRepository.findByIdAndUserId(4L, 7L)).willReturn(Optional.of(project));

        projectService.delete(4L);

        verify(projectRepository).deleteById(4L);
    }

    @Test
    void deleteThrowsNotFoundAndRemovesNothingWhenTheProjectIsNotVisibleToTheCurrentUser() {
        given(currentUserProvider.getCurrentUser()).willReturn(USER);
        given(projectRepository.findByIdAndUserId(8L, 7L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.delete(8L))
                .isInstanceOf(ProjectNotFoundException.class)
                .hasMessage("Proyecto no encontrado");

        verify(projectRepository).findByIdAndUserId(8L, 7L);
        verify(projectRepository, never()).deleteById(any());
    }

    @Test
    void deleteThrowsNotFoundAndRemovesNothingWhenTheProjectDoesNotExist() {
        given(currentUserProvider.getCurrentUser()).willReturn(USER);
        given(projectRepository.findByIdAndUserId(8L, 7L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.delete(8L))
                .isInstanceOf(ProjectNotFoundException.class)
                .hasMessage("Proyecto no encontrado");

        verify(projectRepository, never()).deleteById(any());
    }

    @Test
    void deleteFreesASlotSoTheUserCanCreateAgain() {
        given(currentUserProvider.getCurrentUser()).willReturn(USER);
        given(projectLimitProvider.maxProjectsPerUser()).willReturn(10);
        given(projectRepository.countByUserId(7L)).willReturn(10L, 9L);
        assertThatThrownBy(() -> projectService.create(new CreateProjectRequest("Undécimo")))
                .isInstanceOf(ProjectLimitExceededException.class);

        Project project = project(10L, "Antiguo", LocalDateTime.of(2026, 11, 1, 8, 0));
        given(projectRepository.findByIdAndUserId(10L, 7L)).willReturn(Optional.of(project));
        projectService.delete(10L);

        given(projectRepository.save(any(Project.class))).willAnswer(invocation -> {
            Project saved = invocation.getArgument(0);
            saved.setId(11L);
            return saved;
        });
        ProjectResponse response = projectService.create(new CreateProjectRequest("Nuevo"));

        assertThat(response.id()).isEqualTo(11L);
        verify(projectRepository).deleteById(10L);
    }
}
