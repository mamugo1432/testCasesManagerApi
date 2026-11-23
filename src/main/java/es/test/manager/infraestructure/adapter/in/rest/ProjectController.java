package es.test.manager.infraestructure.adapter.in.rest;

import es.test.manager.application.dto.project.CreateProjectRequest;
import es.test.manager.application.dto.project.ProjectResponse;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Project endpoints. Role-based authorization lives in {@code SecurityConfig}; ownership in the use cases. */
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
        @ApiResponse(responseCode = "403", description = "Rol no autorizado"),
        @ApiResponse(responseCode = "405", description = "Método no soportado"),
        @ApiResponse(responseCode = "409", description = "Límite de proyectos alcanzado"),
        @ApiResponse(responseCode = "415", description = "Content-Type no soportado"),
        @ApiResponse(responseCode = "500", description = "Error interno")
    })
    public ResponseEntity<ProjectResponse> create(@Valid @RequestBody CreateProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(createProjectUseCase.create(request));
    }

    @GetMapping
    @Operation(summary = "Lista los proyectos")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Listado de proyectos"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "Rol no autorizado"),
        @ApiResponse(responseCode = "405", description = "Método no soportado"),
        @ApiResponse(responseCode = "500", description = "Error interno")
    })
    public ResponseEntity<List<ProjectResponse>> list() {
        return ResponseEntity.ok(listProjectsUseCase.list());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene el detalle de un proyecto")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Detalle del proyecto"),
        @ApiResponse(responseCode = "400", description = "Identificador no válido"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "Rol no autorizado"),
        @ApiResponse(responseCode = "404", description = "Proyecto inexistente"),
        @ApiResponse(responseCode = "405", description = "Método no soportado"),
        @ApiResponse(responseCode = "500", description = "Error interno")
    })
    public ResponseEntity<ProjectResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(getProjectUseCase.get(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina un proyecto y sus datos dependientes")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Proyecto eliminado"),
        @ApiResponse(responseCode = "400", description = "Identificador no válido"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "Rol no autorizado"),
        @ApiResponse(responseCode = "404", description = "Proyecto inexistente"),
        @ApiResponse(responseCode = "405", description = "Método no soportado"),
        @ApiResponse(responseCode = "500", description = "Error interno")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        deleteProjectUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }
}
