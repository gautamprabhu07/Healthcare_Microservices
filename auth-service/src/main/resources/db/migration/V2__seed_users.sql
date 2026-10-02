-- All demo accounts use the password: password123
INSERT INTO users (id, email, password, role, first_name, last_name, specialization, enabled) VALUES
    ('223e4567-e89b-12d3-a456-426614174006', 'testuser@test.com', '$2a$12$MvygoCt1sRRWugaq6FTLX.Ia7nQ9HAs0uMLi9XJFhQ9pJgZyeLXhO', 'ADMIN', 'Test', 'User', NULL, TRUE),
    ('a1000000-0000-4000-8000-000000000001', 'admin@clinic.com', '$2a$12$FotBX2B6CgFjOsDFqRfZHuYjzqU7OtHxT5c9Qvh.Hlqkgv3/fTq7G', 'ADMIN', 'Alice', 'Admin', NULL, TRUE),
    ('a1000000-0000-4000-8000-000000000002', 'reception@clinic.com', '$2a$12$eEdK4r72gFyg89d3g2DCbOVdMBbbwWmqgi10qVnobvq4omG1YE.TC', 'RECEPTIONIST', 'Rachel', 'Reyes', NULL, TRUE),
    ('a1000000-0000-4000-8000-000000000003', 'dr.smith@clinic.com', '$2a$12$eYTtgNrKG1UkRaBCIsTv2.viIpUGrpGf6vfw5qHqsKm6cvf3E2G2.', 'DOCTOR', 'John', 'Smith', 'General Medicine', TRUE),
    ('a1000000-0000-4000-8000-000000000004', 'dr.patel@clinic.com', '$2a$12$OywF7GAXTXvUzhOS/qd3ue9pS61RZRmh9snL4r0DsPFj1KbVLPnBi', 'DOCTOR', 'Priya', 'Patel', 'Cardiology', TRUE),
    ('a1000000-0000-4000-8000-000000000005', 'dr.garcia@clinic.com', '$2a$12$yJJRrNlsoCCJhvBq1QxQF.AxQtMEPqQ/FR/2AXR4vSaxwIbJuT36i', 'DOCTOR', 'Maria', 'Garcia', 'Pediatrics', TRUE);
