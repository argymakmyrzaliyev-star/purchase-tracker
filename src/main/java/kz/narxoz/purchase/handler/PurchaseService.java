package kz.narxoz.purchase.handler;

import kz.narxoz.purchase.domain.PurchasePolicy;
import kz.narxoz.purchase.domain.PurchaseStatus;
import kz.narxoz.purchase.domain.Rule;
import org.springframework.stereotype.Service;

/** Application service that delegates status decisions to the domain policy. */
@Service
public final class PurchaseService {
    private final PurchasePolicy policy;

    public PurchaseService(Rule rules) {
        this.policy = new PurchasePolicy(rules);
    }

    public PurchaseStatus move(PurchaseStatus from, PurchaseStatus to) {
        return policy.move(from, to);
    }
}
