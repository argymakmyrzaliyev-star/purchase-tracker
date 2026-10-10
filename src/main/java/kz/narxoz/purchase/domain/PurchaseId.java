package kz.narxoz.purchase.domain;

import java.util.UUID;

/** Internal surrogate identity, separate from the human business key. */
public record PurchaseId(UUID value) {
    public PurchaseId {
        if (value == null) {
            throw new IllegalArgumentException("Purchase ID must not be null");
        }
    }

    public static PurchaseId newId() {
        return new PurchaseId(UUID.randomUUID());
    }

    public static PurchaseId parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Purchase ID must not be null or blank");
        }
        return new PurchaseId(UUID.fromString(value));
    }
}
