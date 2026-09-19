package kz.narxoz.purchase;

import java.util.Objects;

/** Enforces approval before ordering and prevents reopening an order. */
public final class PurchasePolicy {
    private PurchasePolicy() {
    }

    public static PurchaseStatus move(PurchaseStatus from, PurchaseStatus to) {
        Objects.requireNonNull(from, "Current purchase status must not be null");
        Objects.requireNonNull(to, "Target purchase status must not be null");

        boolean allowed = switch (from) {
            case DRAFT -> to == PurchaseStatus.APPROVED;
            case APPROVED -> to == PurchaseStatus.ORDERED;
            case ORDERED -> false;
        };

        if (!allowed) {
            throw new IllegalStateException("Cannot move purchase from " + from + " to " + to);
        }
        return to;
    }
}
