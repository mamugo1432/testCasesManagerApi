package es.test.manager.infraestructure.exception;

import static org.assertj.core.api.Assertions.assertThat;

import es.test.manager.application.dto.error.ApiError;
import es.test.manager.application.dto.error.FieldError;
import es.test.manager.domain.exception.InvalidCredentialsException;
import es.test.manager.domain.exception.InvalidEmailException;
import es.test.manager.domain.exception.InvalidTokenException;
import es.test.manager.domain.exception.InvalidUsernameException;
import es.test.manager.domain.exception.ProjectLimitExceededException;
import es.test.manager.domain.exception.ProjectNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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

    @Test
    void projectNotFoundMapsTo404WithTheSameGenericMessage() {
        ResponseEntity<ApiError> response = handler.handleProjectNotFound(new ProjectNotFoundException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Proyecto no encontrado");
        assertThat(response.getBody().fields()).isEmpty();
    }

    @Test
    void projectLimitExceededMapsTo409WithTheLimitMessage() {
        ResponseEntity<ApiError> response = handler.handleProjectLimit(new ProjectLimitExceededException(10));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message())
                .isEqualTo("No puedes crear más de 10 proyectos. Elimina uno para crear otro.");
        assertThat(response.getBody().fields()).isEmpty();
    }

    @Test
    void validationMapsTo400WithTheInvalidFields() throws Exception {
        BindingResult bindingResult = new BeanPropertyBindingResult(new Payload(), "request");
        bindingResult.rejectValue("title", "NotBlank", "El título es obligatorio");
        MethodParameter parameter =
                new MethodParameter(GlobalExceptionHandlerTest.class.getDeclaredMethod("dummy"), -1);

        ResponseEntity<ApiError> response =
                handler.handleValidation(new MethodArgumentNotValidException(parameter, bindingResult));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Datos de entrada inválidos");
        assertThat(response.getBody().fields())
                .containsExactly(new FieldError("title", "El título es obligatorio"));
    }

    @Test
    void invalidIdTypeMapsTo400() {
        ResponseEntity<ApiError> response = handler.handleTypeMismatch(
                new MethodArgumentTypeMismatchException("abc", Long.class, "id", null, new NumberFormatException()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Identificador no válido");
    }

    @Test
    void malformedJsonMapsTo400() {
        ResponseEntity<ApiError> response = handler.handleNotReadable(
                new HttpMessageNotReadableException("malformed", new MockHttpInputMessage(new byte[0])));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Cuerpo de la petición inválido");
    }

    @Test
    void unsupportedMethodMapsTo405() {
        ResponseEntity<ApiError> response =
                handler.handleMethodNotSupported(new HttpRequestMethodNotSupportedException("PATCH"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Método HTTP no soportado");
    }

    @Test
    void unsupportedContentTypeMapsTo415() {
        ResponseEntity<ApiError> response =
                handler.handleMediaTypeNotSupported(new HttpMediaTypeNotSupportedException("text/plain"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Tipo de contenido no soportado");
    }

    @Test
    void unexpectedErrorsMapTo500WithoutLeakingInternals() {
        ResponseEntity<ApiError> response = handler.handleUnexpected(new RuntimeException("secreto interno"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Error interno del servidor");
        assertThat(response.getBody().message()).doesNotContain("secreto");
        assertThat(response.getBody().fields()).isEmpty();
    }

    @SuppressWarnings("unused")
    private void dummy() {}

    /** Bean with a mutable title used to build the validation error. */
    public static class Payload {

        private String title;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }
}
