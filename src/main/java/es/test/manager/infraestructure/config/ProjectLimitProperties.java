package es.test.manager.infraestructure.config;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/** Maximum number of projects a user may own (RF-013). */
@Component
@ConfigurationProperties(prefix = "app.projects")
@Validated
@Getter
@Setter
public class ProjectLimitProperties {

    public static final int DEFAULT_MAX_PER_USER = 10;

    @Min(value = 1, message = "app.projects.max-per-user debe ser mayor que 0")
    private int maxPerUser = DEFAULT_MAX_PER_USER;
}
