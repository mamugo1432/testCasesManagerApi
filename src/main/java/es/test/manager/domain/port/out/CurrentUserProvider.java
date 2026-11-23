package es.test.manager.domain.port.out;

import es.test.manager.domain.model.AuthenticatedUser;

/** Outbound port: identity of the user authenticated in the current request. */
public interface CurrentUserProvider {

    AuthenticatedUser getCurrentUser();
}
