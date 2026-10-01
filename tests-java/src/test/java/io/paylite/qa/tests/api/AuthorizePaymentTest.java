package io.paylite.qa.tests.api;

import io.paylite.qa.api.ApiSpecs;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

class AuthorizePaymentTest {

    @Test
    @DisplayName("[AUTH-01] Authorize a payment")
    void authorizesPayment() {
        String orderReference = "ORD-" + UUID.randomUUID();

        given()
                .spec(ApiSpecs.base())
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body("""
                        {"orderReference": "%s", "currency": "USD", "amount": 540}
                        """.formatted(orderReference))
        .when()
                .post("/payments")
        .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("orderReference", equalTo(orderReference))
                .body("currency", equalTo("USD"))
                .body("authorizedAmount", equalTo(540))
                .body("capturedAmount", equalTo(0))
                .body("refundedAmount", equalTo(0))
                .body("status", equalTo("AUTHORIZED"));
    }

    @Test
    @DisplayName("[AUTH-02] Location header points to the created payment")
    void locationHeaderPointsToCreatedPayment() {
        Response response = given()
                .spec(ApiSpecs.base())
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body("""
                        {"orderReference": "ORD-%s", "currency": "USD", "amount": 540}
                        """.formatted(UUID.randomUUID()))
        .when()
                .post("/payments")
        .then()
                .statusCode(201)
                .extract().response();

        String id = response.path("id");
        assertThat(response.header("Location")).isEqualTo("/api/v1/payments/" + id);
    }

    @Test
    @DisplayName("[GET-02] Unknown payment returns 404 payment_not_found")
    void unknownPaymentReturnsNotFound() {
        String id = UUID.randomUUID().toString();

        given()
                .spec(ApiSpecs.base())
        .when()
                .get("/payments/{id}", id)
        .then()
                .statusCode(404)
                .contentType("application/problem+json")
                .body("code", equalTo("payment_not_found"))
                .body("instance", equalTo("/api/v1/payments/" + id));
    }
}
