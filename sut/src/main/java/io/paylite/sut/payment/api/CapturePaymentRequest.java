package io.paylite.sut.payment.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CapturePaymentRequest(
        @NotNull @Positive Long amount
) {
}
