package io.paylite.qa.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Payment as returned by the API.
 * Strict types for contract fields (enums fail on unknown values);
 * boxed numbers so a missing field becomes null instead of a silent 0.
 */
public record PaymentResponse(
        UUID id,
        String orderReference,
        Currency currency,
        Long authorizedAmount,
        Long capturedAmount,
        Long refundedAmount,
        PaymentStatus status,
        Instant authorizationExpiresAt,
        Instant createdAt,
        Instant updatedAt
) {
}
