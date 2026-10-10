package kz.narxoz.purchase.domain;

/** Prevents ordering before manager approval. */
public final class UnapprovedCannotOrder implements Rule {
    @Override
    public void check(PurchaseStatus from, PurchaseStatus to) {
        if (from == PurchaseStatus.DRAFT && to == PurchaseStatus.ORDERED) {
            throw new IllegalStateException("A purchase must be approved before it can be ordered");
        }
    }
}
