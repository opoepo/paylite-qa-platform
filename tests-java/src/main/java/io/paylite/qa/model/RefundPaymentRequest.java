package io.paylite.qa.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Body of POST /api/v1/payments/{id}/refund. A null amount is omitted.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RefundPaymentRequest(Long amount) {

    public static RefundPaymentRequest of(long amount) {
        return new RefundPaymentRequest(amount);
    }
}
