package es.test.manager.domain.port.in;

import es.test.manager.application.dto.project.ProjectResponse;
import es.test.manager.domain.model.AuthenticatedUser;

/** Inbound port: get one project owned by the authenticated user (RF-020, RF-021). */
public interface GetProjectUseCase {

    ProjectResponse get(Long id, AuthenticatedUser user);
}
