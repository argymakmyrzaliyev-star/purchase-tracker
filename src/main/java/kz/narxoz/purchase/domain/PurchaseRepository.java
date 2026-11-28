package kz.narxoz.purchase.domain;

import java.util.Optional;

/** Outbound persistence port. It exposes domain values only. */
public interface PurchaseRepository {
    void insert(PurchaseRequest purchase);

    boolean updateStatus(PurchaseId id, PurchaseStatus expected, PurchaseStatus next);

    long count(PurchaseKey businessKey);

    Optional<PurchaseRequest> find(PurchaseKey businessKey);

    Optional<PurchaseRequest> findById(PurchaseId id);
}
