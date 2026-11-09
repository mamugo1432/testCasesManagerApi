package es.test.manager.domain.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * User of the business. Pure POJO with Lombok: no JPA or framework annotations. Persistence mapping
 * lives in the adapter layer.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private Long id;
    private String username;
    private String email;
    private Role role;
    private String passwordHash;
    private LocalDateTime createdAt;
}
