package es.test.manager.infraestructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import es.test.manager.application.dto.project.CreateProjectRequest;
import es.test.manager.application.mapper.ProjectDtoMapper;
import es.test.manager.application.service.ProjectService;
import es.test.manager.domain.exception.ProjectLimitExceededException;
import es.test.manager.domain.model.AuthenticatedUser;
import es.test.manager.domain.model.Role;
import es.test.manager.domain.port.out.ProjectLimitProvider;
import es.test.manager.domain.port.out.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

/** The adapter exposes the configured limit and the service consumes it through the port (RF-013). */
class ProjectLimitProviderAdapterTest {

    private static final AuthenticatedUser USER = new AuthenticatedUser(7L, "ana", "ana@example.com", Role.USER);

    @Test
    void returnsTheConfiguredValue() {
        ProjectLimitProperties properties = new ProjectLimitProperties();
        properties.setMaxPerUser(3);

        ProjectLimitProvider provider = new ProjectLimitProviderAdapter(properties);

        assertThat(provider.maxProjectsPerUser()).isEqualTo(3);
    }

    @Test
    void theServiceReceivesTheLimitThroughThePort() {
        ProjectLimitProperties properties = new ProjectLimitProperties();
        properties.setMaxPerUser(1);
        ProjectRepository repository = mock(ProjectRepository.class);
        given(repository.countByUserId(7L)).willReturn(1L);
        ProjectService service = new ProjectService(
                repository, new ProjectLimitProviderAdapter(properties), Mappers.getMapper(ProjectDtoMapper.class));

        assertThatThrownBy(() -> service.create(new CreateProjectRequest("Proyecto"), USER))
                .isInstanceOf(ProjectLimitExceededException.class);
    }
}
