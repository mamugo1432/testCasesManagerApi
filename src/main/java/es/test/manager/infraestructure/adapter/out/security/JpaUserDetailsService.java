package es.test.manager.infraestructure.adapter.out.security;

import es.test.manager.domain.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Security adapter that loads users from the database. It keeps Spring Security's default in-memory
 * user from being created and exposes the domain user as a {@link UserDetails} with the authority
 * derived from its role (ROLE_ADMIN or ROLE_USER). The public login flow still goes through {@code
 * AuthService}, so this adapter is never reachable over HTTP: basic auth and form login are disabled.
 */
@Service
@RequiredArgsConstructor
public class JpaUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        var user = userRepository
                .findByUsernameOrEmail(identifier, identifier)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        return new SecurityUser(
                user.getId(), user.getUsername(), user.getEmail(), user.getPasswordHash(), user.getRole());
    }
}
