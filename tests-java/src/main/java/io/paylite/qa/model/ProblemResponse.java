package io.paylite.qa.model;

import java.util.List;

/**
 * application/problem+json error body (RFC 9457) as defined in
 * docs/api-contract.md, section Errors. "errors" is present only for
 * validation_failed.
 */
public record ProblemResponse(
        String type,
        String title,
        Integer status,
        String detail,
        String instance,
        String code,
        List<FieldViolation> errors
) {
}
