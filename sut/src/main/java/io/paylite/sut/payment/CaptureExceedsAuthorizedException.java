package io.paylite.sut.payment;

public class CaptureExceedsAuthorizedException extends PaymentRuleViolation {

    public CaptureExceedsAuthorizedException(long requested, long authorized) {
        super(422, "capture_exceeds_authorized", "Capture exceeds authorized amount",
                "Requested " + requested + ", authorized " + authorized);
    }
}
