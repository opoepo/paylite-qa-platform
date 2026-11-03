package io.paylite.qa.tests.api;

import io.paylite.qa.api.PaymentsClient;
import io.paylite.qa.assertions.PaymentAssertions;
import io.paylite.qa.assertions.Violation;
import io.paylite.qa.model.PaymentResponse;
import io.paylite.qa.model.PaymentStatus;
import io.paylite.qa.model.ProblemResponse;
import io.paylite.qa.model.RefundPaymentRequest;
import io.paylite.qa.steps.PaymentSteps;
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
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.assertj.core.api.Assertions.assertThat;


@DisplayName("POST /payments/{id}/refund — refund")
class RefundPaymentTest {

    private final PaymentsClient payments = new PaymentsClient();

    private final PaymentSteps paymentSteps = new PaymentSteps(payments);

    private final PaymentAssertions paymentAssertions = new PaymentAssertions(paymentSteps);


    @Nested
    @DisplayName("Refund · accepted")
    class Accepted {

        @Test
        @DisplayName("[REF-01] Full refund from CAPTURED")
        void fullRefundCaptured() {
            PaymentResponse captured = paymentSteps.captured(370L);

            PaymentResponse refunded = payments.refund(captured.id(), RefundPaymentRequest.of(370L))
                    .then().statusCode(200)
                    .extract().as(PaymentResponse.class);

            assertSoftly(softly -> {
                softly.assertThat(refunded.status()).isEqualTo(PaymentStatus.REFUNDED);
                softly.assertThat(refunded.capturedAmount()).isEqualTo(captured.capturedAmount());
                softly.assertThat(refunded.authorizedAmount()).isEqualTo(captured.authorizedAmount());
                softly.assertThat(refunded.refundedAmount()).isEqualTo(370L);
                softly.assertThat(refunded.currency()).isEqualTo(captured.currency());
                softly.assertThat(refunded.orderReference()).isEqualTo(captured.orderReference());
                softly.assertThat(refunded.createdAt()).isEqualTo(captured.createdAt());
                softly.assertThat(refunded.authorizationExpiresAt()).isEqualTo(captured.authorizationExpiresAt());
                softly.assertThat(refunded.updatedAt()).isAfter(captured.updatedAt());
            });
        }

        @Test
        @DisplayName("[REF-02] Partially refund from CAPTURED")
        void partiallyRefundCaptured() {
            PaymentResponse captured = paymentSteps.captured(370L);

            PaymentResponse partiallyRefunded = payments.refund(captured.id(), RefundPaymentRequest.of(220L))
                    .then().statusCode(200)
                    .extract().as(PaymentResponse.class);

            assertSoftly(softly -> {
                softly.assertThat(partiallyRefunded.status()).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);
                softly.assertThat(partiallyRefunded.capturedAmount()).isEqualTo(captured.capturedAmount());
                softly.assertThat(partiallyRefunded.authorizedAmount()).isEqualTo(captured.authorizedAmount());
                softly.assertThat(partiallyRefunded.refundedAmount()).isEqualTo(220L);
                softly.assertThat(partiallyRefunded.currency()).isEqualTo(captured.currency());
                softly.assertThat(partiallyRefunded.orderReference()).isEqualTo(captured.orderReference());
                softly.assertThat(partiallyRefunded.createdAt()).isEqualTo(captured.createdAt());
                softly.assertThat(partiallyRefunded.authorizationExpiresAt()).isEqualTo(captured.authorizationExpiresAt());
                softly.assertThat(partiallyRefunded.updatedAt()).isAfter(captured.updatedAt());
            });
        }

        @Test
        @DisplayName("[REF-03] Refund is persisted")
        void refundIsPersisted() {
            PaymentResponse refunded = paymentSteps.refunded(370L, 270L);

            assertThat(paymentSteps.fetch(refunded.id())).isEqualTo(refunded);
        }

