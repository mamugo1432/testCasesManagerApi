package es.test.manager.application.dto.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Project creation payload. Only the title can be set by the client; the owner and the creation
 * date are assigned by the server. The title is stored exactly as received, without trimming
 * (RF-005 to RF-008, RF-011).
 */
public record CreateProjectRequest(
        @NotBlank(message = "El título es obligatorio")
                @Size(max = 255, message = "El título no puede superar los 255 caracteres")
                String title) {}
