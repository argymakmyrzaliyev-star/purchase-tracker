package kz.narxoz.purchase.domain;

public class InvalidTransition extends IllegalStateException {
    public InvalidTransition(String message) {
        super(message);
    }
}
