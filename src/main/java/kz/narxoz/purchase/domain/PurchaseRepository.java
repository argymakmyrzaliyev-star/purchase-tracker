package kz.narxoz.purchase.domain;

import java.util.Optional;

/** Outbound persistence port. It exposes domain values only. */
public interface PurchaseRepository {
    void insert(PurchaseRequest purchase);

    long count(PurchaseKey businessKey);

    Optional<PurchaseRequest> find(PurchaseKey businessKey);
}
