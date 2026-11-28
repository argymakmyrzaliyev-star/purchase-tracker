package kz.narxoz.purchase.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class LoginRateLimitFilterTest {
    @Test
    void limitsEleventhAttemptIgnoresForgedForwardingAndDoesNotBlockRead() throws Exception {
        var filter = new LoginRateLimitFilter(new ObjectMapper());
        var passed = new AtomicInteger();
        for (int i = 0; i < 11; i++) {
            var req = new MockHttpServletRequest("POST", "/api/v1/auth/login");
            req.setServletPath("/api/v1/auth/login");
            req.setRemoteAddr("127.0.0.1");
            req.addHeader("X-Forwarded-For", "192.0.2." + i);
            var res = new MockHttpServletResponse();
            filter.doFilter(req, res, (a, b) -> passed.incrementAndGet());
            if (i == 10) {
                assertEquals(429, res.getStatus());
                assertEquals("60", res.getHeader("Retry-After"));
                assertTrue(res.getContentAsString().contains("RATE_LIMITED"));
            }
        }
        assertEquals(10, passed.get());
        var read = new MockHttpServletRequest("GET", "/api/me");
        filter.doFilter(read, new MockHttpServletResponse(), (a, b) -> passed.incrementAndGet());
        assertEquals(11, passed.get());
    }
    @Test
    void correlationIdRejectsUntrustedLongAndControlText() {
        var req = new MockHttpServletRequest();
        req.addHeader("X-Correlation-Id", "x".repeat(1000));
        String id = kz.narxoz.purchase.handler.ApiError.correlationId(req);
        assertDoesNotThrow(() -> java.util.UUID.fromString(id));
    }
}
