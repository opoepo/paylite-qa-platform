package io.paylite.qa.data;

import io.paylite.qa.model.AuthorizePaymentRequest;

import java.util.UUID;

/**
 * Test data for the payments API.
 * Unique where uniqueness matters (order reference), fixed and
 * predictable everywhere else, so a failure is reproducible.
 */
public final class PaymentData {

    private static final String DEFAULT_CURRENCY = "USD";
    private static final long DEFAULT_AMOUNT = 540L;

    private PaymentData() {
    }

    /** A request the API must accept. Negative cases derive from it via with...(). */
    public static AuthorizePaymentRequest validAuthorization() {
        return new AuthorizePaymentRequest(uniqueOrderReference(), DEFAULT_CURRENCY, DEFAULT_AMOUNT);
    }

    /** "QA-" marks rows created by automated tests, as opposed to manual curl checks. */
    public static String uniqueOrderReference() {
        return "QA-" + UUID.randomUUID();
    }
}
