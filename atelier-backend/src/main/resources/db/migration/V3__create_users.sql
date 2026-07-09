CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,

                       email VARCHAR(255) NOT NULL UNIQUE,
                       name VARCHAR(255),

                       google_id VARCHAR(255),
                       role VARCHAR(255),

                       created_at TIMESTAMP,
                       last_login_at TIMESTAMP
);