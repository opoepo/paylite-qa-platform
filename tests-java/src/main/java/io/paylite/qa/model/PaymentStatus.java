package io.paylite.qa.model;

/** Payment statuses as defined in docs/api-contract.md. */
public enum PaymentStatus {
    AUTHORIZED, CANCELLED, EXPIRED, CAPTURED, PARTIALLY_REFUNDED, REFUNDED
}
