package io.paylite.sut.payment;

public class RefundExceedsCapturedException extends PaymentRuleViolation {

    public RefundExceedsCapturedException(long requested, long refundable) {
        super(422, "refund_exceeds_captured", "Refund exceeds captured amount",
                "Requested " + requested + ", refundable remainder is " + refundable);
    }
}
