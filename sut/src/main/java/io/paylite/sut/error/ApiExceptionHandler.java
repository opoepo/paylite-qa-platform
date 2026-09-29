package io.paylite.sut.error;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import io.paylite.sut.payment.Currency;
import io.paylite.sut.payment.PaymentNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Translates every error into application/problem+json (RFC 9457)
 * with a machine-readable top-level "code" and, for input problems,
 * a per-field "errors" list. See docs/api-contract.md, section Errors.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private static final String TYPE_BASE = "https://paylite.local/errors/";
    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    // ── Domain exceptions ───────────────────────────────────────────────

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<Object> handlePaymentNotFound(PaymentNotFoundException ex,
                                                        HttpServletRequest request) {
        return respond(problem(HttpStatus.NOT_FOUND, "payment_not_found",
                "Payment not found", ex.getMessage(), request.getRequestURI()));
    }

    // ── Framework exceptions (overrides of ResponseEntityExceptionHandler) ──

    /** Bean Validation failed: @NotNull, @Positive, @Size on request DTOs. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        List<FieldViolation> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> violation(fe.getField(), fieldCode(fe)))
                .sorted(Comparator.comparing(FieldViolation::field)
                        .thenComparing(FieldViolation::code))
                .toList();
        return respond(validationFailed(violations, path(request)));
    }

    /** JSON could not be read: broken syntax, wrong type, unknown field. */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        Throwable cause = ex.getCause();

        if (cause instanceof UnrecognizedPropertyException unknown) {
            return respond(problem(HttpStatus.BAD_REQUEST, "malformed_request", "Malformed request",
                    "Unknown field '" + unknown.getPropertyName() + "'", path(request)));
        }

        if (cause instanceof MismatchedInputException mismatch && !mismatch.getPath().isEmpty()) {
            String field = mismatch.getPath().get(mismatch.getPath().size() - 1).getFieldName();
            if (field != null) {
                return respond(validationFailed(List.of(typeViolation(field)), path(request)));
            }
        }

        return respond(problem(HttpStatus.BAD_REQUEST, "malformed_request", "Malformed request",
                "Request body is missing or is not valid JSON", path(request)));
    }

    /** Missing required header, e.g. Idempotency-Key. */
    @Override
    protected ResponseEntity<Object> handleServletRequestBindingException(ServletRequestBindingException ex,
                                                                          HttpHeaders headers,
                                                                          HttpStatusCode status,
                                                                          WebRequest request) {
        if (ex instanceof MissingRequestHeaderException missing
                && IDEMPOTENCY_HEADER.equalsIgnoreCase(missing.getHeaderName())) {
            return respond(problem(HttpStatus.BAD_REQUEST, "missing_idempotency_key",
                    "Missing Idempotency-Key",
                    "Header 'Idempotency-Key' is required for this operation", path(request)));
        }
        return respond(problem(HttpStatus.BAD_REQUEST, "malformed_request", "Malformed request",
                ex.getMessage(), path(request)));
    }

    /** Path or query parameter of the wrong type, e.g. GET /payments/not-a-uuid. */
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex,
                                                        HttpHeaders headers,
                                                        HttpStatusCode status,
                                                        WebRequest request) {
        String name = (ex instanceof MethodArgumentTypeMismatchException m) ? m.getName() : "parameter";
        return respond(problem(HttpStatus.BAD_REQUEST, "malformed_request", "Malformed request",
                "Invalid format of '" + name + "'", path(request)));
    }

    // ── Safety net ──────────────────────────────────────────────────────

    /** Anything unexpected: log the details, never leak them to the client. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return respond(problem(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error", "Internal error",
                "Unexpected error. The incident has been logged.", request.getRequestURI()));
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private static ProblemDetail problem(HttpStatus status, String code, String title,
                                         String detail, String path) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(URI.create(TYPE_BASE + code.replace('_', '-')));
        pd.setTitle(title);
        pd.setInstance(URI.create(path));
        pd.setProperty("code", code);
        return pd;
    }

    private static ProblemDetail validationFailed(List<FieldViolation> violations, String path) {
        ProblemDetail pd = problem(HttpStatus.UNPROCESSABLE_ENTITY, "validation_failed",
                "Validation failed", "One or more fields are invalid", path);
        pd.setProperty("errors", violations);
        return pd;
    }

    /** Field-level code for a JSON type error caught by Jackson. */
    private static FieldViolation typeViolation(String field) {
        String code = switch (field) {
            case "amount" -> "invalid_amount";
            case "currency" -> "unsupported_currency";
            default -> "invalid_value";
        };
        return violation(field, code);
    }

    private static FieldViolation violation(String field, String code) {
        return new FieldViolation(field, code, messageFor(code));
    }

    /**
     * API messages are fixed English strings keyed by code — never the
     * validator's localized defaults, which depend on server and client locale.
     */
    private static String messageFor(String code) {
        return switch (code) {
            case "required" -> "is required";
            case "too_long" -> "is too long";
            case "invalid_amount" -> "must be a positive integer in minor units";
            case "unsupported_currency" -> "must be one of " + supportedCurrencies();
            default -> "has an invalid value";
        };
    }

    /** Field-level code for a Bean Validation constraint. */
    private static String fieldCode(FieldError error) {
        String constraint = Objects.requireNonNullElse(error.getCode(), "");
        return switch (constraint) {
            case "NotNull", "NotBlank" -> "required";
            case "Size" -> "too_long";
            case "Positive" -> "invalid_amount";
            default -> "invalid_value";
        };
    }

    private static String supportedCurrencies() {
        return Arrays.stream(Currency.values())
                .map(Enum::name)
                .collect(Collectors.joining(", "));
    }

    private static String path(WebRequest request) {
        if (request instanceof ServletWebRequest servlet) {
            return servlet.getRequest().getRequestURI();
        }
        return "";
    }

    private static ResponseEntity<Object> respond(ProblemDetail pd) {
        return ResponseEntity.status(pd.getStatus())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(pd);
    }
}
