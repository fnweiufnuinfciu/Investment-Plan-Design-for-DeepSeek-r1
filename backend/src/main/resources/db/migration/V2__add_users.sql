-- V2: Add users table and analysis_history table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(200) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'USER',
    email VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS analysis_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    ticker VARCHAR(20) NOT NULL,
    report_date DATE,
    recommendation VARCHAR(20),
    recommendation_score INT,
    confidence DOUBLE,
    objective_ratio DOUBLE,
    subjective_ratio DOUBLE,
    volatility_20d DOUBLE,
    signal_score DOUBLE,
    target_weight DOUBLE,
    side VARCHAR(10),
    plan_id VARCHAR(50),
    input_source VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Insert default admin user (password: admin123)
INSERT INTO users (username, password, role, email) VALUES
('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN', 'admin@investment.com');
