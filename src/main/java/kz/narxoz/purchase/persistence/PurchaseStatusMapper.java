package kz.narxoz.purchase.persistence;

import kz.narxoz.purchase.domain.PurchaseStatus;

/** Mapping belongs at the database boundary, not in the domain. */
final class PurchaseStatusMapper {
    private PurchaseStatusMapper() {
    }

    static String toDatabase(PurchaseStatus status) {
        return status.name();
    }

    static PurchaseStatus fromDatabase(String value) {
        try {
            return PurchaseStatus.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new IllegalStateException("Unknown purchase status in database: " + value, ex);
        }
    }
}
