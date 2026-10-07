package io.paylite.qa.tests.api;

import io.paylite.qa.api.PaymentsClient;
import io.paylite.qa.assertions.Violation;
import io.paylite.qa.data.PaymentData;
import io.paylite.qa.model.AuthorizePaymentRequest;
import io.paylite.qa.model.Currency;
import io.paylite.qa.model.FieldViolation;
import io.paylite.qa.model.PaymentResponse;
import io.paylite.qa.model.PaymentStatus;
import io.paylite.qa.model.ProblemResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigInteger;
import java.util.Map;
import java.util.stream.Stream;

import static io.paylite.qa.assertions.ApiAssertions.assertProblem;
import static io.paylite.qa.assertions.ApiAssertions.assertValidationFailed;
import static io.paylite.qa.assertions.Violation.violation;
import static io.paylite.qa.data.PaymentData.validAuthorizationWith;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@DisplayName("POST /payments — authorize")
class AuthorizePaymentTest {

    private final PaymentsClient payments = new PaymentsClient();

    @Nested
    @DisplayName("Accepted requests")
    class Accepted {

        @Test
        @DisplayName("[AUTH-01] Authorize a payment")
        void authorizesPayment() {
            AuthorizePaymentRequest request = PaymentData.validAuthorization();

            PaymentResponse payment = authorizeExpectingCreated(request);

            assertSoftly(softly -> {
                softly.assertThat(payment.id()).isNotNull();
                softly.assertThat(payment.orderReference()).isEqualTo(request.orderReference());
                softly.assertThat(payment.currency()).isEqualTo(Currency.valueOf(request.currency()));
                softly.assertThat(payment.authorizedAmount()).isEqualTo(request.amount());
                softly.assertThat(payment.capturedAmount()).isZero();
                softly.assertThat(payment.refundedAmount()).isZero();
                softly.assertThat(payment.status()).isEqualTo(PaymentStatus.AUTHORIZED);
            });
        }

        @Test
        @DisplayName("[AUTH-02] Location header points to the created payment")
        void locationHeaderPointsToCreatedPayment() {
            Response response = payments.authorize(PaymentData.validAuthorization());

            response.then().statusCode(201);
            PaymentResponse payment = response.as(PaymentResponse.class);

            assertThat(response.header("Location"))
                    .isEqualTo("/api/v1/payments/" + payment.id());
        }

        @Test
        @DisplayName("[AUTH-05] Zero-exponent currency amount is stored as-is")
        void acceptsZeroExponentCurrency() {
            AuthorizePaymentRequest request = PaymentData.validAuthorization()
                    .withCurrency("VND")
                    .withAmount(50_000L);

            PaymentResponse payment = authorizeExpectingCreated(request);

            assertThat(payment.currency()).isEqualTo(Currency.VND);
            assertThat(payment.authorizedAmount()).isEqualTo(50_000L);
        }

        @Test
        @DisplayName("[AUTH-06] Minimum amount 1 is accepted")
        void acceptsMinimumAmount() {
            PaymentResponse payment = authorizeExpectingCreated(
                    PaymentData.validAuthorization().withAmount(1L));

            assertThat(payment.authorizedAmount()).isEqualTo(1L);
        }

        @Test
        @DisplayName("[AUTH-19] Order reference of exactly 255 characters is accepted")
        void acceptsOrderReferenceAtLimit() {
            String reference = PaymentData.uniqueOrderReferenceOfLength(255);

            PaymentResponse payment = authorizeExpectingCreated(
                    PaymentData.validAuthorization().withOrderReference(reference));

            assertThat(payment.orderReference()).isEqualTo(reference);
        }

        private PaymentResponse authorizeExpectingCreated(AuthorizePaymentRequest request) {
            return payments.authorize(request)
                    .then().statusCode(201)
                    .extract().as(PaymentResponse.class);
        }
    }

    @Nested
    @DisplayName("Field validation → 422 validation_failed")
    class FieldValidation {

