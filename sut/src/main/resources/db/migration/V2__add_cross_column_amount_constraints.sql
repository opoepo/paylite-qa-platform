-- Cross-column money invariants from docs/api-contract.md (Business rules 1-2).
-- Per-column CHECKs in V1 cannot express relations between columns.

ALTER TABLE payments
    ADD CONSTRAINT chk_payments_captured_le_authorized
        CHECK (captured_amount <= authorized_amount),
    ADD CONSTRAINT chk_payments_refunded_le_captured
        CHECK (refunded_amount <= captured_amount);
