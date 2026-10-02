package io.paylite.qa.model;

/** One entry of errors[] in a validation_failed response. */
public record FieldViolation(String field, String code, String message) {
}
