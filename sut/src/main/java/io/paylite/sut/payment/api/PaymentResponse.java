package io.paylite.sut.payment.api;

import io.paylite.sut.payment.Currency;
import io.paylite.sut.payment.Payment;
import io.paylite.sut.payment.PaymentStatus;

import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        String orderReference,
        Currency currency,
        long authorizedAmount,
        long capturedAmount,
        long refundedAmount,
        PaymentStatus status,
        Instant authorizationExpiresAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrderReference(),
                payment.getCurrency(),
                payment.getAuthorizedAmount(),
                payment.getCapturedAmount(),
                payment.getRefundedAmount(),
                payment.getStatus(),
                payment.getAuthorizationExpiresAt(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
