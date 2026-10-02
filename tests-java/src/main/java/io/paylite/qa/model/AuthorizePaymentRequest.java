package io.paylite.qa.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Body of POST /api/v1/payments.
 * Deliberately loose types: tests must be able to send invalid input
 * (unknown currency, missing amount). A null field is omitted from the JSON.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthorizePaymentRequest(
        String orderReference,
        String currency,
        Long amount
) {
    public AuthorizePaymentRequest withOrderReference(String orderReference) {
        return new AuthorizePaymentRequest(orderReference, currency, amount);
    }

    public AuthorizePaymentRequest withCurrency(String currency) {
        return new AuthorizePaymentRequest(orderReference, currency, amount);
    }

    public AuthorizePaymentRequest withAmount(Long amount) {
        return new AuthorizePaymentRequest(orderReference, currency, amount);
    }
}
