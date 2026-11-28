package kz.narxoz.purchase.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class TokenServiceTest {
    @Test
    void issuesSignedPurchaseTokenForFifteenMinutes() {
        var config = new JwtConfig();
        var revocations = new TokenRevocations();
        var key = config.jwtKey("a-test-key-used-only-by-token-service-tests");
        var service = new TokenService(config.jwtEncoder(key), "purchase-tracker");
        var user = UsernamePasswordAuthenticationToken.authenticated("requester", null,
                List.of(new SimpleGrantedAuthority("ROLE_REQUESTER")));
        var raw = service.issue(user);
        var jwt = config.jwtDecoder(key, "purchase-tracker", revocations).decode(raw);
        assertEquals("requester", jwt.getSubject());
        assertEquals("purchase-tracker", jwt.getClaimAsString("iss"));
        assertEquals("HS256", jwt.getHeaders().get("alg"));
        assertEquals(List.of("REQUESTER"), jwt.getClaimAsStringList("roles"));
        assertEquals(Duration.ofMinutes(15), Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()));
        assertTrue(jwt.getExpiresAt().isAfter(Instant.now()));
        assertThrows(JwtException.class, () -> config.jwtDecoder(key, "other-product", revocations).decode(raw));
        assertThrows(IllegalArgumentException.class, () -> config.jwtKey("short"));
        revocations.revoke(jwt);
        assertThrows(JwtException.class, () -> config.jwtDecoder(key, "purchase-tracker", revocations).decode(raw));
        assertDoesNotThrow(() -> config.jwtDecoder(key, "purchase-tracker", revocations).decode(service.issue(user)));
    }
}
