package es.test.manager.domain.port.in;

import es.test.manager.application.dto.project.ProjectResponse;
import es.test.manager.domain.model.AuthenticatedUser;
import java.util.List;

/** Inbound port: list every project owned by the authenticated user (RF-016 to RF-019). */
public interface ListProjectsUseCase {

    List<ProjectResponse> list(AuthenticatedUser user);
}
