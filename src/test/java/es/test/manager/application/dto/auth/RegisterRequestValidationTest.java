package es.test.manager.application.dto.auth;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Bean validation rules of the registration payload (RF-002 to RF-005). */
class RegisterRequestValidationTest {

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

    private static RegisterRequest validRequest() {
        return new RegisterRequest("ana", "ana@example.com", "Password123", "Password123");
    }

    private static Set<String> invalidFields(RegisterRequest request) {
        return validator.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    @Test
    void acceptsAValidRegistration() {
        assertThat(validator.validate(validRequest())).isEmpty();
    }

    @Test
    void acceptsAUsernameWithOuterSpacesBecauseItIsStoredAsReceived() {
        RegisterRequest request = new RegisterRequest("  ana  ", "ana@example.com", "Password123", "Password123");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsBlankUsername() {
        RegisterRequest request = new RegisterRequest("   ", "ana@example.com", "Password123", "Password123");

        assertThat(invalidFields(request)).contains("username");
    }

    @Test
    void rejectsUsernameShorterThanThreeCharacters() {
        RegisterRequest request = new RegisterRequest("ab", "ana@example.com", "Password123", "Password123");

        assertThat(invalidFields(request)).contains("username");
    }

    @Test
    void rejectsUsernameLongerThanFiftyCharacters() {
        String username = "a".repeat(51);
        RegisterRequest request = new RegisterRequest(username, "ana@example.com", "Password123", "Password123");

        assertThat(invalidFields(request)).contains("username");
    }

    @Test
    void rejectsInvalidEmail() {
        RegisterRequest request = new RegisterRequest("ana", "not-an-email", "Password123", "Password123");

        assertThat(invalidFields(request)).contains("email");
    }

    @Test
    void rejectsPasswordWithSpaces() {
        RegisterRequest request = new RegisterRequest("ana", "ana@example.com", "Password 123", "Password 123");

        assertThat(invalidFields(request)).contains("password");
    }

    @Test
    void rejectsPasswordShorterThanEightCharacters() {
        RegisterRequest request = new RegisterRequest("ana", "ana@example.com", "Pass123", "Pass123");

        assertThat(invalidFields(request)).contains("password");
    }

    @Test
    void rejectsPasswordLongerThanTwentyCharacters() {
        String password = "a".repeat(21);
        RegisterRequest request = new RegisterRequest("ana", "ana@example.com", password, password);

        assertThat(invalidFields(request)).contains("password");
    }

    @Test
    void rejectsMismatchedPasswordConfirmation() {
        RegisterRequest request = new RegisterRequest("ana", "ana@example.com", "Password123", "Password321");

        assertThat(invalidFields(request)).contains("passwordConfirmed");
    }
}
