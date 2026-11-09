package es.test.manager.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Identity of the user authenticated through a JWT. It contains only the claims issued in the token
 * (no password), so it can be used as the principal without querying the database.
 */
@Getter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticatedUser {

    private Long id;
    private String username;
    private String email;
    private Role role;
}
