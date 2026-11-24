package es.test.manager.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Pure result of {@code TagNormalizer}. It carries the visible name after the Unicode trim and the
 * normalized name after the case and diacritic steps (RF-058 a RF-062).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagName {

    private String visibleName;
    private String normalizedName;
}
