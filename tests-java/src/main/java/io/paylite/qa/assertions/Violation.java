package io.paylite.qa.assertions;

/** Expected entry of errors[]: which field and which code. */
public record Violation(String field, String code) {

    public static Violation violation(String field, String code) {
        return new Violation(field, code);
    }
}
