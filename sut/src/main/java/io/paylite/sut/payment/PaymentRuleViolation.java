package io.paylite.sut.payment;

/**
 * A business rule from docs/api-contract.md was violated.
 * Carries everything needed to build the problem+json response,
 * so a new rule needs no change in the exception handler.
 * The status is a plain int: the domain does not depend on Spring Web.
 */
public abstract class PaymentRuleViolation extends RuntimeException {

    private final int httpStatus;
    private final String code;
    private final String title;

    protected PaymentRuleViolation(int httpStatus, String code, String title, String detail) {
        super(detail);
        this.httpStatus = httpStatus;
        this.code = code;
        this.title = title;
    }

    public int httpStatus() {
        return httpStatus;
    }

    public String code() {
        return code;
    }

    public String title() {
        return title;
    }
}
