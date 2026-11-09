package es.test.manager.infraestructure.exception;

import static org.assertj.core.api.Assertions.assertThat;

import es.test.manager.application.dto.error.ApiError;
import es.test.manager.application.dto.error.FieldError;
import es.test.manager.domain.exception.InvalidCredentialsException;
import es.test.manager.domain.exception.InvalidEmailException;
import es.test.manager.domain.exception.InvalidTokenException;
import es.test.manager.domain.exception.InvalidUsernameException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** Every domain exception produces the expected status and structured body, with no internals. */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void invalidUsernameMapsTo409() {
        ResponseEntity<ApiError> response = handler.handleInvalidUsername(new InvalidUsernameException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("El username ya está en uso");
        assertThat(response.getBody().fields())
                .containsExactly(new FieldError("username", "El username ya está en uso"));
    }

    @Test
    void invalidEmailMapsTo409() {
        ResponseEntity<ApiError> response = handler.handleInvalidEmail(new InvalidEmailException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("El email ya está en uso");
        assertThat(response.getBody().fields()).containsExactly(new FieldError("email", "El email ya está en uso"));
    }

    @Test
    void invalidCredentialsMapsTo401WithAGenericMessage() {
        ResponseEntity<ApiError> response = handler.handleInvalidCredentials(new InvalidCredentialsException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Credenciales inválidas");
        assertThat(response.getBody().fields()).isEmpty();
    }

    @Test
    void invalidTokenMapsTo401WithAGenericMessage() {
        ResponseEntity<ApiError> response = handler.handleInvalidToken(new InvalidTokenException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Token inválido o caducado");
        assertThat(response.getBody().fields()).isEmpty();
    }
}
