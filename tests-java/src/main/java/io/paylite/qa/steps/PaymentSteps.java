package io.paylite.qa.steps;

import io.paylite.qa.api.PaymentsClient;
import io.paylite.qa.data.PaymentData;
import io.paylite.qa.model.CapturePaymentRequest;
import io.paylite.qa.model.PaymentResponse;

import java.util.UUID;

/**
 * Preconditions for tests: payments brought into a known state.
 *
 * <p>Unlike {@link PaymentsClient}, every step asserts that its API calls
 * succeeded. A step is not the operation under test: if it fails, the
 * test's setup is broken, and the test should fail here, at the step,
 * with the unexpected status — not later, with a confusing error.
 *
 * <p>Use steps for what a test takes as given. Call {@link PaymentsClient}
 * directly for the operation the test checks, so that operation is visible
 * in the test body.
 */
public class PaymentSteps {

    private final PaymentsClient payments;

    public PaymentSteps(PaymentsClient payments) {
        this.payments = payments;
    }

    /**
     * A payment authorized with the default valid request.
     */
    public PaymentResponse authorized() {
        return payments.authorize(PaymentData.validAuthorization())
                .then().statusCode(201)
                .extract().as(PaymentResponse.class);
    }

    /**
     * An authorized payment, then captured for the given amount.
     */
    public PaymentResponse captured(long amount) {
        UUID paymentId = authorized().id();

        return payments.capture(paymentId, CapturePaymentRequest.of(amount))
                .then().statusCode(200)
                .extract().as(PaymentResponse.class);
    }

    /**
     * An authorized payment, then cancelled.
     */
    public PaymentResponse cancelled() {
        UUID paymentId = authorized().id();

        return payments.cancel(paymentId)
                .then().statusCode(200)
                .extract().as(PaymentResponse.class);
    }

    /**
     * The current state of the payment, read through the API.
     */
    public PaymentResponse fetch(UUID paymentId) {
        return payments.get(paymentId)
                .then().statusCode(200)
                .extract().as(PaymentResponse.class);
    }
}
