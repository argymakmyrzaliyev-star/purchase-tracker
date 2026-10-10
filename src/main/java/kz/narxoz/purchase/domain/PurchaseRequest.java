package kz.narxoz.purchase.domain;

import java.util.Objects;

public record PurchaseRequest(PurchaseId id, PurchaseKey businessKey,
                              PurchaseStatus status, String title) {
    public PurchaseRequest {
        Objects.requireNonNull(id, "Purchase ID must not be null");
        Objects.requireNonNull(businessKey, "Purchase business key must not be null");
        Objects.requireNonNull(status, "Purchase status must not be null");
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Purchase title must not be null or blank");
        }
    }
}
