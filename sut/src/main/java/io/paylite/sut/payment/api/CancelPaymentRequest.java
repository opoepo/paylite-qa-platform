package io.paylite.sut.payment.api;

/**
 * Body of POST /payments/{id}/cancel. Has no fields: no body or an empty
 * object is accepted, any field is rejected as unknown (ADR-0014).
 */
public record CancelPaymentRequest() {
}
