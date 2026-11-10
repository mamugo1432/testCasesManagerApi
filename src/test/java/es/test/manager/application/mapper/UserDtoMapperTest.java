package es.test.manager.application.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import es.test.manager.application.dto.auth.UserResponse;
import es.test.manager.domain.model.AuthenticatedUser;
import es.test.manager.domain.model.Role;
import es.test.manager.domain.model.User;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

/** MapStruct mapping from the user models to the public DTO, which never exposes the hash. */
class UserDtoMapperTest {

    private final UserDtoMapper mapper = Mappers.getMapper(UserDtoMapper.class);

    @Test
    void mapsAUserToThePublicResponse() {
        User user = User.builder()
                .id(7L)
                .username("ana")
                .email("ana@example.com")
                .role(Role.USER)
                .passwordHash("hash-secreto")
                .createdAt(LocalDateTime.of(2026, 11, 9, 10, 15))
                .build();

        UserResponse response = mapper.toResponse(user);

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.username()).isEqualTo("ana");
        assertThat(response.email()).isEqualTo("ana@example.com");
        assertThat(response.role()).isEqualTo(Role.USER);
    }

    @Test
    void mapsAnAuthenticatedUserToThePublicResponse() {
        AuthenticatedUser user = new AuthenticatedUser(8L, "luis", "luis@example.com", Role.ADMIN);

        UserResponse response = mapper.toResponse(user);

        assertThat(response.id()).isEqualTo(8L);
        assertThat(response.username()).isEqualTo("luis");
        assertThat(response.email()).isEqualTo("luis@example.com");
        assertThat(response.role()).isEqualTo(Role.ADMIN);
    }
}
