package kz.narxoz.purchase.domain;

public class MissingPurchase extends RuntimeException {
    public MissingPurchase(PurchaseId id) {
        super("purchase request not found: " + id.value());
    }
}
