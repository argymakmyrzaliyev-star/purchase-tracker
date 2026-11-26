package kz.narxoz.purchase.handler;

import org.springframework.context.annotation.Import;
import kz.narxoz.purchase.security.SecurityConfig;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.authentication.AuthenticationManager;
import kz.narxoz.purchase.security.TokenService;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import kz.narxoz.purchase.config.Application;
import kz.narxoz.purchase.domain.DuplicatePurchase;
import kz.narxoz.purchase.domain.MissingPurchase;
import kz.narxoz.purchase.domain.PurchaseId;
import kz.narxoz.purchase.domain.PurchaseKey;
import kz.narxoz.purchase.domain.PurchaseRequest;
import kz.narxoz.purchase.domain.PurchaseStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.mockito.Mockito.verify;
import static kz.narxoz.purchase.handler.PurchaseController.PATH;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({PurchaseController.class, AuthController.class})
@Import(SecurityConfig.class)
@ContextConfiguration(classes = Application.class)
class PurchaseControllerTest {
    private static final String KEY = "PR-19";
    private static final String TITLE = "Monitors for room 3";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @MockitoBean
    private PurchaseService purchases;

    @MockitoBean
    private JwtDecoder decoder;
    @MockitoBean
    private AuthenticationManager login;
    @MockitoBean
    private TokenService tokens;

