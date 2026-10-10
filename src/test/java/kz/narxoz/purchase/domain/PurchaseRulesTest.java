package kz.narxoz.purchase.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PurchaseRulesTest {
    @Test
    void transitionRuleAllowsTheTwoForwardSteps() {
        Rule rule = new TransitionRule();
        assertDoesNotThrow(() -> rule.check(PurchaseStatus.DRAFT, PurchaseStatus.APPROVED));
        assertDoesNotThrow(() -> rule.check(PurchaseStatus.APPROVED, PurchaseStatus.ORDERED));
    }

    @Test
    void transitionRuleRejectsReturningFromOrdered() {
        assertThrows(IllegalStateException.class,
                () -> new TransitionRule().check(PurchaseStatus.ORDERED, PurchaseStatus.DRAFT));
    }

    @Test
    void approvalGuardStopsDraftFromBeingOrdered() {
        assertThrows(IllegalStateException.class,
                () -> new UnapprovedCannotOrder().check(PurchaseStatus.DRAFT, PurchaseStatus.ORDERED));
    }
}
