package kz.narxoz.purchase.config;

import kz.narxoz.purchase.client.DownstreamProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;
import java.time.Duration;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("dev")
@AutoConfigureMockMvc
class DevProfileTest {
    private static final String API = "$.paths['/api/v1/purchase-requests']";
    private static final String ITEM_API = "$.paths['/api/v1/purchase-requests/{id}']";

    @Autowired
    private DownstreamProperties downstream;

    @Autowired
    private MockMvc mvc;

    @Test
    void bindsDownstreamConfigurationWithoutCallingIt() {
        assertEquals(URI.create("http://localhost:9090"), downstream.baseUrl());
        assertEquals(Duration.ofSeconds(2), downstream.timeout());
    }

    @Test
    void publishesOnlySafeActuatorData() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
        mvc.perform(get("/actuator/info"))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"env", "beans", "heapdump", "shutdown"})
    void otherActuatorEndpointsAreNotExposed(String endpoint) throws Exception {
        mvc.perform(get("/actuator/{endpoint}", endpoint)).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"requester", "unregistered"})
    void badCredentialsShareOneResponse(String username) throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(APPLICATION_JSON)
                        .header("X-Correlation-Id", "login-check")
                        .content("{\"username\":\"" + username + "\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.message").value("bad username or password"))
                .andExpect(jsonPath("$.correlationId").value("login-check"));
    }

    @Test
    void blankLoginFieldIsInvalid() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"irrelevant\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"))
                .andExpect(jsonPath("$.message").value("username must not be blank"));
    }

    @Test
    void publishesOpenApiContract() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/auth/login'].post.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/me'].get.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/purchase-requests/{id}/approve'].post.responses['403']").exists())
                .andExpect(jsonPath(API + ".post.responses['201'].headers.Location").exists())
                .andExpect(jsonPath(API + ".post.responses['400']").exists())
                .andExpect(jsonPath(API + ".post.responses['409']").exists())
                .andExpect(jsonPath(ITEM_API + ".get.responses['200']").exists())
                .andExpect(jsonPath(ITEM_API + ".get.responses['404']").exists())
                .andExpect(jsonPath("$.components.schemas.CreatePurchase.required",
                        containsInAnyOrder("businessKey", "title")))
                .andExpect(jsonPath("$.components.schemas.PurchaseResponse.properties.id.format").value("uuid"))
                .andExpect(jsonPath("$.components.schemas.PurchaseResponse.properties.createdAt").doesNotExist());
    }
}
