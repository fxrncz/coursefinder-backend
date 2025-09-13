-- Create users table if it doesn't exist
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    age INTEGER,
    gender VARCHAR(10),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create index for better performance
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- Pending user registration (store until email verified)
CREATE TABLE IF NOT EXISTS pending_registration (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    code_hash VARCHAR(128) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    consumed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_pending_registration_email ON pending_registration(email);

-- Password reset table
CREATE TABLE IF NOT EXISTS password_reset (
    id SERIAL PRIMARY KEY,
    user_id BIGINT,
    email VARCHAR(100) NOT NULL,
    code_hash VARCHAR(128) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    consumed BOOLEAN NOT NULL DEFAULT FALSE,
    attempts INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_password_reset_email ON password_reset(email);

-- MBTI detailed information table
CREATE TABLE IF NOT EXISTS mbti_details (
    id SERIAL PRIMARY KEY,
    mbti_type VARCHAR(10) UNIQUE NOT NULL,
    title VARCHAR(100),
    description TEXT,
    learning_style_summary TEXT NOT NULL,
    learning_style_details TEXT NOT NULL,
    learning_style_environments TEXT NOT NULL,
    learning_style_resources TEXT NOT NULL,
    study_tips_summary TEXT NOT NULL,
    study_tips_details TEXT NOT NULL,
    study_tips_dos TEXT NOT NULL,
    study_tips_donts TEXT NOT NULL,
    study_tips_common_mistakes TEXT NOT NULL,
    growth_strengths TEXT NOT NULL,
    growth_weaknesses TEXT NOT NULL,
    growth_opportunities TEXT NOT NULL,
    growth_challenges TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_mbti_details_type ON mbti_details(mbti_type);

