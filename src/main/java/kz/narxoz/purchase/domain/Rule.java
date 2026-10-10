package kz.narxoz.purchase.domain;

/** A plain-Java rule for a purchase status change. */
@FunctionalInterface
public interface Rule {
    void check(PurchaseStatus from, PurchaseStatus to);
}
