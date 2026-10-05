package io.paylite.qa.api;

import io.paylite.qa.model.AuthorizePaymentRequest;
import io.restassured.response.Response;

import java.util.UUID;

import static io.restassured.RestAssured.given;

/**
 * Operations of the PayLite payments API, named as in the contract.
 * Knows paths and required headers. Never asserts anything:
 * checking the response is the test's job.
 */
public class PaymentsClient {

    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    /** Authorize with a fresh idempotency key — the usual case. */
    public Response authorize(AuthorizePaymentRequest request) {
        return authorize(request, UUID.randomUUID().toString());
    }

    /** Authorize with a chosen key — for idempotency tests. */
    public Response authorize(AuthorizePaymentRequest request, String idempotencyKey) {
        return given()
                .spec(ApiSpecs.base())
                .header(IDEMPOTENCY_HEADER, idempotencyKey)
                .body(request)
        .when()
                .post("/payments");
    }

    public Response get(UUID paymentId) {
        return given()
                .spec(ApiSpecs.base())
        .when()
                .get("/payments/{id}", paymentId);
    }
}
