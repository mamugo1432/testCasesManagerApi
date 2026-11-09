package es.test.manager.infraestructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.mock.env.MockEnvironment;

/** Configuration binding and validation of the project limit (RF-013, RF-015). */
class ProjectLimitPropertiesTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    private static ProjectLimitProperties bind(String value) {
        ProjectLimitProperties properties = new ProjectLimitProperties();
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("app.projects.max-per-user", value);
        Binder binder = Binder.get(environment);
        BindResult<ProjectLimitProperties> result = binder.bind("app.projects", Bindable.ofInstance(properties));
        assertThat(result.isBound()).isTrue();
        return result.get();
    }

    private static Set<String> invalidFields(ProjectLimitProperties properties) {
        return validator.validate(properties).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    @Test
    void defaultsToTen() {
        ProjectLimitProperties properties = new ProjectLimitProperties();

        assertThat(properties.getMaxPerUser()).isEqualTo(10);
        assertThat(invalidFields(properties)).isEmpty();
    }

    @Test
    void bindsAConfiguredValue() {
        ProjectLimitProperties properties = bind("4");

        assertThat(properties.getMaxPerUser()).isEqualTo(4);
        assertThat(invalidFields(properties)).isEmpty();
    }

    @Test
    void rejectsZero() {
        assertThat(invalidFields(bind("0"))).contains("maxPerUser");
    }

    @Test
    void rejectsANegativeValue() {
        assertThat(invalidFields(bind("-2"))).contains("maxPerUser");
    }
}
