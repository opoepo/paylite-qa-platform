package io.paylite.sut.payment;

import io.paylite.sut.payment.api.AuthorizePaymentRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository repository;
    private final Clock clock;
    private final Duration authorizationTtl;

    public PaymentService(PaymentRepository repository,
                          Clock clock,
                          @Value("${paylite.authorization-ttl}") Duration authorizationTtl) {
        this.repository = repository;
        this.clock = clock;
        this.authorizationTtl = authorizationTtl;
    }

    @Transactional
    public Payment authorize(AuthorizePaymentRequest request) {
        Instant now = clock.instant();
        Payment payment = Payment.authorize(
                request.orderReference(),
                request.currency(),
                request.amount(),
                now,
                now.plus(authorizationTtl)
        );
        return repository.save(payment);
    }

    @Transactional
    public Payment get(UUID id) {
        Payment payment = repository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));
        payment.expireIfDue(clock.instant());
        return payment;
    }

    /** Rule violations do not roll back the expiry set by expireIfDue (ADR-0019). */
    @Transactional(noRollbackFor = PaymentRuleViolation.class)
    public Payment capture(UUID id, long amount) {
        Payment payment = load(id);
        payment.capture(amount, clock.instant());
        return payment;
    }

    /** Rule violations do not roll back the expiry set by expireIfDue (ADR-0019). */
    @Transactional(noRollbackFor = PaymentRuleViolation.class)
    public Payment cancel(UUID id) {
        Payment payment = load(id);
        payment.cancel(clock.instant());
        return payment;
    }

    /** Rule violations do not roll back the expiry set by expireIfDue (ADR-0019). */
    @Transactional(noRollbackFor = PaymentRuleViolation.class)
    public Payment refund(UUID id, long amount) {
        Payment payment = load(id);
        payment.refund(amount, clock.instant());
        return payment;
    }

    private Payment load(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));
    }
}
