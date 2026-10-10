package kz.narxoz.purchase.domain;

/** Human-readable purchase number, for example PR-19. */
public record PurchaseKey(String value) {
    public PurchaseKey {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Purchase business key must not be null or blank");
        }
    }
}