        @Test
        @DisplayName("[AUTH-10] Empty object reports every missing field, sorted by name")
        void emptyObjectReportsAllMissingFields() {
            assertValidationFailed(payments.authorizeRaw(Map.of()),
                    violation("amount", "required"),
                    violation("currency", "required"),
                    violation("orderReference", "required"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("invalidValues")
        void rejectsInvalidValue(String caseName, AuthorizePaymentRequest request, Violation expected) {
            assertValidationFailed(payments.authorize(request), expected);
        }

        static Stream<Arguments> invalidValues() {
            AuthorizePaymentRequest valid = PaymentData.validAuthorization();
            return Stream.of(
                    Arguments.of("[AUTH-12] negative amount",
                            valid.withAmount(-5L), violation("amount", "invalid_amount")),
                    Arguments.of("[AUTH-13] zero amount",
                            valid.withAmount(0L), violation("amount", "invalid_amount")),
                    Arguments.of("[AUTH-15] unsupported currency GBP",
                            valid.withCurrency("GBP"), violation("currency", "unsupported_currency")),
                    Arguments.of("[AUTH-16] lower-case currency",
                            valid.withCurrency("usd"), violation("currency", "unsupported_currency")),
                    Arguments.of("[AUTH-17] blank order reference",
                            valid.withOrderReference("   "), violation("orderReference", "required")),
                    Arguments.of("[AUTH-18] order reference of 256 characters",
                            valid.withOrderReference("Q".repeat(256)), violation("orderReference", "too_long")),
                    Arguments.of("[AUTH-25] missing amount",
                            valid.withAmount(null), violation("amount", "required"))
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("wrongJsonTypes")
        void rejectsWrongJsonType(String caseName, Map<String, Object> body, Violation expected) {
            assertValidationFailed(payments.authorizeRaw(body), expected);
        }

        static Stream<Arguments> wrongJsonTypes() {
            return Stream.of(
                    Arguments.of("[AUTH-11] fractional amount",
                            validAuthorizationWith("amount", 12.5), violation("amount", "invalid_amount")),
                    Arguments.of("[AUTH-14] non-numeric amount",
                            validAuthorizationWith("amount", "abc"), violation("amount", "invalid_amount")),
                    Arguments.of("[AUTH-21] amount as numeric string",
                            validAuthorizationWith("amount", "100"), violation("amount", "invalid_amount")),
                    Arguments.of("[AUTH-22] amount beyond 64-bit range",
                            validAuthorizationWith("amount", new BigInteger("9223372036854775808")),
                            violation("amount", "invalid_amount")),
                    Arguments.of("[AUTH-23] currency as number",
                            validAuthorizationWith("currency", 0), violation("currency", "unsupported_currency")),
                    Arguments.of("[AUTH-24] order reference as number",
                            validAuthorizationWith("orderReference", 123), violation("orderReference", "invalid_value"))
            );
        }

        @Test
        @DisplayName("[AUTH-20] Messages are English regardless of Accept-Language")
        void messagesIndependentOfClientLocale() {
            PaymentsClient russianClient = new PaymentsClient(Map.of("Accept-Language", "ru"));

            ProblemResponse problem = assertValidationFailed(russianClient.authorizeRaw(Map.of()),
                    violation("amount", "required"),
                    violation("currency", "required"),
                    violation("orderReference", "required"));

            assertThat(problem.errors())
                    .extracting(FieldViolation::message)
                    .containsOnly("is required");
        }
    }

    @Nested
    @DisplayName("Malformed requests → 400")
    class Malformed {

        @Test
        @DisplayName("[AUTH-30] Unknown field is rejected and named in detail")
        void rejectsUnknownField() {
            ProblemResponse problem = assertProblem(
                    payments.authorizeRaw(validAuthorizationWith("status", "CAPTURED")),
                    400, "malformed_request");

            assertThat(problem.detail()).contains("'status'");
        }

        @Test
        @DisplayName("[AUTH-31] Truncated JSON is rejected")
        void rejectsTruncatedJson() {
            assertProblem(payments.authorizeRaw("{\"orderReference\":"), 400, "malformed_request");
        }

        @Test
        @DisplayName("[AUTH-32] Empty body is rejected")
        void rejectsEmptyBody() {
            assertProblem(payments.authorizeRaw(""), 400, "malformed_request");
        }

        @Test
        @DisplayName("[AUTH-33] Missing Idempotency-Key is rejected")
        void rejectsMissingIdempotencyKey() {
            assertProblem(payments.authorizeWithoutIdempotencyKey(PaymentData.validAuthorization()),
                    400, "missing_idempotency_key");
        }

        @Test
        @DisplayName("[AUTH-34] Empty Idempotency-Key is rejected")
        void rejectsEmptyIdempotencyKey() {
            assertProblem(payments.authorize(PaymentData.validAuthorization(), ""),
                    400, "missing_idempotency_key");
        }

        @Test
        @DisplayName("[AUTH-36] Whitespace-only Idempotency-Key is rejected")
        void rejectsBlankIdempotencyKey() {
            assertProblem(payments.authorize(PaymentData.validAuthorization(), "   "),
                    400, "missing_idempotency_key");
        }

        @Test
        @DisplayName("[AUTH-35] Unsupported content type returns 415")
        void rejectsUnsupportedContentType() {
            payments.authorizeRaw("plain text", "text/plain")
                    .then()
                    .statusCode(415)
                    .contentType("application/problem+json");
        }
    }
}
