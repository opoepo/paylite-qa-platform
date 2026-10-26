package io.paylite.qa.tests.api;

import io.paylite.qa.api.PaymentsClient;
import io.paylite.qa.model.PaymentResponse;
import io.paylite.qa.model.PaymentStatus;
import io.paylite.qa.model.ProblemResponse;
import io.paylite.qa.steps.PaymentSteps;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static io.paylite.qa.assertions.ApiAssertions.assertProblem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@DisplayName("POST /payments/{id}/cancel — cancel")
class CancelPaymentTest {

    private final PaymentsClient payments = new PaymentsClient();

    private final PaymentSteps paymentSteps = new PaymentSteps(payments);

    @Nested
    @DisplayName("Cancel · accepted")
    class Accepted {

        @Test
        @DisplayName("[CNL-01] Cancel an authorized payment")
        void cancelPayment() {
            PaymentResponse authorized = paymentSteps.authorized();

            PaymentResponse cancelled = payments.cancel(authorized.id())
                    .then().statusCode(200)
                    .extract().body().as(PaymentResponse.class);

            assertSoftly(softly -> {
                softly.assertThat(cancelled.status()).isEqualTo(PaymentStatus.CANCELLED);
                softly.assertThat(cancelled.authorizedAmount()).isEqualTo(authorized.authorizedAmount());
                softly.assertThat(cancelled.capturedAmount()).isZero();
                softly.assertThat(cancelled.refundedAmount()).isZero();
                softly.assertThat(cancelled.currency()).isEqualTo(authorized.currency());
                softly.assertThat(cancelled.orderReference()).isEqualTo(authorized.orderReference());
                softly.assertThat(cancelled.createdAt()).isEqualTo(authorized.createdAt());
                softly.assertThat(cancelled.authorizationExpiresAt()).isEqualTo(authorized.authorizationExpiresAt());
                softly.assertThat(cancelled.updatedAt()).isAfter(authorized.updatedAt());
            });
        }

        @Test
        @DisplayName("[CNL-02] Cancel is persisted")
        void cancelIsPersisted() {
            PaymentResponse cancelled = paymentSteps.cancelled();

            assertThat(paymentSteps.fetch(cancelled.id())).isEqualTo(cancelled);
        }

        @Test
        @DisplayName("[CNL-15] Empty object {} as body")
        void cancelWithEmptyObjectAsBody() {
            PaymentResponse authorized = paymentSteps.authorized();

            PaymentResponse cancelled = payments.cancelWithBody(authorized.id(), Map.of())
                    .then().statusCode(200)
                    .extract().as(PaymentResponse.class);

            assertThat(cancelled.status()).isEqualTo(PaymentStatus.CANCELLED);
        }

    }

    @Nested
    @DisplayName("Cancel · rejected by business rules")
    class RuleViolations {

        @Test
        @DisplayName("[CNL-03] Cancel a captured payment")
        void cancelCapturedPayment() {
            PaymentResponse captured = paymentSteps.captured(370L);

            assertProblem(payments.cancel(captured.id()),
                    409, "invalid_state");
            assertUnchanged(captured);
        }

        @Test
        @DisplayName("[CNL-04] Second cancel")
        void secondCancelPayment() {
            PaymentResponse cancelled = paymentSteps.cancelled();

            assertProblem(payments.cancel(cancelled.id()),
                    409, "invalid_state");
            assertUnchanged(cancelled);
        }

    }

    @Nested
    @DisplayName("Cancel · malformed and invalid requests")
    class RequestErrors {

        @Test
        @DisplayName("[CNL-11] Unknown payment")
        void cancelUnknownPayment() {
            assertProblem(payments.cancel(UUID.randomUUID()),
                    404, "payment_not_found");
        }

        @Test
        @DisplayName("[CNL-12] Payment id is not a UUID")
        void cancelPaymentIdIsNotAUUID() {
            assertProblem(payments.cancelRaw("not-a-uuid"),
                    400, "malformed_request");
        }

        @Test
        @DisplayName("[CNL-13] No Idempotency-Key")
        void cancelNoIdempotencyKey() {
            PaymentResponse authorized = paymentSteps.authorized();

            assertProblem(payments.cancelWithoutIdempotencyKey(authorized.id()),
                    400, "missing_idempotency_key");
            assertUnchanged(authorized);
        }

        @Test
        @DisplayName("[CNL-14] Body with a field")
        void cancelWithField() {
            PaymentResponse authorized = paymentSteps.authorized();

            ProblemResponse problem = assertProblem(payments.cancelWithBody(authorized.id(),
                    Map.of("unknown_field", "unknown_value")), 400, "malformed_request");
            assertThat(problem.detail()).contains("'unknown_field'");
            assertUnchanged(authorized);
        }
    }

    /**
     * A rejected operation must leave no trace: every field, including updatedAt, is the same.
     */
    private void assertUnchanged(PaymentResponse expected) {
        assertThat(paymentSteps.fetch(expected.id()))
                .as("payment after the rejected operation")
                .isEqualTo(expected);
    }
}