        @Test
        @DisplayName("[REF-04] Refund of 1 (lower bound)")
        void refundOfLowerBound() {
            PaymentResponse captured = paymentSteps.captured(370L);

            PaymentResponse partiallyRefunded = payments.refund(captured.id(), RefundPaymentRequest.of(1L))
                    .then().statusCode(200)
                    .extract().as(PaymentResponse.class);
            assertThat(partiallyRefunded.refundedAmount()).isEqualTo(1L);
            assertThat(partiallyRefunded.status()).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);
        }

        @Test
        @DisplayName("[REF-05] Refund of 369, one below captured (status boundary)")
        void refundOneBelowCaptured() {
            PaymentResponse captured = paymentSteps.captured(370L);

            PaymentResponse partiallyRefunded = payments.refund(captured.id(), RefundPaymentRequest.of(369L))
                    .then().statusCode(200)
                    .extract().as(PaymentResponse.class);
            assertThat(partiallyRefunded.refundedAmount()).isEqualTo(369L);
            assertThat(partiallyRefunded.status()).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);
        }

        @Test
        @DisplayName("[REF-06] Three partial refunds")
        void threePartialRefunds() {
            PaymentResponse captured = paymentSteps.captured(370L);

            PaymentResponse firstPartiallyRefunded = payments.refund(captured.id(), RefundPaymentRequest.of(100L))
                    .then().statusCode(200)
                    .extract().as(PaymentResponse.class);
            assertThat(firstPartiallyRefunded.refundedAmount()).isEqualTo(100L);
            assertThat(firstPartiallyRefunded.status()).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);

            PaymentResponse secondPartiallyRefunded = payments.refund(captured.id(), RefundPaymentRequest.of(120L))
                    .then().statusCode(200)
                    .extract().as(PaymentResponse.class);
            assertThat(secondPartiallyRefunded.refundedAmount()).isEqualTo(220L);
            assertThat(secondPartiallyRefunded.status()).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);

            PaymentResponse thirdPartiallyRefunded = payments.refund(captured.id(), RefundPaymentRequest.of(50L))
                    .then().statusCode(200)
                    .extract().as(PaymentResponse.class);
            assertThat(thirdPartiallyRefunded.refundedAmount()).isEqualTo(270L);
            assertThat(thirdPartiallyRefunded.status()).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);
        }

        @Test
        @DisplayName("[REF-07] Refund of the exact remainder")
        void refundExactRemainder() {
            PaymentResponse firstRefunded = paymentSteps.refunded(370L, 220L);

            PaymentResponse totallyRefunded = payments.refund(firstRefunded.id(), RefundPaymentRequest.of(150L))
                    .then().statusCode(200)
                    .extract().as(PaymentResponse.class);
            assertThat(totallyRefunded.refundedAmount()).isEqualTo(370L);
            assertThat(totallyRefunded.status()).isEqualTo(PaymentStatus.REFUNDED);
        }

    }

    @Nested
    @DisplayName("Refund · rejected by business rules")
    class RuleViolations {
        @Test
        @DisplayName("[REF-08] Refund of captured + 1 is rejected")
        void refundOfCapturedPlusOne() {
            PaymentResponse captured = paymentSteps.captured(370L);

            assertProblem(payments.refund(captured.id(), RefundPaymentRequest.of(captured.capturedAmount() + 1L)),
                    422, "refund_exceeds_captured");
            paymentAssertions.assertUnchanged(captured);
        }

        @Test
        @DisplayName("[REF-09] Refund out of boundary from PARTIALLY_REFUNDED is rejected")
        void refundOutOfBoundary() {
            PaymentResponse partiallyRefunded = paymentSteps.refunded(370L, 220L);

            assertProblem(payments.refund(partiallyRefunded.id(), RefundPaymentRequest.of(151L)),
                    422, "refund_exceeds_captured");
            paymentAssertions.assertUnchanged(partiallyRefunded);
        }

        @Test
        @DisplayName("[REF-10] Refund from AUTHORIZED is rejected")
        void refundFromAuthorized() {
            PaymentResponse authorized = paymentSteps.authorized();

            assertProblem(payments.refund(authorized.id(), RefundPaymentRequest.of(370L)),
                    409, "invalid_state");
            paymentAssertions.assertUnchanged(authorized);
        }

        @Test
        @DisplayName("[REF-11] Refund from CANCELLED is rejected")
        void refundFromCancelled() {
            PaymentResponse cancelled = paymentSteps.cancelled();

            assertProblem(payments.refund(cancelled.id(), RefundPaymentRequest.of(370L)),
                    409, "invalid_state");
            paymentAssertions.assertUnchanged(cancelled);
        }

        @Test
        @DisplayName("[REF-12] Refund from REFUNDED is rejected")
        void refundFromRefunded() {
            PaymentResponse totallyRefunded = paymentSteps.refunded(370L, 370L);

            assertProblem(payments.refund(totallyRefunded.id(), RefundPaymentRequest.of(10L)),
                    409, "invalid_state");
            paymentAssertions.assertUnchanged(totallyRefunded);
        }
    }

    @Nested
    @DisplayName("Refund · malformed and invalid requests")
    class RequestErrors {

        @Test
        @DisplayName("[REF-16] Refund unknown payment is rejected")
        void refundUnknownPayment() {
            assertProblem(payments.refund(UUID.randomUUID(), RefundPaymentRequest.of(370L)),
                    404, "payment_not_found");
        }

        @Test
        @DisplayName("[REF-17] Refund with not UUID id is rejected")
        void refundWithNotUUID() {
            assertProblem(payments.refundRaw("not-a-uuid", RefundPaymentRequest.of(370L)),
                    400, "malformed_request");
        }

        @Test
        @DisplayName("[REF-18] Refund Missing Idempotency-Key is rejected")
        void missingIdempotencyKey() {
            PaymentResponse captured = paymentSteps.captured(370L);

            assertProblem(payments.refundWithoutIdempotencyKey(captured.id(), RefundPaymentRequest.of(370L)),
                    400, "missing_idempotency_key");
            paymentAssertions.assertUnchanged(captured);
        }

        @Test
        @DisplayName("[REF-19] Field reason is rejected")
        void fieldReasonIsRejected() {
            PaymentResponse captured = paymentSteps.captured(370L);

            ProblemResponse problem = assertProblem(payments.refundRaw(captured.id().toString(),
                    Map.of("reason", "some reason", "amount", 370L)), 400, "malformed_request");
            assertThat(problem.detail()).contains("'reason'");
            paymentAssertions.assertUnchanged(captured);
        }

        @Test
        @DisplayName("[REF-20] Refund with no body")
        void refundWithNoBody() {
            PaymentResponse captured = paymentSteps.captured(370L);

            assertProblem(payments.refundWithoutBody(captured.id()), 400, "malformed_request");
            paymentAssertions.assertUnchanged(captured);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("invalidAmounts")
        @DisplayName("Refund with invalid amount is rejected")
        void refundWithInvalidAmount(String caseName, Map<String, Object> body, Violation expected) {
            PaymentResponse captured = paymentSteps.captured(370L);

            assertValidationFailed(payments.refundRaw(captured.id().toString(), body), expected);

            paymentAssertions.assertUnchanged(captured);
        }

        static Stream<Arguments> invalidAmounts() {
            return Stream.of(
                    Arguments.of("[REF-21] zero amount", amount(0), violation("amount", "invalid_amount")),
                    Arguments.of("[REF-21] negative amount", amount(-5), violation("amount", "invalid_amount")),
                    Arguments.of("[REF-21] fractional amount", amount(12.5), violation("amount", "invalid_amount")),
                    Arguments.of("[REF-21] missing amount", Map.of(), violation("amount", "required"))
            );
        }


    }

    private static Map<String, Object> amount(Object value) {
        return Map.of("amount", value);
    }

}
