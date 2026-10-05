package io.paylite.qa.tests.api;

import io.paylite.qa.api.PaymentsClient;
import io.paylite.qa.data.PaymentData;
import io.paylite.qa.model.AuthorizePaymentRequest;
import io.paylite.qa.model.Currency;
import io.paylite.qa.model.PaymentResponse;
import io.paylite.qa.model.PaymentStatus;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class AuthorizePaymentTest {

    private final PaymentsClient payments = new PaymentsClient();

    @Test
    @DisplayName("[AUTH-01] Authorize a payment")
    void authorizesPayment() {
        AuthorizePaymentRequest request = PaymentData.validAuthorization();

        PaymentResponse payment = payments.authorize(request)
                .then().statusCode(201)
                .extract().as(PaymentResponse.class);

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
}
