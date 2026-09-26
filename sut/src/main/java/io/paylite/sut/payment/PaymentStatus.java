package io.paylite.sut.payment;

/**
 * Payment lifecycle. Allowed transitions are defined in docs/api-contract.md.
 */
public enum PaymentStatus {
    AUTHORIZED,
    CANCELLED,
    EXPIRED,
    CAPTURED,
    PARTIALLY_REFUNDED,
    REFUNDED;

    public boolean isTerminal() {
        return this == CANCELLED || this == EXPIRED || this == REFUNDED;
    }
}
