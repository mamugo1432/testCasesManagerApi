package es.test.manager.infraestructure.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;

import es.test.manager.infraestructure.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** springdoc metadata of the four project endpoints (RF-001 support). */
class ProjectControllerOpenApiTest {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Test
    void tagsTheControllerAsProjects() {
        assertThat(ProjectController.class.getAnnotation(Tag.class).name()).isEqualTo("Proyectos");
    }

    @Test
    void documentsEveryEndpointWithBearerSecurityAndItsResponseCodes() {
        assertEndpoint("create", "201", "400", "401", "405", "409", "415", "500");
        assertEndpoint("list", "200", "401", "405", "500");
        assertEndpoint("get", "200", "400", "401", "404", "405", "500");
        assertEndpoint("delete", "204", "400", "401", "404", "405", "500");
    }

    @Test
    void registersTheBearerSchemeInOpenApi() {
        assertThat(new OpenApiConfig().testCaseManagerOpenApi().getComponents().getSecuritySchemes())
                .containsKey(BEARER_SCHEME);
    }

    private static void assertEndpoint(String methodName, String... responseCodes) {
        Method method = Arrays.stream(ProjectController.class.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(methodName))
                .findFirst()
                .orElseThrow();

        assertThat(method.getAnnotation(Operation.class)).isNotNull();
        assertThat(method.getAnnotation(SecurityRequirement.class).name()).isEqualTo(BEARER_SCHEME);
        Set<String> documentedCodes = Arrays.stream(
                        method.getAnnotation(ApiResponses.class).value())
                .map(ApiResponse::responseCode)
                .collect(Collectors.toSet());
        assertThat(documentedCodes).contains(responseCodes);
    }
}
