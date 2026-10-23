package io.paylite.qa.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Body of POST /api/v1/payments/{id}/capture. A null amount is omitted. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CapturePaymentRequest(Long amount) {

    public static CapturePaymentRequest of(long amount) {
        return new CapturePaymentRequest(amount);
    }
}
