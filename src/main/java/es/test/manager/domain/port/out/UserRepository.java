package es.test.manager.domain.port.out;

import es.test.manager.domain.model.User;
import java.util.Optional;

/** Outbound port: user persistence. */
public interface UserRepository {

    Optional<User> findById(Long id);

    /** Single query matching either the username or the email (login accepts both). */
    Optional<User> findByUsernameOrEmail(String username, String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    User save(User user);
}
