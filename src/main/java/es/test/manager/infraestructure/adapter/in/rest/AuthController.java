package es.test.manager.infraestructure.adapter.in.rest;

import es.test.manager.application.dto.auth.AuthResponse;
import es.test.manager.application.dto.auth.LoginRequest;
import es.test.manager.application.dto.auth.RegisterRequest;
import es.test.manager.application.dto.auth.UserResponse;
import es.test.manager.domain.model.AuthenticatedUser;
import es.test.manager.domain.port.in.LoginUseCase;
import es.test.manager.domain.port.in.RegisterUseCase;
import es.test.manager.domain.port.in.SessionUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Authentication endpoints. */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación")
public class AuthController {

    private final RegisterUseCase registerUseCase;
    private final LoginUseCase loginUseCase;
    private final SessionUseCase sessionUseCase;

    @PostMapping("/register")
    @Operation(summary = "Registra una cuenta nueva")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(registerUseCase.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Inicia sesión con username o email")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(loginUseCase.login(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Devuelve el usuario autenticado")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(sessionUseCase.getCurrentUser(user));
    }
}
