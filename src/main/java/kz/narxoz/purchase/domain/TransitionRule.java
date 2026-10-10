package kz.narxoz.purchase.domain;

/** Enforces the purchase status table. */
public final class TransitionRule implements Rule {
    @Override
    public void check(PurchaseStatus from, PurchaseStatus to) {
        boolean allowed = switch (from) {
            case DRAFT -> to == PurchaseStatus.APPROVED;
            case APPROVED -> to == PurchaseStatus.ORDERED;
            case ORDERED -> false;
        };

        if (!allowed) {
            throw new IllegalStateException("Cannot move purchase from " + from + " to " + to);
        }
    }
}
