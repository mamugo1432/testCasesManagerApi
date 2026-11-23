package es.test.manager.domain.port.in;

import es.test.manager.application.dto.project.ProjectResponse;
import java.util.List;

/** Inbound port: list every project (RF-016 to RF-019). */
public interface ListProjectsUseCase {

    List<ProjectResponse> list();
}
