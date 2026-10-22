package io.paylite.sut.payment;

import java.time.Instant;

public class AuthorizationExpiredException extends PaymentRuleViolation {

    public AuthorizationExpiredException(Instant expiredAt) {
        super(409, "authorization_expired", "Authorization expired",
                "The hold expired at " + expiredAt);
    }
}
