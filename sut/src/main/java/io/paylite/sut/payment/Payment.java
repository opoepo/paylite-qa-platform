package io.paylite.sut.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_reference", nullable = false, updatable = false)
    private String orderReference;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", nullable = false, length = 3, updatable = false)
    private Currency currency;

    @Column(name = "authorized_amount", nullable = false, updatable = false)
    private long authorizedAmount;

    @Column(name = "captured_amount", nullable = false)
    private long capturedAmount;

    @Column(name = "refunded_amount", nullable = false)
    private long refundedAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PaymentStatus status;

    @Column(name = "authorization_expires_at", nullable = false, updatable = false)
    private Instant authorizationExpiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * The only way to create a payment: a new authorization (hold).
     * Time is passed in, not read from the system clock, so tests control it.
     */
    public static Payment authorize(String orderReference,
                                    Currency currency,
                                    long amount,
                                    Instant now,
                                    Instant expiresAt) {
        Payment payment = new Payment();
        payment.orderReference = orderReference;
        payment.currency = currency;
        payment.authorizedAmount = amount;
        payment.capturedAmount = 0;
        payment.refundedAmount = 0;
        payment.status = PaymentStatus.AUTHORIZED;
        payment.authorizationExpiresAt = expiresAt;
        payment.createdAt = now;
        payment.updatedAt = now;
        return payment;
    }

    /**
     * Lazy expiry (ADR-0010): a hold is expired from the moment
     * now >= authorizationExpiresAt. Returns true if the status changed.
     */
    public boolean expireIfDue(Instant now) {
        if (status == PaymentStatus.AUTHORIZED && !now.isBefore(authorizationExpiresAt)) {
            status = PaymentStatus.EXPIRED;
            updatedAt = now;
            return true;
        }
        return false;
    }

    /**
     * Capture the actual amount (contract rules 1, 3, 5; ADR-0009).
     * Checks run in a fixed order: expiry, then state, then amount.
     */
    public void capture(long amount, Instant now) {
        requireLiveAuthorization(now, "capture");
        if (amount > authorizedAmount) {
            throw new CaptureExceedsAuthorizedException(amount, authorizedAmount);
        }
        capturedAmount = amount;
        status = PaymentStatus.CAPTURED;
        updatedAt = now;
    }

    /** Release the hold without moving money. Checks: expiry, then state. */
    public void cancel(Instant now) {
        requireLiveAuthorization(now, "cancel");
        status = PaymentStatus.CANCELLED;
        updatedAt = now;
    }

    /**
     * The hold must not be expired and the payment must be AUTHORIZED.
     * Expiry is evaluated first and may itself change the status (ADR-0010).
     */
    private void requireLiveAuthorization(Instant now, String operation) {
        expireIfDue(now);
        if (status == PaymentStatus.EXPIRED) {
            throw new AuthorizationExpiredException(authorizationExpiresAt);
        }
        if (status != PaymentStatus.AUTHORIZED) {
            throw new InvalidStateException(status, operation);
        }
    }
}
