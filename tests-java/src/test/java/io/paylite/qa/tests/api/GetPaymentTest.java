package io.paylite.qa.tests.api;

import io.paylite.qa.api.PaymentsClient;
import io.paylite.qa.data.PaymentData;
import io.paylite.qa.model.PaymentResponse;
import io.paylite.qa.model.ProblemResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GetPaymentTest {

    private final PaymentsClient payments = new PaymentsClient();

    @Test
    @DisplayName("[GET-01] Existing payment is returned exactly as created")
    void returnsExistingPayment() {
        PaymentResponse created = payments.authorize(PaymentData.validAuthorization())
                .then().statusCode(201)
                .extract().as(PaymentResponse.class);

        PaymentResponse fetched = payments.get(created.id())
                .then().statusCode(200)
                .extract().as(PaymentResponse.class);

        assertThat(fetched).isEqualTo(created);
    }

    @Test
    @DisplayName("[GET-02] Unknown payment returns 404 payment_not_found")
    void unknownPaymentReturnsNotFound() {
        UUID id = UUID.randomUUID();

        ProblemResponse problem = payments.get(id)
                .then().statusCode(404)
                .contentType("application/problem+json")
                .extract().as(ProblemResponse.class);

        assertThat(problem.code()).isEqualTo("payment_not_found");
        assertThat(problem.instance()).isEqualTo("/api/v1/payments/" + id);
    }
}
