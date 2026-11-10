package es.test.manager.infraestructure.adapter.in.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import es.test.manager.domain.exception.InvalidTokenException;
import es.test.manager.domain.model.AuthenticatedUser;
import es.test.manager.domain.model.Role;
import es.test.manager.domain.port.in.CreateProjectUseCase;
import es.test.manager.domain.port.in.DeleteProjectUseCase;
import es.test.manager.domain.port.in.GetProjectUseCase;
import es.test.manager.domain.port.in.ListProjectsUseCase;
import es.test.manager.domain.port.out.TokenService;
import es.test.manager.infraestructure.config.SecurityConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Exercises the real security chain ({@link SecurityConfig} + JWT filter) over the project
 * endpoints, so an endpoint accidentally made public or a broken filter would fail here (RF-001).
 */
@WebMvcTest(ProjectController.class)
@Import(SecurityConfig.class)
class ProjectEndpointSecurityTest {

    private static final AuthenticatedUser USER = new AuthenticatedUser(7L, "ana", "ana@example.com", Role.USER);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateProjectUseCase createProjectUseCase;

    @MockitoBean
    private ListProjectsUseCase listProjectsUseCase;

    @MockitoBean
    private GetProjectUseCase getProjectUseCase;

    @MockitoBean
    private DeleteProjectUseCase deleteProjectUseCase;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void requestsWithoutATokenAreRejectedByTheRealSecurityChain() throws Exception {
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("No autenticado"));
    }

    @Test
    void invalidTokensAreRejectedByTheRealJwtFilter() throws Exception {
        given(tokenService.extractUser("bad-token")).willThrow(new InvalidTokenException());

        mockMvc.perform(get("/api/projects").header(HttpHeaders.AUTHORIZATION, "Bearer bad-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("No autenticado"));
    }

    @Test
    void validTokensReachTheControllerWithThePrincipalFromTheToken() throws Exception {
        given(tokenService.extractUser("good-token")).willReturn(USER);
        given(listProjectsUseCase.list(any(AuthenticatedUser.class))).willReturn(List.of());

        mockMvc.perform(get("/api/projects").header(HttpHeaders.AUTHORIZATION, "Bearer good-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
