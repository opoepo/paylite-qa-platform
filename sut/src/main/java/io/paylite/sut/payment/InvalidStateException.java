package io.paylite.sut.payment;

public class InvalidStateException extends PaymentRuleViolation {

    public InvalidStateException(PaymentStatus status, String operation) {
        super(409, "invalid_state", "Operation not allowed in the current state",
                "Cannot " + operation + " a payment in status " + status);
    }
}
