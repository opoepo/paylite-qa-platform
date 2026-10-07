package io.paylite.sut.error;

/** Idempotency-Key header is present but empty or blank. */
public class MissingIdempotencyKeyException extends RuntimeException {

    public MissingIdempotencyKeyException() {
        super("Header 'Idempotency-Key' is required for this operation");
    }
}
