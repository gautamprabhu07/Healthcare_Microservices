CREATE TABLE appointment (
    id            UUID                     PRIMARY KEY,
    patient_id    UUID                     NOT NULL,
    patient_name  VARCHAR(255)             NOT NULL,
    patient_email VARCHAR(255)             NOT NULL,
    doctor_id     UUID                     NOT NULL,
    doctor_name   VARCHAR(255)             NOT NULL,
    start_time    TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time      TIMESTAMP WITH TIME ZONE NOT NULL,
    reason        VARCHAR(255),
    notes         VARCHAR(2000),
    status        VARCHAR(20)              NOT NULL,
    cancel_reason VARCHAR(255),
    created_by    UUID                     NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version       BIGINT                   NOT NULL DEFAULT 0,
    CHECK (end_time > start_time)
);

CREATE INDEX idx_appointment_doctor_start ON appointment (doctor_id, start_time);
CREATE INDEX idx_appointment_patient_start ON appointment (patient_id, start_time);
CREATE INDEX idx_appointment_status ON appointment (status);
