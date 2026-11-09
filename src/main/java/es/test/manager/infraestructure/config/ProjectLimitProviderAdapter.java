package es.test.manager.infraestructure.config;

import es.test.manager.domain.port.out.ProjectLimitProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Exposes the configured project limit to the application through the outbound port (RF-013). */
@Component
@RequiredArgsConstructor
public class ProjectLimitProviderAdapter implements ProjectLimitProvider {

    private final ProjectLimitProperties properties;

    @Override
    public int maxProjectsPerUser() {
        return properties.getMaxPerUser();
    }
}
