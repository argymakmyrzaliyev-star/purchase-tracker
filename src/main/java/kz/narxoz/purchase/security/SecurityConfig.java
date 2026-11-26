package kz.narxoz.purchase.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kz.narxoz.purchase.handler.ApiError;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.*;
import org.springframework.security.web.*;
import org.springframework.security.web.access.AccessDeniedHandler;
import java.io.IOException;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Configuration
@EnableMethodSecurity
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SecurityConfig {
    private static final String PURCHASES = "/api/v1/purchase-requests";

    @Bean
    SecurityFilterChain api(HttpSecurity http, ObjectMapper json) throws Exception {
        AuthenticationEntryPoint unauthenticated = (req, res, ex) -> {
            res.setHeader("WWW-Authenticate", "Bearer");
            write(json, req, res, 401, "UNAUTHENTICATED", "valid token required");
        };
        AccessDeniedHandler forbidden = (req, res, ex) ->
                write(json, req, res, 403, "FORBIDDEN", "not allowed for your role");
        var roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles");
        roles.setAuthorityPrefix("ROLE_");
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);
        return http.csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(r -> r
                        .requestMatchers(POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers(GET, "/", "/index.html", "/portal.css", "/portal.js",
                                "/actuator/health", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(POST, PURCHASES + "/*/approve").hasRole("APPROVER")
                        .requestMatchers(POST, PURCHASES).hasAnyRole("REQUESTER", "APPROVER")
                        .requestMatchers(GET, PURCHASES + "/*", "/api/me").hasAnyRole("REQUESTER", "APPROVER")
                        .anyRequest().denyAll())
                .exceptionHandling(e -> e.authenticationEntryPoint(unauthenticated).accessDeniedHandler(forbidden))
                .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(converter))
                        .authenticationEntryPoint(unauthenticated).accessDeniedHandler(forbidden))
                .build();
    }

    private static void write(ObjectMapper json, HttpServletRequest req, HttpServletResponse res,
                              int status, String code, String message) throws IOException {
        var error = ApiError.forRequest(code, message, req);
        res.setStatus(status);
        res.setContentType(APPLICATION_JSON_VALUE);
        res.setHeader("X-Correlation-Id", error.correlationId());
        json.writeValue(res.getOutputStream(), error);
    }
}
