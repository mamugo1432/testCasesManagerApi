package es.test.manager.application.dto.auth;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Registration payload. Field constraints cover the input rules (RF-002 to RF-005). The username is
 * validated and stored exactly as received, outer spaces included.
 */
public record RegisterRequest(
        @NotBlank(message = "El username es obligatorio")
                @Size(min = 3, max = 50, message = "El username debe tener entre 3 y 50 caracteres")
                String username,
        @NotBlank(message = "El email es obligatorio")
                @Email(message = "El email no tiene un formato válido")
                @Size(max = 255, message = "El email no puede superar los 255 caracteres")
                String email,
        @NotBlank(message = "La contraseña es obligatoria")
                @Size(min = 8, max = 20, message = "La contraseña debe tener entre 8 y 20 caracteres")
                @Pattern(regexp = "\\S+", message = "La contraseña no puede contener espacios")
                String password,
        @NotBlank(message = "La confirmación de contraseña es obligatoria") String confirmPassword) {

    /**
     * Bean Validation publishes this violation as {@code passwordConfirmed}; the frontend maps that
     * name to the confirmation input (the payload field is {@code confirmPassword}).
     */
    @AssertTrue(message = "Las contraseñas no coinciden")
    public boolean isPasswordConfirmed() {
        return password != null && password.equals(confirmPassword);
    }
}
