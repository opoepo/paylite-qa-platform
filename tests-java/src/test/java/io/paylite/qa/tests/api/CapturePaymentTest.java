package io.paylite.qa.tests.api;

import io.paylite.qa.api.PaymentsClient;
import io.paylite.qa.assertions.Violation;
import io.paylite.qa.data.PaymentData;
import io.paylite.qa.model.CapturePaymentRequest;
import io.paylite.qa.model.PaymentResponse;
import io.paylite.qa.model.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static io.paylite.qa.assertions.ApiAssertions.assertProblem;
import static io.paylite.qa.assertions.ApiAssertions.assertValidationFailed;
import static io.paylite.qa.assertions.Violation.violation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@DisplayName("POST /payments/{id}/capture — capture")
class CapturePaymentTest {

    private final PaymentsClient payments = new PaymentsClient();

    @Nested
    @DisplayName("Capture · accepted")
    class Accepted {

        @Test
        @DisplayName("[CAP-01] Partial capture changes only the capture fields")
        void partialCapture() {
            PaymentResponse before = authorizedPayment();

            PaymentResponse after = captureExpectingOk(before.id(), 370L);

            assertSoftly(softly -> {
                softly.assertThat(after.status()).isEqualTo(PaymentStatus.CAPTURED);
                softly.assertThat(after.capturedAmount()).isEqualTo(370L);
                softly.assertThat(after.authorizedAmount()).isEqualTo(before.authorizedAmount());
                softly.assertThat(after.refundedAmount()).isZero();
                softly.assertThat(after.currency()).isEqualTo(before.currency());
                softly.assertThat(after.orderReference()).isEqualTo(before.orderReference());
                softly.assertThat(after.createdAt()).isEqualTo(before.createdAt());
                softly.assertThat(after.authorizationExpiresAt()).isEqualTo(before.authorizationExpiresAt());
                softly.assertThat(after.updatedAt()).isAfter(before.updatedAt());
            });
        }

        @Test
        @DisplayName("[CAP-02] Capture is persisted")
        void captureIsPersisted() {
            PaymentResponse captured = captureExpectingOk(authorizedPayment().id(), 370L);

            assertThat(fetch(captured.id())).isEqualTo(captured);
        }

        @Test
        @DisplayName("[CAP-03] Capture of 1 is accepted (lower bound)")
        void captureOfOne() {
            PaymentResponse captured = captureExpectingOk(authorizedPayment().id(), 1L);

            assertThat(captured.capturedAmount()).isEqualTo(1L);
        }

        @Test
        @DisplayName("[CAP-04] Capture of the full authorized amount is accepted (upper bound)")
        void captureOfFullAmount() {
            PaymentResponse before = authorizedPayment();

            PaymentResponse captured = captureExpectingOk(before.id(), before.authorizedAmount());

            assertThat(captured.capturedAmount()).isEqualTo(before.authorizedAmount());
        }
    }

    @Nested
    @DisplayName("Capture · rejected by business rules")
    class RuleViolations {

        @Test
        @DisplayName("[CAP-05] Second capture is rejected and changes nothing")
        void secondCapture() {
            PaymentResponse captured = captureExpectingOk(authorizedPayment().id(), 370L);

            assertProblem(payments.capture(captured.id(), CapturePaymentRequest.of(370L)),
                    409, "invalid_state");

            assertUnchanged(captured);
        }

        @Test
        @DisplayName("[CAP-06] Capture above the authorized amount is rejected and changes nothing")
        void captureAboveAuthorized() {
            PaymentResponse before = authorizedPayment();

            assertProblem(payments.capture(before.id(), CapturePaymentRequest.of(before.authorizedAmount() + 1)),
                    422, "capture_exceeds_authorized");

            assertUnchanged(before);
        }
    }

    @Nested
    @DisplayName("Capture · malformed and invalid requests")
    class RequestErrors {

        @Test
        @DisplayName("[CAP-11] Unknown payment returns 404")
        void unknownPayment() {
            assertProblem(payments.capture(UUID.randomUUID(), CapturePaymentRequest.of(370L)),
                    404, "payment_not_found");
        }

        @Test
        @DisplayName("[CAP-12] Payment id that is not a UUID is rejected")
        void malformedPaymentId() {
            assertProblem(payments.captureRaw("not-a-uuid", amount(370)),
                    400, "malformed_request");
        }

        @Test
        @DisplayName("[CAP-13] Missing Idempotency-Key is rejected")
        void missingIdempotencyKey() {
            PaymentResponse before = authorizedPayment();

            assertProblem(payments.captureWithoutIdempotencyKey(before.id(), CapturePaymentRequest.of(370L)),
                    400, "missing_idempotency_key");

            assertUnchanged(before);
        }

        @Test
        @DisplayName("[CAP-14] Unknown field in the body is rejected")
        void unknownField() {
            PaymentResponse before = authorizedPayment();

            assertProblem(payments.captureRaw(before.id().toString(),
                            Map.of("amount", 370, "status", "CAPTURED")),
                    400, "malformed_request");

            assertUnchanged(before);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("invalidAmounts")
        @DisplayName("Invalid amount is rejected")
        void invalidAmount(String caseName, Map<String, Object> body, Violation expected) {
            PaymentResponse before = authorizedPayment();

            assertValidationFailed(payments.captureRaw(before.id().toString(), body), expected);

            assertUnchanged(before);
        }

        static Stream<Arguments> invalidAmounts() {
            return Stream.of(
                    Arguments.of("[CAP-15] zero amount", amount(0), violation("amount", "invalid_amount")),
                    Arguments.of("[CAP-15] negative amount", amount(-5), violation("amount", "invalid_amount")),
                    Arguments.of("[CAP-15] fractional amount", amount(12.5), violation("amount", "invalid_amount")),
                    Arguments.of("[CAP-15] missing amount", Map.of(), violation("amount", "required"))
            );
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private PaymentResponse authorizedPayment() {
        return payments.authorize(PaymentData.validAuthorization())
                .then().statusCode(201)
                .extract().as(PaymentResponse.class);
    }

    private PaymentResponse captureExpectingOk(UUID paymentId, long amount) {
        return payments.capture(paymentId, CapturePaymentRequest.of(amount))
                .then().statusCode(200)
                .extract().as(PaymentResponse.class);
    }

    private PaymentResponse fetch(UUID paymentId) {
        return payments.get(paymentId)
                .then().statusCode(200)
                .extract().as(PaymentResponse.class);
    }

    /** A rejected operation must leave no trace: every field, including updatedAt, is the same. */
    private void assertUnchanged(PaymentResponse expected) {
        assertThat(fetch(expected.id()))
                .as("payment after the rejected operation")
                .isEqualTo(expected);
    }

    private static Map<String, Object> amount(Object value) {
        return Map.of("amount", value);
    }
}
