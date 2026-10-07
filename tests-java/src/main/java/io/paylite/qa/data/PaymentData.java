package io.paylite.qa.data;

import io.paylite.qa.model.AuthorizePaymentRequest;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Test data for the payments API.
 * Unique where uniqueness matters (order reference), fixed and
 * predictable everywhere else, so a failure is reproducible.
 */
public final class PaymentData {

    private static final String DEFAULT_CURRENCY = "USD";
    private static final long DEFAULT_AMOUNT = 540L;
    private static final String TEST_PREFIX = "QA-";

    private PaymentData() {
    }

    /** A request the API must accept. Negative cases derive from it via with...(). */
    public static AuthorizePaymentRequest validAuthorization() {
        return new AuthorizePaymentRequest(uniqueOrderReference(), DEFAULT_CURRENCY, DEFAULT_AMOUNT);
    }

    /**
     * The valid request as a mutable map with one field replaced by a value
     * of any JSON type — for input the typed model cannot express
     * (12.5, "100", a number where text is expected).
     */
    public static Map<String, Object> validAuthorizationWith(String field, Object value) {
        AuthorizePaymentRequest valid = validAuthorization();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("orderReference", valid.orderReference());
        body.put("currency", valid.currency());
        body.put("amount", valid.amount());
        body.put(field, value);
        return body;
    }

    /** "QA-" marks rows created by automated tests, as opposed to manual curl checks. */
    public static String uniqueOrderReference() {
        return TEST_PREFIX + UUID.randomUUID();
    }

    /** A unique order reference padded to exactly the given length — for boundary tests. */
    public static String uniqueOrderReferenceOfLength(int length) {
        String base = uniqueOrderReference();
        if (length < base.length()) {
            throw new IllegalArgumentException(
                    "Length " + length + " is shorter than a unique reference (" + base.length() + ")");
        }
        return base + "x".repeat(length - base.length());
    }
}
