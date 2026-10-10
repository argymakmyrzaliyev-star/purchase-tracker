package kz.narxoz.purchase.domain;

/** Unchecked so that a failing purchase transaction is rolled back. */
public class DuplicatePurchase extends RuntimeException {
    public DuplicatePurchase(PurchaseKey businessKey, Throwable cause) {
        super("duplicate purchase: " + businessKey.value(), cause);
    }
}
