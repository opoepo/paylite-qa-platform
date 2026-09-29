package io.paylite.sut.error;

public record FieldViolation(String field, String code, String message) {
}
