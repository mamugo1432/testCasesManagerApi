package es.test.manager.infraestructure.adapter.in.rest;

import es.test.manager.application.dto.project.CreateProjectRequest;
import es.test.manager.application.dto.project.ProjectResponse;
import es.test.manager.domain.model.AuthenticatedUser;
import es.test.manager.domain.port.in.CreateProjectUseCase;
import es.test.manager.domain.port.in.DeleteProjectUseCase;
import es.test.manager.domain.port.in.GetProjectUseCase;
import es.test.manager.domain.port.in.ListProjectsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Project endpoints. The owner always comes from the JWT principal, never from the request. */
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Tag(name = "Proyectos")
public class ProjectController {

    private final CreateProjectUseCase createProjectUseCase;
    private final ListProjectsUseCase listProjectsUseCase;
    private final GetProjectUseCase getProjectUseCase;
    private final DeleteProjectUseCase deleteProjectUseCase;

    @PostMapping
    @Operation(
            summary = "Crea un proyecto",
            description = "El propietario sale del JWT y solo se usa el título del cuerpo.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Proyecto creado"),
        @ApiResponse(responseCode = "400", description = "Título inválido o cuerpo mal formado"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "405", description = "Método no soportado"),
        @ApiResponse(responseCode = "409", description = "Límite de proyectos alcanzado"),
        @ApiResponse(responseCode = "415", description = "Content-Type no soportado"),
        @ApiResponse(responseCode = "500", description = "Error interno")
    })
    public ResponseEntity<ProjectResponse> create(
            @Valid @RequestBody CreateProjectRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(createProjectUseCase.create(request, user));
    }

    @GetMapping
    @Operation(summary = "Lista los proyectos del usuario autenticado")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Listado de proyectos"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "405", description = "Método no soportado"),
        @ApiResponse(responseCode = "500", description = "Error interno")
    })
    public ResponseEntity<List<ProjectResponse>> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(listProjectsUseCase.list(user));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene el detalle de un proyecto propio")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Detalle del proyecto"),
        @ApiResponse(responseCode = "400", description = "Identificador no válido"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "404", description = "Proyecto inexistente o ajeno"),
        @ApiResponse(responseCode = "405", description = "Método no soportado"),
        @ApiResponse(responseCode = "500", description = "Error interno")
    })
    public ResponseEntity<ProjectResponse> get(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(getProjectUseCase.get(id, user));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina un proyecto propio y sus datos dependientes")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Proyecto eliminado"),
        @ApiResponse(responseCode = "400", description = "Identificador no válido"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "404", description = "Proyecto inexistente o ajeno"),
        @ApiResponse(responseCode = "405", description = "Método no soportado"),
        @ApiResponse(responseCode = "500", description = "Error interno")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser user) {
        deleteProjectUseCase.delete(id, user);
        return ResponseEntity.noContent().build();
    }
}
