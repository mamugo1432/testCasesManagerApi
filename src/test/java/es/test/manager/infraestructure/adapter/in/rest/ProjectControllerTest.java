package es.test.manager.infraestructure.adapter.in.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import es.test.manager.application.dto.project.CreateProjectRequest;
import es.test.manager.application.dto.project.ProjectResponse;
import es.test.manager.domain.model.AuthenticatedUser;
import es.test.manager.domain.model.Role;
import es.test.manager.domain.port.in.CreateProjectUseCase;
import es.test.manager.domain.port.in.DeleteProjectUseCase;
import es.test.manager.domain.port.in.GetProjectUseCase;
import es.test.manager.domain.port.in.ListProjectsUseCase;
import es.test.manager.infraestructure.exception.GlobalExceptionHandler;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.filter.OncePerRequestFilter;

/** HTTP contract of the project endpoints (RF-001, RF-005, RF-011, RF-016). */
@ExtendWith(MockitoExtension.class)
class ProjectControllerTest {

    private static final AuthenticatedUser USER =
            new AuthenticatedUser(7L, "ana", "ana@example.com", Role.USER);

    @Mock
    private CreateProjectUseCase createProjectUseCase;

    @Mock
    private ListProjectsUseCase listProjectsUseCase;

    private LocalValidatorFactoryBean validator;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        ProjectController controller = new ProjectController(createProjectUseCase, listProjectsUseCase);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .addFilters(requireAuthentication())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        validator.close();
    }

    private static void authenticate() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(USER, null, List.of()));
    }

    /** Mirrors the production security entry point: requests without authentication get 401. */
    private static OncePerRequestFilter requireAuthentication() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(
                    HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                    throws ServletException, IOException {
                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write("{\"message\":\"No autenticado\",\"fields\":[]}");
                    return;
                }
                filterChain.doFilter(request, response);
            }
        };
    }

    @Test
    void createReturns201WithTheCreatedProject() throws Exception {
        authenticate();
        given(createProjectUseCase.create(any(CreateProjectRequest.class), any(AuthenticatedUser.class)))
                .willReturn(new ProjectResponse(1L, "Mi proyecto", "09/11/2026"));

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Mi proyecto\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Mi proyecto"))
                .andExpect(jsonPath("$.createdAt").value("09/11/2026"))
                .andExpect(jsonPath("$.userId").doesNotExist());
    }

    @Test
    void createSendsTheTitleAndTheAuthenticatedUserToTheUseCase() throws Exception {
        authenticate();
        given(createProjectUseCase.create(any(CreateProjectRequest.class), any(AuthenticatedUser.class)))
                .willReturn(new ProjectResponse(1L, "Mi proyecto", "09/11/2026"));

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Mi proyecto\"}"))
                .andExpect(status().isCreated());

        ArgumentCaptor<CreateProjectRequest> request = ArgumentCaptor.forClass(CreateProjectRequest.class);
        verify(createProjectUseCase).create(request.capture(), eq(USER));
        org.assertj.core.api.Assertions.assertThat(request.getValue().title())
                .isEqualTo("Mi proyecto");
    }

    @Test
    void createIgnoresUnknownFieldsAndServerAssignedValues() throws Exception {
        authenticate();
        given(createProjectUseCase.create(any(CreateProjectRequest.class), any(AuthenticatedUser.class)))
                .willReturn(new ProjectResponse(1L, "Mi proyecto", "09/11/2026"));

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Mi proyecto\",\"userId\":99,\"id\":5,\"createdAt\":\"01/01/2000\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void createReturns401WithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Mi proyecto\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("No autenticado"));
    }

    @Test
    void listReturns200WithTheProjects() throws Exception {
        authenticate();
        given(listProjectsUseCase.list(any(AuthenticatedUser.class)))
                .willReturn(List.of(
                        new ProjectResponse(1L, "Primero", "09/11/2026"),
                        new ProjectResponse(2L, "Segundo", "08/11/2026")));

        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Primero"))
                .andExpect(jsonPath("$[0].createdAt").value("09/11/2026"))
                .andExpect(jsonPath("$[1].id").value(2));
    }

    @Test
    void listReturns401WithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("No autenticado"));
    }
}
