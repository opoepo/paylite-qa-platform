package io.paylite.sut.payment;

/**
 * Supported currencies and their minor-unit exponent (ADR-0001).
 * USD 12.34 is stored as 1234 (exponent 2); VND 12 is stored as 12 (exponent 0).
 */
public enum Currency {
    USD(2),
    EUR(2),
    VND(0);

    private final int exponent;

    Currency(int exponent) {
        this.exponent = exponent;
    }

    public int exponent() {
        return exponent;
    }
}
