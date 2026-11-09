package es.test.manager.domain.port.in;

import es.test.manager.application.dto.project.CreateProjectRequest;
import es.test.manager.application.dto.project.ProjectResponse;
import es.test.manager.domain.model.AuthenticatedUser;

/** Inbound port: create a project owned by the authenticated user (RF-005, RF-012). */
public interface CreateProjectUseCase {

    ProjectResponse create(CreateProjectRequest request, AuthenticatedUser user);
}
