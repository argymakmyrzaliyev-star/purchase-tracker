package kz.narxoz.purchase.handler;

import kz.narxoz.purchase.domain.PurchaseId;
import kz.narxoz.purchase.domain.PurchaseKey;
import kz.narxoz.purchase.domain.PurchasePolicy;
import kz.narxoz.purchase.domain.PurchaseRepository;
import kz.narxoz.purchase.domain.PurchaseRequest;
import kz.narxoz.purchase.domain.PurchaseStatus;
import kz.narxoz.purchase.domain.Rule;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** Spring manages transaction boundaries; the domain decides status rules. */
@Service
public class PurchaseService {
    private final PurchasePolicy policy;
    private final PurchaseRepository purchases;

    public PurchaseService(Rule rules, PurchaseRepository purchases) {
        this.policy = new PurchasePolicy(rules);
        this.purchases = purchases;
    }

    public PurchaseStatus move(PurchaseStatus from, PurchaseStatus to) {
        return policy.move(from, to);
    }

    @Transactional
    public PurchaseRequest register(PurchaseId id, String businessKey, String title) {
        PurchaseRequest purchase = new PurchaseRequest(id, new PurchaseKey(businessKey),
                PurchaseStatus.DRAFT, title);
        purchases.insert(purchase);
        return purchase;
    }

    @Transactional
    public void insertTwice(String businessKey) {
        PurchaseKey key = new PurchaseKey(businessKey);
        purchases.insert(new PurchaseRequest(PurchaseId.newId(), key,
                PurchaseStatus.DRAFT, "Laptop for the design team"));
        purchases.insert(new PurchaseRequest(PurchaseId.newId(), key,
                PurchaseStatus.DRAFT, "Duplicate laptop request"));
        // DuplicatePurchase escapes this method; Spring rolls back both inserts.
    }

    @Transactional(readOnly = true)
    public long count(String businessKey) {
        return purchases.count(new PurchaseKey(businessKey));
    }

    @Transactional(readOnly = true)
    public Optional<PurchaseRequest> find(String businessKey) {
        return purchases.find(new PurchaseKey(businessKey));
    }
}
