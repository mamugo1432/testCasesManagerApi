package es.test.manager.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Global tag. {@code name} is the persisted visible name and {@code normalizedName} is the
 * uniqueness key used to reuse it across projects and users (RF-058 a RF-066).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tag {

    private Long id;
    private String name;
    private String normalizedName;
}
