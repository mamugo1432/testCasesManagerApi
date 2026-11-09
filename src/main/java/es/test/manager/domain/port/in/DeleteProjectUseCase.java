package es.test.manager.domain.port.in;

import es.test.manager.domain.model.AuthenticatedUser;

/** Inbound port: delete one project owned by the authenticated user (RF-022, RF-025, RF-027). */
public interface DeleteProjectUseCase {

    void delete(Long id, AuthenticatedUser user);
}
