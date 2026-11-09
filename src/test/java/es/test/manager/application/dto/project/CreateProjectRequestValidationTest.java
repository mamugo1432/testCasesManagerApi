package es.test.manager.application.dto.project;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Bean validation rules of the project creation payload (RF-006, RF-007, RF-008). */
class CreateProjectRequestValidationTest {

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

    private static Set<String> invalidFields(String title) {
        return validator.validate(new CreateProjectRequest(title)).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    @Test
    void acceptsAValidTitle() {
        assertThat(invalidFields("Proyecto de prueba")).isEmpty();
    }

    @Test
    void rejectsANullTitle() {
        assertThat(invalidFields(null)).contains("title");
    }

    @Test
    void rejectsAnEmptyTitle() {
        assertThat(invalidFields("")).contains("title");
    }

    @Test
    void rejectsATitleMadeOnlyOfSpaces() {
        assertThat(invalidFields("   ")).contains("title");
    }

    @Test
    void acceptsATitleOfExactly255Utf16Units() {
        assertThat(invalidFields("a".repeat(255))).isEmpty();
    }

    @Test
    void rejectsATitleOf256Utf16Units() {
        assertThat(invalidFields("a".repeat(256))).contains("title");
    }

    @Test
    void accepts127EmojisBecauseTheyAre254Utf16Units() {
        assertThat(invalidFields("😀".repeat(127))).isEmpty();
    }

    @Test
    void rejects128EmojisBecauseTheyAre256Utf16Units() {
        assertThat(invalidFields("😀".repeat(128))).contains("title");
    }

    @Test
    void acceptsOuterSpacesBecauseTheTitleIsStoredAsReceived() {
        assertThat(invalidFields("  Proyecto  ")).isEmpty();
    }
}
