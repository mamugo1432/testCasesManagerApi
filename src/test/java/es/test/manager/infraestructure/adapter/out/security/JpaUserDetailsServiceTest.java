package es.test.manager.infraestructure.adapter.out.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import es.test.manager.domain.model.Role;
import es.test.manager.domain.model.User;
import es.test.manager.domain.port.out.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/** The adapter loads the database user into Spring Security's UserDetails contract. */
@ExtendWith(MockitoExtension.class)
class JpaUserDetailsServiceTest {

    private static final String PASSWORD_HASH = "$2a$10$abcdefghijklmnopqrstuv";

    @Mock
    private UserRepository userRepository;

    private JpaUserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        userDetailsService = new JpaUserDetailsService(userRepository);
    }

    private static User user() {
        return User.builder()
                .id(5L)
                .username("ana")
                .email("ana@example.com")
                .role(Role.ADMIN)
                .passwordHash(PASSWORD_HASH)
                .build();
    }

    @Test
    void loadsTheUserWithItsPasswordHashIdentityAndRole() {
        when(userRepository.findByUsernameOrEmail("ana", "ana")).thenReturn(Optional.of(user()));

        UserDetails details = userDetailsService.loadUserByUsername("ana");

        assertThat(details).isInstanceOf(SecurityUser.class);
        assertThat(details.getUsername()).isEqualTo("ana");
        assertThat(details.getPassword()).isEqualTo(PASSWORD_HASH);
        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
        assertThat(((SecurityUser) details).getId()).isEqualTo(5L);
        assertThat(((SecurityUser) details).getEmail()).isEqualTo("ana@example.com");
        assertThat(((SecurityUser) details).getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void doesNotTrimTheIdentifier() {
        when(userRepository.findByUsernameOrEmail("  ana", "  ana")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("  ana"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
