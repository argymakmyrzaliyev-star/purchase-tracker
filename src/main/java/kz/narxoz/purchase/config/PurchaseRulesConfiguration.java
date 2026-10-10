package kz.narxoz.purchase.config;

import kz.narxoz.purchase.domain.Rule;
import kz.narxoz.purchase.domain.TransitionRule;
import kz.narxoz.purchase.domain.UnapprovedCannotOrder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class PurchaseRulesConfiguration {
    @Bean
    TransitionRule transitionRule() {
        return new TransitionRule();
    }

    @Bean
    UnapprovedCannotOrder unapprovedCannotOrder() {
        return new UnapprovedCannotOrder();
    }

    @Bean
    @Primary
    Rule purchaseRules(TransitionRule transitions, UnapprovedCannotOrder approvalGuard) {
        return (from, to) -> {
            approvalGuard.check(from, to);
            transitions.check(from, to);
        };
    }
}
