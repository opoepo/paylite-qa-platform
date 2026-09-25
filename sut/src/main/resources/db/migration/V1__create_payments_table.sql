-- Payments: the central entity of PayLite.
-- All amounts are stored in minor units (cents for USD/EUR, whole units for VND).
-- See ADR-0001.

CREATE TABLE payments (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_reference             VARCHAR(255)    NOT NULL,
    currency                    CHAR(3)         NOT NULL
                                    CHECK (currency IN ('USD', 'EUR', 'VND')),
    authorized_amount           BIGINT          NOT NULL
                                    CHECK (authorized_amount > 0),
    captured_amount             BIGINT          NOT NULL DEFAULT 0
                                    CHECK (captured_amount >= 0),
    refunded_amount             BIGINT          NOT NULL DEFAULT 0
                                    CHECK (refunded_amount >= 0),
    status                      VARCHAR(30)     NOT NULL DEFAULT 'AUTHORIZED'
                                    CHECK (status IN (
                                        'AUTHORIZED',
                                        'CANCELLED',
                                        'EXPIRED',
                                        'CAPTURED',
                                        'PARTIALLY_REFUNDED',
                                        'REFUNDED'
                                    )),
    authorization_expires_at    TIMESTAMPTZ     NOT NULL,
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- Support filtering and pagination from GET /payments
CREATE INDEX idx_payments_order_reference  ON payments (order_reference);
CREATE INDEX idx_payments_status           ON payments (status);
CREATE INDEX idx_payments_created_at       ON payments (created_at DESC);
