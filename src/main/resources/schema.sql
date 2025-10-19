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

-- Test results table
CREATE TABLE IF NOT EXISTS test_results (
    id SERIAL PRIMARY KEY,
    user_id BIGINT,
    guest_token UUID,
    session_id UUID NOT NULL,
    mbti_type VARCHAR(4),
    riasec_code VARCHAR(4),
    course_path TEXT,
    career_suggestions TEXT,
    student_goals TEXT,
    generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    taken_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Add new columns to existing test_results table (safe migration)
-- These statements are safe to run multiple times
ALTER TABLE test_results ADD COLUMN IF NOT EXISTS age INTEGER;
ALTER TABLE test_results ADD COLUMN IF NOT EXISTS gender VARCHAR(20);
ALTER TABLE test_results ADD COLUMN IF NOT EXISTS is_from_plmar BOOLEAN;
-- Align with TestResult entity: add learning_style, study_tips, personality_growth_tips
-- Removed learning_style, study_tips, personality_growth_tips from schema (major cleanup)

-- Indexes for test_results table
CREATE INDEX IF NOT EXISTS idx_test_results_user_id ON test_results(user_id);
CREATE INDEX IF NOT EXISTS idx_test_results_guest_token ON test_results(guest_token);
CREATE INDEX IF NOT EXISTS idx_test_results_session_id ON test_results(session_id);
CREATE INDEX IF NOT EXISTS idx_test_results_mbti_type ON test_results(mbti_type);
CREATE INDEX IF NOT EXISTS idx_test_results_riasec_code ON test_results(riasec_code);
CREATE INDEX IF NOT EXISTS idx_test_results_generated_at ON test_results(generated_at);
CREATE INDEX IF NOT EXISTS idx_test_results_age ON test_results(age);
CREATE INDEX IF NOT EXISTS idx_test_results_gender ON test_results(gender);
CREATE INDEX IF NOT EXISTS idx_test_results_is_from_plmar ON test_results(is_from_plmar);

-- New MBTI+RIASEC matching table used by the application
CREATE TABLE IF NOT EXISTS mbti_riasec_matching (
    id BIGSERIAL PRIMARY KEY,
    mbti_type VARCHAR(4) NOT NULL,
    riasec_code VARCHAR(2) NOT NULL,
    courses TEXT[],
    careers TEXT[],
    explanation TEXT
);

CREATE INDEX IF NOT EXISTS idx_mbti_riasec_matching_mbti_type ON mbti_riasec_matching(mbti_type);
CREATE INDEX IF NOT EXISTS idx_mbti_riasec_matching_riasec_code ON mbti_riasec_matching(riasec_code);
CREATE INDEX IF NOT EXISTS idx_mbti_riasec_matching_combination ON mbti_riasec_matching(mbti_type, riasec_code);

-- Updated description tables used by the application
CREATE TABLE IF NOT EXISTS updated_career_description (
    id BIGSERIAL PRIMARY KEY,
    career_name TEXT,
    description TEXT
);

CREATE TABLE IF NOT EXISTS updated_course_description (
    id BIGSERIAL PRIMARY KEY,
    course_name TEXT,
    description TEXT
);

-- Detailed scoring data table for personality test results
CREATE TABLE IF NOT EXISTS personality_test_scores (
    id BIGSERIAL PRIMARY KEY,
    test_result_id BIGINT NOT NULL,
    session_id UUID NOT NULL,
    
    -- RIASEC Scores (raw and percentage)
    riasec_r_raw INTEGER NOT NULL,
    riasec_r_percentage DECIMAL(5,2) NOT NULL,
    riasec_i_raw INTEGER NOT NULL,
    riasec_i_percentage DECIMAL(5,2) NOT NULL,
    riasec_a_raw INTEGER NOT NULL,
    riasec_a_percentage DECIMAL(5,2) NOT NULL,
    riasec_s_raw INTEGER NOT NULL,
    riasec_s_percentage DECIMAL(5,2) NOT NULL,
    riasec_e_raw INTEGER NOT NULL,
    riasec_e_percentage DECIMAL(5,2) NOT NULL,
    riasec_c_raw INTEGER NOT NULL,
    riasec_c_percentage DECIMAL(5,2) NOT NULL,
    
    -- MBTI Scores (raw and percentage)
    mbti_e_raw INTEGER NOT NULL,
    mbti_e_percentage DECIMAL(5,2) NOT NULL,
    mbti_i_raw INTEGER NOT NULL,
    mbti_i_percentage DECIMAL(5,2) NOT NULL,
    mbti_s_raw INTEGER NOT NULL,
    mbti_s_percentage DECIMAL(5,2) NOT NULL,
    mbti_n_raw INTEGER NOT NULL,
    mbti_n_percentage DECIMAL(5,2) NOT NULL,
    mbti_t_raw INTEGER NOT NULL,
    mbti_t_percentage DECIMAL(5,2) NOT NULL,
    mbti_f_raw INTEGER NOT NULL,
    mbti_f_percentage DECIMAL(5,2) NOT NULL,
    mbti_j_raw INTEGER NOT NULL,
    mbti_j_percentage DECIMAL(5,2) NOT NULL,
    mbti_p_raw INTEGER NOT NULL,
    mbti_p_percentage DECIMAL(5,2) NOT NULL,
    
    -- Final Results
    final_riasec_code VARCHAR(10) NOT NULL,
    final_mbti_type VARCHAR(4) NOT NULL,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key constraint
    CONSTRAINT fk_personality_test_scores_test_result 
        FOREIGN KEY (test_result_id) REFERENCES test_results(id) ON DELETE CASCADE
);

-- Indexes for better performance
CREATE INDEX IF NOT EXISTS idx_personality_test_scores_test_result_id ON personality_test_scores(test_result_id);
CREATE INDEX IF NOT EXISTS idx_personality_test_scores_session_id ON personality_test_scores(session_id);
CREATE INDEX IF NOT EXISTS idx_personality_test_scores_final_riasec ON personality_test_scores(final_riasec_code);
CREATE INDEX IF NOT EXISTS idx_personality_test_scores_final_mbti ON personality_test_scores(final_mbti_type);

ALTER TABLE development_plan DROP COLUMN IF EXISTS cert_learning_resources;

-- Admin accounts table (for system administrators)
CREATE TABLE IF NOT EXISTS admins (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    is_active BOOLEAN DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_admins_email ON admins(email);
CREATE INDEX IF NOT EXISTS idx_admins_username ON admins(username);