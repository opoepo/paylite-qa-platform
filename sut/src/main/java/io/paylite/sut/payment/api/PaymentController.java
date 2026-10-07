package io.paylite.sut.payment.api;

import io.paylite.sut.payment.Payment;
import io.paylite.sut.payment.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.paylite.sut.error.MissingIdempotencyKeyException;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    /**
     * Idempotency-Key is required (contract rule 7).
     * Replay semantics (same key -> same result) are implemented in a later step.
     */
    @PostMapping
    public ResponseEntity<PaymentResponse> authorize(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody AuthorizePaymentRequest request) {
        requireIdempotencyKey(idempotencyKey);
        Payment payment = service.authorize(request);
        URI location = URI.create("/api/v1/payments/" + payment.getId());
        return ResponseEntity.created(location).body(PaymentResponse.from(payment));
    }

    @GetMapping("/{id}")
    public PaymentResponse get(@PathVariable UUID id) {
        return PaymentResponse.from(service.get(id));
    }

    private static void requireIdempotencyKey(String key) {
        if (key.isBlank()) {
            throw new MissingIdempotencyKeyException();
        }
    }
}
