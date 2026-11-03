package io.paylite.qa.tests.api;

import io.paylite.qa.api.PaymentsClient;
import io.paylite.qa.assertions.Violation;
import io.paylite.qa.model.CapturePaymentRequest;
import io.paylite.qa.model.PaymentResponse;
import io.paylite.qa.model.PaymentStatus;
import io.paylite.qa.steps.PaymentSteps;
import io.paylite.qa.assertions.PaymentAssertions;
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

    private final PaymentSteps paymentSteps = new PaymentSteps(payments);

    private final PaymentAssertions paymentAssertions = new PaymentAssertions(paymentSteps);

    @Nested
    @DisplayName("Capture · accepted")
    class Accepted {

        @Test
        @DisplayName("[CAP-01] Partial capture changes only the capture fields")
        void partialCapture() {
            PaymentResponse authorized = paymentSteps.authorized();

            PaymentResponse captured = payments.capture(authorized.id(), CapturePaymentRequest.of(370L))
                    .then().statusCode(200)
                    .extract().as(PaymentResponse.class);

            assertSoftly(softly -> {
                softly.assertThat(captured.status()).isEqualTo(PaymentStatus.CAPTURED);
                softly.assertThat(captured.capturedAmount()).isEqualTo(370L);
                softly.assertThat(captured.authorizedAmount()).isEqualTo(authorized.authorizedAmount());
                softly.assertThat(captured.refundedAmount()).isZero();
                softly.assertThat(captured.currency()).isEqualTo(authorized.currency());
                softly.assertThat(captured.orderReference()).isEqualTo(authorized.orderReference());
                softly.assertThat(captured.createdAt()).isEqualTo(authorized.createdAt());
                softly.assertThat(captured.authorizationExpiresAt()).isEqualTo(authorized.authorizationExpiresAt());
                softly.assertThat(captured.updatedAt()).isAfter(authorized.updatedAt());
            });
        }

        @Test
        @DisplayName("[CAP-02] Capture is persisted")
        void captureIsPersisted() {
            PaymentResponse captured = paymentSteps.captured(370L);

            assertThat(paymentSteps.fetch(captured.id())).isEqualTo(captured);
        }

        @Test
        @DisplayName("[CAP-03] Capture of 1 is accepted (lower bound)")
        void captureOfOne() {
            PaymentResponse authorized = paymentSteps.authorized();

            PaymentResponse captured = payments.capture(authorized.id(), CapturePaymentRequest.of(1L))
                    .then().statusCode(200)
                    .extract().as(PaymentResponse.class);
            assertThat(captured.capturedAmount()).isEqualTo(1L);
        }

        @Test
        @DisplayName("[CAP-04] Capture of the full authorized amount is accepted (upper bound)")
        void captureOfFullAmount() {
            PaymentResponse authorized = paymentSteps.authorized();

            PaymentResponse captured = payments.capture(authorized.id(), CapturePaymentRequest.of(authorized.authorizedAmount()))
                    .then().statusCode(200)
                    .extract().as(PaymentResponse.class);

            assertThat(captured.capturedAmount()).isEqualTo(authorized.authorizedAmount());
        }
    }

    @Nested
    @DisplayName("Capture · rejected by business rules")
    class RuleViolations {

        @Test
        @DisplayName("[CAP-05] Second capture is rejected and changes nothing")
        void secondCapture() {
            PaymentResponse captured = paymentSteps.captured(370L);

            assertProblem(payments.capture(captured.id(), CapturePaymentRequest.of(370L)),
                    409, "invalid_state");

            paymentAssertions.assertUnchanged(captured);
        }

        @Test
        @DisplayName("[CAP-06] Capture above the authorized amount is rejected and changes nothing")
        void captureAboveAuthorized() {
            PaymentResponse authorized = paymentSteps.authorized();

            assertProblem(payments.capture(authorized.id(), CapturePaymentRequest.of(authorized.authorizedAmount() + 1)),
                    422, "capture_exceeds_authorized");

            paymentAssertions.assertUnchanged(authorized);
        }

        @Test
        @DisplayName("[CAP-07] Capture of a cancelled payment")
        void captureOfACancelledPayment() {
            PaymentResponse cancelled = paymentSteps.cancelled();

            assertProblem(payments.capture(cancelled.id(), CapturePaymentRequest.of(370L)),
                    409, "invalid_state");

            paymentAssertions.assertUnchanged(cancelled);
        }

        @Test
        @DisplayName("[CAP-16] Capture of a partially refunded payment")
        void captureFromPartiallyRefunded() {
            PaymentResponse partiallyRefunded = paymentSteps.refunded(370L, 220L);

            assertProblem(payments.capture(partiallyRefunded.id(), CapturePaymentRequest.of(370L)),
                    409, "invalid_state");

            paymentAssertions.assertUnchanged(partiallyRefunded);
        }

        @Test
        @DisplayName("[CAP-17] Capture of a fully refunded payment")
        void captureFromFullyRefunded() {
            PaymentResponse totallyRefunded = paymentSteps.refunded(370L, 370L);

            assertProblem(payments.capture(totallyRefunded.id(), CapturePaymentRequest.of(370L)),
                    409, "invalid_state");

            paymentAssertions.assertUnchanged(totallyRefunded);
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
            PaymentResponse authorized = paymentSteps.authorized();

            assertProblem(payments.captureWithoutIdempotencyKey(authorized.id(), CapturePaymentRequest.of(370L)),
                    400, "missing_idempotency_key");

            paymentAssertions.assertUnchanged(authorized);
        }

        @Test
        @DisplayName("[CAP-14] Unknown field in the body is rejected")
        void unknownField() {
            PaymentResponse authorized = paymentSteps.authorized();

            assertProblem(payments.captureRaw(authorized.id().toString(),
                            Map.of("amount", 370, "status", "CAPTURED")),
                    400, "malformed_request");

            paymentAssertions.assertUnchanged(authorized);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("invalidAmounts")
        @DisplayName("[CAP-15] Invalid amount is rejected")
        void invalidAmount(String caseName, Map<String, Object> body, Violation expected) {
            PaymentResponse authorized = paymentSteps.authorized();

            assertValidationFailed(payments.captureRaw(authorized.id().toString(), body), expected);

            paymentAssertions.assertUnchanged(authorized);
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

    private static Map<String, Object> amount(Object value) {
        return Map.of("amount", value);
    }
}
