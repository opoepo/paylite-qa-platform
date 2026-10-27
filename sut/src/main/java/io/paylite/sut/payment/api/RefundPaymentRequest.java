package io.paylite.sut.payment.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RefundPaymentRequest(
        @NotNull @Positive Long amount
) {
}
