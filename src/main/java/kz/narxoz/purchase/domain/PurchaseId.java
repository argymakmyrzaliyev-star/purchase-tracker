package kz.narxoz.purchase.domain;

/** Identifies a purchase request. */
public record PurchaseId(String value) {
    public PurchaseId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Purchase ID must not be null or blank");
        }
    }
}
