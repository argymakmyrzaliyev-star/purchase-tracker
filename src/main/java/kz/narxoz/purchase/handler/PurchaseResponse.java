package kz.narxoz.purchase.handler;

import kz.narxoz.purchase.domain.PurchaseRequest;
import kz.narxoz.purchase.domain.PurchaseStatus;

import java.util.UUID;

public record PurchaseResponse(UUID id, String businessKey, PurchaseStatus status, String title) {
    static PurchaseResponse from(PurchaseRequest purchase) {
        return new PurchaseResponse(purchase.id().value(), purchase.businessKey().value(),
                purchase.status(), purchase.title());
    }
}
