package es.test.manager.domain.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Project of the business. Pure POJO with Lombok: no JPA or framework annotations. Persistence
 * mapping lives in the adapter layer.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Project {

    private Long id;
    private Long userId;
    private String title;
    private LocalDateTime createdAt;
}
