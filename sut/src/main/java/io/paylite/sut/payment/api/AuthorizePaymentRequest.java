package io.paylite.sut.payment.api;

import io.paylite.sut.payment.Currency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AuthorizePaymentRequest(
        @NotBlank @Size(max = 255) String orderReference,
        @NotNull Currency currency,
        @NotNull @Positive Long amount
) {
}
