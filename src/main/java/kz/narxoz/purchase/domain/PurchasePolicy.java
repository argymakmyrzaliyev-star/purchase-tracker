package kz.narxoz.purchase.domain;

import java.util.Objects;

/** Applies purchase status rules without depending on any framework. */
public final class PurchasePolicy {
    private final Rule rules;

    public PurchasePolicy(Rule rules) {
        this.rules = Objects.requireNonNull(rules, "Purchase rules must not be null");
    }

    public PurchaseStatus move(PurchaseStatus from, PurchaseStatus to) {
        Objects.requireNonNull(from, "Current purchase status must not be null");
        Objects.requireNonNull(to, "Target purchase status must not be null");
        rules.check(from, to);
        return to;
    }
}
