package kz.narxoz.purchase.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import kz.narxoz.purchase.handler.ApiError;
import org.springframework.web.filter.OncePerRequestFilter;

/** Bound memory and count login attempts by direct peer, never by a spoofable forwarded header. */
public class LoginRateLimitFilter extends OncePerRequestFilter {
    private record Window(long started, int attempts) {}
    private final Map<String, Window> windows = new HashMap<>();
    private final ObjectMapper json;
    public LoginRateLimitFilter(ObjectMapper json) { this.json = json; }
    private synchronized boolean allowed(String peer) {
        long now = System.nanoTime();
        windows.values().removeIf(w -> now - w.started() >= 60_000_000_000L);
        Window w = windows.get(peer);
        if (w == null) {
            if (windows.size() >= 4096) return false;
            windows.put(peer, new Window(now, 1));
            return true;
        }
        if (w.attempts() >= 10) return false;
        windows.put(peer, new Window(w.started(), w.attempts() + 1));
        return true;
    }
    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        if ("POST".equals(req.getMethod()) && "/api/v1/auth/login".equals(req.getServletPath())
                && !allowed(req.getRemoteAddr())) {
            var error = ApiError.forRequest("RATE_LIMITED", "too many login attempts; retry in one minute", req);
            res.setStatus(429);
            res.setContentType("application/json");
            res.setHeader("Retry-After", "60");
            res.setHeader("X-Correlation-Id", error.correlationId());
            json.writeValue(res.getOutputStream(), error);
            return;
        }
        chain.doFilter(req, res);
    }
}
