package io.paylite.qa.assertions;

import io.paylite.qa.model.FieldViolation;
import io.paylite.qa.model.ProblemResponse;
import io.restassured.response.Response;
import org.assertj.core.groups.Tuple;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * Checks for error responses as defined in docs/api-contract.md, section Errors.
 */
public final class ApiAssertions {

    private static final String PROBLEM_JSON = "application/problem+json";
    private static final String TYPE_BASE = "https://paylite.local/errors/";

    private ApiAssertions() {
    }

    /**
     * Envelope shared by every error (test cases ERR-01, ERR-02):
     * HTTP status, content type, code, type URI, status field, instance.
     */
    public static ProblemResponse assertProblem(Response response, int status, String code) {
        response.then()
                .statusCode(status)
                .contentType(PROBLEM_JSON);

        ProblemResponse problem = response.as(ProblemResponse.class);
        assertSoftly(softly -> {
            softly.assertThat(problem.code()).as("code").isEqualTo(code);
            softly.assertThat(problem.status()).as("status field").isEqualTo(status);
            softly.assertThat(problem.type()).as("type")
                    .isEqualTo(TYPE_BASE + code.replace('_', '-'));
            softly.assertThat(problem.instance()).as("instance").isNotBlank();
        });
        return problem;
    }

    /**
     * 422 validation_failed whose errors[] holds exactly these field/code
     * pairs, in this order, and nothing else. An extra unexpected error
     * fails the check, so a test cannot pass for the wrong reason.
     */
    public static ProblemResponse assertValidationFailed(Response response, Violation... expected) {
        ProblemResponse problem = assertProblem(response, 422, "validation_failed");

        Tuple[] expectedPairs = Arrays.stream(expected)
                .map(v -> tuple(v.field(), v.code()))
                .toArray(Tuple[]::new);

        assertThat(problem.errors())
                .as("errors[] as (field, code)")
                .extracting(FieldViolation::field, FieldViolation::code)
                .containsExactly(expectedPairs);
        return problem;
    }
}
