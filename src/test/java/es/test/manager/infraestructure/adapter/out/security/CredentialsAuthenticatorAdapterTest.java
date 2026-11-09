package es.test.manager.infraestructure.adapter.out.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import es.test.manager.domain.exception.InvalidCredentialsException;
import es.test.manager.domain.model.AuthenticatedUser;
import es.test.manager.domain.model.Role;
import es.test.manager.domain.model.User;
import es.test.manager.domain.port.out.CredentialsAuthenticator;
import es.test.manager.domain.port.out.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * End-to-end credential check without Spring context: AuthenticationManager + DaoAuthenticationProvider
 * + JpaUserDetailsService + BCrypt. It proves the login really uses loadUserByUsername.
 */
class CredentialsAuthenticatorAdapterTest {

    private static final String RAW_PASSWORD = "Password123";
    private static final String PASSWORD_HASH = new BCryptPasswordEncoder().encode(RAW_PASSWORD);

    private UserRepository userRepository;
    private CredentialsAuthenticator authenticator;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(new JpaUserDetailsService(userRepository));
        provider.setPasswordEncoder(new BCryptPasswordEncoder());
        provider.afterPropertiesSet();
        authenticator = new CredentialsAuthenticatorAdapter(new ProviderManager(provider));
    }

    private static User user() {
        return User.builder()
                .id(5L)
                .username("ana")
                .email("ana@example.com")
                .role(Role.USER)
                .passwordHash(PASSWORD_HASH)
                .build();
    }

    @Test
    void authenticatesWithTheUsernameThroughTheUserDetailsService() {
        when(userRepository.findByUsernameOrEmail("ana", "ana")).thenReturn(Optional.of(user()));

        assertThat(authenticator.authenticate("ana", RAW_PASSWORD))
                .isEqualTo(new AuthenticatedUser(5L, "ana", "ana@example.com", Role.USER));
    }

    @Test
    void authenticatesWithTheEmailThroughTheUserDetailsService() {
        when(userRepository.findByUsernameOrEmail("ana@example.com", "ana@example.com"))
                .thenReturn(Optional.of(user()));

        assertThat(authenticator.authenticate("ana@example.com", RAW_PASSWORD))
                .isEqualTo(new AuthenticatedUser(5L, "ana", "ana@example.com", Role.USER));
    }

    @Test
    void unknownUserAndWrongPasswordThrowTheSameDomainException() {
        when(userRepository.findByUsernameOrEmail("nobody", "nobody")).thenReturn(Optional.empty());
        when(userRepository.findByUsernameOrEmail("ana", "ana")).thenReturn(Optional.of(user()));

        assertThatThrownBy(() -> authenticator.authenticate("nobody", RAW_PASSWORD))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Credenciales inválidas");
        assertThatThrownBy(() -> authenticator.authenticate("ana", "WrongPass1"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Credenciales inválidas");
    }

    @Test
    void doesNotTrimTheIdentifier() {
        when(userRepository.findByUsernameOrEmail("  ana", "  ana")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticator.authenticate("  ana", RAW_PASSWORD))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void treatsANullPrincipalAsInvalidCredentials() {
        CredentialsAuthenticator adapter = authenticatorReturning(authenticationWithPrincipal(null));

        assertThatThrownBy(() -> adapter.authenticate("ana", RAW_PASSWORD))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Credenciales inválidas");
    }

    @Test
    void treatsAForeignPrincipalAsInvalidCredentials() {
        CredentialsAuthenticator adapter = authenticatorReturning(authenticationWithPrincipal("ana"));

        assertThatThrownBy(() -> adapter.authenticate("ana", RAW_PASSWORD))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Credenciales inválidas");
    }

    @Test
    void treatsANullAuthenticationAsInvalidCredentials() {
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(null);
        CredentialsAuthenticator adapter = new CredentialsAuthenticatorAdapter(authenticationManager);

        assertThatThrownBy(() -> adapter.authenticate("ana", RAW_PASSWORD))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Credenciales inválidas");
    }

    private static CredentialsAuthenticator authenticatorReturning(Authentication authentication) {
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
        return new CredentialsAuthenticatorAdapter(authenticationManager);
    }

    private static Authentication authenticationWithPrincipal(Object principal) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(principal);
        return authentication;
    }
}
