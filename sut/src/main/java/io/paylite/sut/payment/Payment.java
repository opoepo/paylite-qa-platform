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
}
