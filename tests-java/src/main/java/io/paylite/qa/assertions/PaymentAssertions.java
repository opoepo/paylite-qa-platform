package io.paylite.qa.assertions;

import io.paylite.qa.model.PaymentResponse;
import io.paylite.qa.steps.PaymentSteps;

import static org.assertj.core.api.Assertions.assertThat;

public class PaymentAssertions {

    private final PaymentSteps paymentSteps;

    public PaymentAssertions(PaymentSteps steps) {
        this.paymentSteps = steps;
    }

    /**
     * A rejected operation must leave no trace: every field, including updatedAt, is the same
     */
    public void assertUnchanged(PaymentResponse expected) {
        assertThat(paymentSteps.fetch(expected.id()))
                .as("payment after the rejected operation")
                .isEqualTo(expected);
    }

}
