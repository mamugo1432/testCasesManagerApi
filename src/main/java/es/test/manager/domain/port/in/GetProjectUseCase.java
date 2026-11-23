package es.test.manager.domain.port.in;

import es.test.manager.application.dto.project.ProjectResponse;

/** Inbound port: get one project by id (RF-020, RF-021). */
public interface GetProjectUseCase {

    ProjectResponse get(Long id);
}
