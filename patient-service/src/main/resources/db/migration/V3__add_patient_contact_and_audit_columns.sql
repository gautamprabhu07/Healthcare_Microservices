ALTER TABLE patient ADD COLUMN phone_number VARCHAR(20);
ALTER TABLE patient ADD COLUMN gender VARCHAR(10);
ALTER TABLE patient ADD COLUMN created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE patient ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Existing demo patients: audit timestamps follow their registration date.
UPDATE patient
SET created_at = CAST(registered_date AS TIMESTAMP WITH TIME ZONE),
    updated_at = CAST(registered_date AS TIMESTAMP WITH TIME ZONE);

-- Demo contact details for the seeded patients.
UPDATE patient SET gender = 'MALE',   phone_number = '+1-555-0101' WHERE id = '123e4567-e89b-12d3-a456-426614174000';
UPDATE patient SET gender = 'FEMALE', phone_number = '+1-555-0102' WHERE id = '123e4567-e89b-12d3-a456-426614174001';
UPDATE patient SET gender = 'FEMALE', phone_number = '+1-555-0103' WHERE id = '123e4567-e89b-12d3-a456-426614174002';
UPDATE patient SET gender = 'MALE',   phone_number = '+1-555-0104' WHERE id = '123e4567-e89b-12d3-a456-426614174003';
UPDATE patient SET gender = 'FEMALE', phone_number = '+1-555-0105' WHERE id = '123e4567-e89b-12d3-a456-426614174004';
UPDATE patient SET gender = 'MALE',   phone_number = '+1-555-0106' WHERE id = '223e4567-e89b-12d3-a456-426614174005';
UPDATE patient SET gender = 'FEMALE', phone_number = '+1-555-0107' WHERE id = '223e4567-e89b-12d3-a456-426614174006';
UPDATE patient SET gender = 'MALE',   phone_number = '+1-555-0108' WHERE id = '223e4567-e89b-12d3-a456-426614174007';
UPDATE patient SET gender = 'FEMALE', phone_number = '+1-555-0109' WHERE id = '223e4567-e89b-12d3-a456-426614174008';
UPDATE patient SET gender = 'MALE',   phone_number = '+1-555-0110' WHERE id = '223e4567-e89b-12d3-a456-426614174009';
UPDATE patient SET gender = 'FEMALE', phone_number = '+1-555-0111' WHERE id = '223e4567-e89b-12d3-a456-426614174010';
UPDATE patient SET gender = 'MALE',   phone_number = '+1-555-0112' WHERE id = '223e4567-e89b-12d3-a456-426614174011';
UPDATE patient SET gender = 'FEMALE', phone_number = '+1-555-0113' WHERE id = '223e4567-e89b-12d3-a456-426614174012';
UPDATE patient SET gender = 'MALE',   phone_number = '+1-555-0114' WHERE id = '223e4567-e89b-12d3-a456-426614174013';
UPDATE patient SET gender = 'FEMALE', phone_number = '+1-555-0115' WHERE id = '223e4567-e89b-12d3-a456-426614174014';
