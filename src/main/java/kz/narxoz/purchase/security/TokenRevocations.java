package kz.narxoz.purchase.security;

import org.springframework.stereotype.Component;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/** Single-process demo: expires entries with the original token. Restart rotates the dev key. */
@Component
public class TokenRevocations {
    private final Map<String, Instant> revoked = new HashMap<>();
    public synchronized void revoke(Jwt jwt) {
        purge();
        if (jwt.getId() == null || jwt.getExpiresAt() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        if (!revoked.containsKey(jwt.getId()) && revoked.size() >= 10000) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
        }
        revoked.put(jwt.getId(), jwt.getExpiresAt());
    }
    public synchronized boolean valid(Jwt jwt) {
        purge();
        return jwt.getId() != null && !revoked.containsKey(jwt.getId());
    }
    private void purge() {
        Instant now = Instant.now();
        revoked.values().removeIf(exp -> !exp.isAfter(now));
    }
}
