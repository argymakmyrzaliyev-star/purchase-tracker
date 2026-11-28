package kz.narxoz.purchase.security;

import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@Configuration
public class LoginConfig {
    @Bean
    PasswordEncoder passwords() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    @Profile("dev")
    InMemoryUserDetailsManager devUsers(PasswordEncoder passwords,
            @Value("${CSS_REQUESTER_PASSWORD}") String requesterPassword,
            @Value("${CSS_APPROVER_PASSWORD}") String approverPassword) {
        if (requesterPassword.length() < 16 || approverPassword.length() < 16
                || requesterPassword.equals(approverPassword)) {
            throw new IllegalArgumentException("Provide distinct dev passwords of at least 16 characters");
        }
        return new InMemoryUserDetailsManager(
                User.withUsername("requester").password(passwords.encode(requesterPassword)).roles("REQUESTER").build(),
                User.withUsername("approver").password(passwords.encode(approverPassword)).roles("APPROVER").build());
    }

    @Bean
    @Profile("dev")
    AuthenticationManager devLogin(InMemoryUserDetailsManager users, PasswordEncoder passwords) {
        var provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(users);
        provider.setPasswordEncoder(passwords);
        return new ProviderManager(provider);
    }

    @Bean
    @Profile("!dev")
    AuthenticationManager loginWithoutClassroomAccounts() {
        return credentials -> { throw new BadCredentialsException("bad username or password"); };
    }
}
