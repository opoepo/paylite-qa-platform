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

    /**
     * An expired hold is persisted as EXPIRED even though the capture fails:
     * without noRollbackFor the exception would roll back the status change
     * made by expireIfDue (ADR-0019).
     */
    @Transactional(noRollbackFor = AuthorizationExpiredException.class)
    public Payment capture(UUID id, long amount) {
        Payment payment = repository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));
        payment.capture(amount, clock.instant());
        return payment;
    }

    /** Same rollback rule as capture: an expired hold is persisted as EXPIRED (ADR-0019). */
    @Transactional(noRollbackFor = AuthorizationExpiredException.class)
    public Payment cancel(UUID id) {
        Payment payment = repository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));
        payment.cancel(clock.instant());
        return payment;
    }
}