    private static RequestPostProcessor as(String role) {
        return jwt().jwt(j -> j.subject("classroom-user"))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Test
    void createsDraftAndReturnsLocation() throws Exception {
        assertCreated(TITLE);
    }

    @Test
    void keepsFullTitleAtTheLimit() throws Exception {
        assertCreated("x".repeat(120));
    }

    @Test
    void readsPurchaseByUuid() throws Exception {
        PurchaseRequest purchase = draft(PurchaseId.newId(), TITLE);
        when(purchases.require(purchase.id())).thenReturn(purchase);

        mvc.perform(get(PATH + "/{id}", purchase.id().value()).with(as("REQUESTER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", aMapWithSize(4)))
                .andExpect(jsonPath("$.id").value(purchase.id().value().toString()))
                .andExpect(jsonPath("$.businessKey").value(KEY))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.title").value(TITLE));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void blankTitleFailsBeforeRegisterAndEchoesCorrelationId(String title) throws Exception {
        mvc.perform(post(PATH).with(as("REQUESTER")).contentType(APPLICATION_JSON).content(body(KEY, title))
                        .header("X-Correlation-Id", "purchase-test"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$", aMapWithSize(3)))
                .andExpect(jsonPath("$.code").value("VALIDATION"))
                .andExpect(jsonPath("$.message").value("title must not be blank"))
                .andExpect(jsonPath("$.correlationId").value("purchase-test"));
        verifyNoInteractions(purchases);
    }

    @Test
    void blankBusinessKeyNamesTheField() throws Exception {
        mvc.perform(post(PATH).with(as("REQUESTER")).contentType(APPLICATION_JSON).content(body("", TITLE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"))
                .andExpect(jsonPath("$.message").value("business key must not be blank"));
        verifyNoInteractions(purchases);
    }

    @Test
    void titleOverTheLimitFailsBeforeRegister() throws Exception {
        mvc.perform(post(PATH).with(as("REQUESTER")).contentType(APPLICATION_JSON).content(body(KEY, "x".repeat(121))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"))
                .andExpect(jsonPath("$.message").value("title size must be between 0 and 120"));
        verifyNoInteractions(purchases);
    }

    @Test
    void missingUuidReturnsNotFound() throws Exception {
        PurchaseId id = PurchaseId.newId();
        when(purchases.require(id)).thenThrow(new MissingPurchase(id));

        mvc.perform(get(PATH + "/{id}", id.value()).with(as("REQUESTER")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void duplicateBusinessKeyReturnsConflict() throws Exception {
        when(purchases.register(any(PurchaseId.class), eq(KEY), eq(TITLE)))
                .thenThrow(new DuplicatePurchase(new PurchaseKey(KEY), null));

        mvc.perform(post(PATH).with(as("REQUESTER")).contentType(APPLICATION_JSON).content(body(KEY, TITLE)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE"));
    }

    @ParameterizedTest
    @ValueSource(strings = {KEY, "1-1-1-1-1"})
    void malformedIdReturnsValidationError(String id) throws Exception {
        mvc.perform(get(PATH + "/{id}", id).with(as("REQUESTER")).header("X-Correlation-Id", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"))
                .andExpect(jsonPath("$.message").value("id must be a UUID"))
                .andExpect(jsonPath("$.correlationId", matchesPattern("[0-9a-f-]{36}")));
        verifyNoInteractions(purchases);
    }

    @Test
    void malformedJsonReturnsValidationError() throws Exception {
        mvc.perform(post(PATH).with(as("REQUESTER")).contentType(APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"))
                .andExpect(jsonPath("$.message").value("body must be JSON"));
        verifyNoInteractions(purchases);
    }

    @Test
    void wrongContentTypeReturnsUnsupported() throws Exception {
        mvc.perform(post(PATH).with(as("REQUESTER")).contentType(TEXT_PLAIN).content("not JSON"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED"));
        verifyNoInteractions(purchases);
    }

    @Test
    void unexpectedFailureHidesExceptionDetails() throws Exception {
        PurchaseId id = PurchaseId.newId();
        when(purchases.require(id)).thenThrow(new IllegalStateException("database detail"));

        mvc.perform(get(PATH + "/{id}", id.value()).with(as("REQUESTER")).header("X-Correlation-Id", "unexpected-test"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$", aMapWithSize(3)))
                .andExpect(jsonPath("$.code").value("INTERNAL"))
                .andExpect(jsonPath("$.message").value("unexpected error"))
                .andExpect(jsonPath("$.correlationId").value("unexpected-test"));
    }

    @Test
    void missingTokenStopsRead() throws Exception {
        mvc.perform(get(PATH + "/{id}", PurchaseId.newId().value()).header("X-Correlation-Id", "test-read"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.message").value("valid token required"))
                .andExpect(jsonPath("$.correlationId").value("test-read"));
        verifyNoInteractions(purchases);
    }

    @Test
    void requesterCannotApprove() throws Exception {
        mvc.perform(post(PATH + "/{id}/approve", PurchaseId.newId().value()).with(as("REQUESTER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("not allowed for your role"))
                .andExpect(jsonPath("$.correlationId").isString());
        verifyNoInteractions(purchases);
    }

    @Test
    void approverActsUnderTokenIdentity() throws Exception {
        PurchaseId id = PurchaseId.newId();
        var approved = new PurchaseRequest(id, new PurchaseKey(KEY), PurchaseStatus.APPROVED, TITLE);
        when(purchases.approve(id, "classroom-user")).thenReturn(approved);
        mvc.perform(post(PATH + "/{id}/approve", id.value()).with(as("APPROVER"))
                        .contentType(APPLICATION_JSON).content("{\"actor\":\"other\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"));
        verify(purchases).approve(id, "classroom-user");
    }

    @Test
    void meUsesAuthenticatedPrincipal() throws Exception {
        mvc.perform(get("/api/me").with(as("REQUESTER"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("classroom-user"))
                .andExpect(jsonPath("$.authorities[0]").value("ROLE_REQUESTER"));
    }

    private void assertCreated(String title) throws Exception {
        when(purchases.register(any(PurchaseId.class), eq(KEY), eq(title)))
                .thenAnswer(call -> draft(call.getArgument(0), title));

        mvc.perform(post(PATH).with(as("REQUESTER")).contentType(APPLICATION_JSON).content(body(KEY, title)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", aMapWithSize(4)))
                .andExpect(jsonPath("$.businessKey").value(KEY))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.title").value(title))
                .andExpect(result -> {
                    String id = json.readTree(result.getResponse().getContentAsByteArray()).get("id").asText();
                    header().string("Location", PATH + "/" + PurchaseId.parse(id).value()).match(result);
                });
    }

    private String body(String key, String title) throws Exception {
        return json.writeValueAsString(new CreatePurchase(key, title));
    }

    private PurchaseRequest draft(PurchaseId id, String title) {
        return new PurchaseRequest(id, new PurchaseKey(KEY), PurchaseStatus.DRAFT, title);
    }
}
