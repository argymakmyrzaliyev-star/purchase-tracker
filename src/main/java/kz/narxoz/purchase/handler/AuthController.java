package kz.narxoz.purchase.handler;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import kz.narxoz.purchase.security.TokenService;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class AuthController {
    private final AuthenticationManager login;
    private final TokenService tokens;

    public AuthController(AuthenticationManager login, TokenService tokens) {
        this.login = login;
        this.tokens = tokens;
    }

    record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    record TokenResponse(String accessToken, String tokenType, long expiresIn) {}
    record Identity(String username, List<String> authorities) {}

    @PostMapping("/api/v1/auth/login")
    @ApiResponse(responseCode = "200", description = "Bearer token issued")
    @ApiResponse(responseCode = "400", description = "Invalid login fields")
    @ApiResponse(responseCode = "401", description = "Bad username or password")
    TokenResponse login(@Valid @RequestBody LoginRequest body) {
        var credentials = UsernamePasswordAuthenticationToken.unauthenticated(body.username(), body.password());
        return new TokenResponse(tokens.issue(login.authenticate(credentials)), "Bearer", TokenService.LIFETIME_SECONDS);
    }

    @GetMapping("/api/me")
    @ApiResponse(responseCode = "200", description = "Current caller")
    @ApiResponse(responseCode = "401", description = "Valid token required")
    Identity me(Authentication caller) {
        return new Identity(caller.getName(), caller.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).toList());
    }
}
