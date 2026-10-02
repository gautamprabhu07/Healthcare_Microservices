CREATE TABLE billing_account (
    id            UUID                     PRIMARY KEY,
    patient_id    UUID                     NOT NULL UNIQUE,
    patient_name  VARCHAR(255)             NOT NULL,
    patient_email VARCHAR(255)             NOT NULL,
    status        VARCHAR(20)              NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Invoice numbers look like INV-2026-001000; the sequence leaves room for the seed data.
CREATE SEQUENCE invoice_number_seq START WITH 1000;

CREATE TABLE invoice (
    id             UUID                     PRIMARY KEY,
    invoice_number VARCHAR(30)              NOT NULL UNIQUE,
    account_id     UUID                     NOT NULL REFERENCES billing_account (id),
    patient_id     UUID                     NOT NULL,
    appointment_id UUID                     UNIQUE,
    description    VARCHAR(255)             NOT NULL,
    amount         NUMERIC(10, 2)           NOT NULL CHECK (amount > 0),
    currency       VARCHAR(3)               NOT NULL,
    status         VARCHAR(20)              NOT NULL,
    issued_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    due_date       DATE                     NOT NULL,
    paid_at        TIMESTAMP WITH TIME ZONE,
    version        BIGINT                   NOT NULL DEFAULT 0
);

CREATE INDEX idx_invoice_account ON invoice (account_id);
CREATE INDEX idx_invoice_patient ON invoice (patient_id);
CREATE INDEX idx_invoice_status ON invoice (status);
