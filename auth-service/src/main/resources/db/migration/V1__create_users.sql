CREATE TABLE users (
    id             UUID                     PRIMARY KEY,
    email          VARCHAR(255)             NOT NULL UNIQUE,
    password       VARCHAR(255)             NOT NULL,
    role           VARCHAR(50)              NOT NULL,
    first_name     VARCHAR(100)             NOT NULL,
    last_name      VARCHAR(100)             NOT NULL,
    specialization VARCHAR(100),
    enabled        BOOLEAN                  NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
