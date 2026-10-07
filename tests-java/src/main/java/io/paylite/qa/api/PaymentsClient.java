package io.paylite.qa.api;

import io.paylite.qa.model.AuthorizePaymentRequest;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;

/**
 * Operations of the PayLite payments API, named as in the contract.
 * Knows paths and required headers. Never asserts anything:
 * checking the response is the test's job.
 */
public class PaymentsClient {

    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    private final Map<String, String> extraHeaders;

    public PaymentsClient() {
        this(Map.of());
    }

    /** A client that sends these headers with every request, e.g. Accept-Language. */
    public PaymentsClient(Map<String, String> extraHeaders) {
        this.extraHeaders = Map.copyOf(extraHeaders);
    }

    /** Authorize with a fresh idempotency key — the usual case. */
    public Response authorize(AuthorizePaymentRequest request) {
        return authorize(request, UUID.randomUUID().toString());
    }

    /** Authorize with a chosen key — for idempotency tests. */
    public Response authorize(AuthorizePaymentRequest request, String idempotencyKey) {
        return request()
                .header(IDEMPOTENCY_HEADER, idempotencyKey)
                .body(request)
        .when()
                .post("/payments");
    }

    /** Authorize without the mandatory header — protocol tests only. */
    public Response authorizeWithoutIdempotencyKey(AuthorizePaymentRequest request) {
        return request()
                .body(request)
        .when()
                .post("/payments");
    }

    /**
     * Authorize with any body the typed model cannot express:
     * a Map with wrong JSON types, or a String that is not valid JSON.
     */
    public Response authorizeRaw(Object body) {
        return request()
                .header(IDEMPOTENCY_HEADER, UUID.randomUUID().toString())
                .body(body)
        .when()
                .post("/payments");
    }

    /** Same as authorizeRaw, with a chosen Content-Type — protocol tests only. */
    public Response authorizeRaw(Object body, String contentType) {
        return request()
                .header(IDEMPOTENCY_HEADER, UUID.randomUUID().toString())
                .contentType(contentType)
                .body(body)
        .when()
                .post("/payments");
    }

    public Response get(UUID paymentId) {
        return request()
        .when()
                .get("/payments/{id}", paymentId);
    }

    private RequestSpecification request() {
        return given()
                .spec(ApiSpecs.base())
                .headers(extraHeaders);
    }
}
