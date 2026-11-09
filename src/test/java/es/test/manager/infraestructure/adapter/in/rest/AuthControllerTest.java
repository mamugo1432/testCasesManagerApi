package es.test.manager.infraestructure.adapter.in.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import es.test.manager.application.dto.auth.AuthResponse;
import es.test.manager.application.dto.auth.LoginRequest;
import es.test.manager.application.dto.auth.RegisterRequest;
import es.test.manager.application.dto.auth.UserResponse;
import es.test.manager.application.service.AuthService;
import es.test.manager.domain.exception.InvalidCredentialsException;
import es.test.manager.domain.exception.InvalidEmailException;
import es.test.manager.domain.exception.InvalidUsernameException;
import es.test.manager.domain.model.AuthenticatedUser;
import es.test.manager.domain.model.Role;
import es.test.manager.domain.port.in.LoginUseCase;
import es.test.manager.domain.port.in.RegisterUseCase;
import es.test.manager.domain.port.in.SessionUseCase;
import es.test.manager.domain.port.out.CredentialsAuthenticator;
import es.test.manager.domain.port.out.UserRepository;
import es.test.manager.infraestructure.exception.GlobalExceptionHandler;
import es.test.manager.infraestructure.security.JwtAuthenticationFilter;
import es.test.manager.infraestructure.security.JwtTokenService;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/** HTTP contract of the authentication endpoints. */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private static final String REGISTER_BODY =
            """
            {
              "username": "ana",
              "email": "ana@example.com",
              "password": "Password123",
              "confirmPassword": "Password123"
            }
            """;

    private static final String JWT_SECRET = "test-secret-0123456789-0123456789-0123456789";

    private static final String LOGIN_BODY =
            """
            {
              "identifier": "ana",
              "password": "Password123"
            }
            """;

    @Mock
    private RegisterUseCase registerUseCase;

    @Mock
    private LoginUseCase loginUseCase;

    @Mock
    private SessionUseCase sessionUseCase;

    private LocalValidatorFactoryBean validator;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        AuthController controller = new AuthController(registerUseCase, loginUseCase, sessionUseCase);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        validator.close();
    }

    @Test
    void registerReturns201WithoutToken() throws Exception {
        given(registerUseCase.register(any(RegisterRequest.class)))
                .willReturn(new UserResponse(1L, "ana", "ana@example.com", Role.USER));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("ana"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }

    @Test
    void registerReturns400WithFieldErrors() throws Exception {
        String invalidBody =
                """
                {
                  "username": "ab",
                  "email": "ana@example.com",
                  "password": "Password123",
                  "confirmPassword": "Password123"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Datos de entrada inválidos"))
                .andExpect(jsonPath("$.fields[0].field").value("username"));
    }

    @Test
    void registerReturns409WhenTheUsernameIsTaken() throws Exception {
        given(registerUseCase.register(any(RegisterRequest.class))).willThrow(new InvalidUsernameException());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("El username ya está en uso"))
                .andExpect(jsonPath("$.fields[0].field").value("username"));
    }

    @Test
    void registerReturns409WhenTheEmailIsTaken() throws Exception {
        given(registerUseCase.register(any(RegisterRequest.class))).willThrow(new InvalidEmailException());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("El email ya está en uso"))
                .andExpect(jsonPath("$.fields[0].field").value("email"));
    }

    @Test
    void loginReturns200WithTheAccessToken() throws Exception {
        given(loginUseCase.login(any(LoginRequest.class))).willReturn(new AuthResponse("jwt-token"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-token"));
    }

    @Test
    void loginReturns401WithAGenericMessage() throws Exception {
        given(loginUseCase.login(any(LoginRequest.class))).willThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciales inválidas"));
    }

    @Test
    void meReturnsTheAuthenticatedUser() throws Exception {
        AuthenticatedUser principal = new AuthenticatedUser(5L, "ana", "ana@example.com", Role.USER);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        given(sessionUseCase.getCurrentUser(any(AuthenticatedUser.class)))
                .willReturn(new UserResponse(5L, "ana", "ana@example.com", Role.USER));

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.username").value("ana"))
                .andExpect(jsonPath("$.email").value("ana@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void meReturnsTheUserWhenAValidBearerTokenIsSent() throws Exception {
        JwtTokenService tokenService = new JwtTokenService(JWT_SECRET, Duration.ofHours(1));
        AuthService authService = new AuthService(
                mock(UserRepository.class),
                mock(PasswordEncoder.class),
                mock(CredentialsAuthenticator.class),
                tokenService);
        MockMvc tokenMockMvc = MockMvcBuilders.standaloneSetup(
                        new AuthController(registerUseCase, loginUseCase, authService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .addFilters(new JwtAuthenticationFilter(tokenService))
                .build();
        String token = tokenService.generateToken(new AuthenticatedUser(5L, "ana", "ana@example.com", Role.USER));

        tokenMockMvc
                .perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.username").value("ana"))
                .andExpect(jsonPath("$.email").value("ana@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }
}
