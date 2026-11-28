package kz.narxoz.purchase.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import java.time.Instant;

@Service
public class TokenService {
    public static final long LIFETIME_SECONDS = 900;
    private final JwtEncoder encoder;
    private final String issuer;

    public TokenService(JwtEncoder encoder, @Value("${spring.application.name}") String issuer) {
        this.encoder = encoder;
        this.issuer = issuer;
    }

    public String issue(Authentication user) {
        var roles = user.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .filter(role -> role.startsWith("ROLE_")).map(role -> role.substring(5)).toList();
        var now = Instant.now();
        var claims = JwtClaimsSet.builder().issuer(issuer).subject(user.getName()).id(java.util.UUID.randomUUID().toString()).issuedAt(now)
                .expiresAt(now.plusSeconds(LIFETIME_SECONDS)).claim("roles", roles).build();
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
