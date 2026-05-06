-- V1__init_schema.sql
-- Initial database schema for DeepSeek-R1 Investment Decision System

CREATE TABLE IF NOT EXISTS reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticker VARCHAR(10) NOT NULL,
    report_date DATE NOT NULL,
    report_text TEXT NOT NULL,
    source VARCHAR(100),
    sector VARCHAR(50),
    analyst_recommendation VARCHAR(20),
    objective_ratio DOUBLE DEFAULT 0.5,
    subjective_ratio DOUBLE DEFAULT 0.5,
    volatility_20d DOUBLE DEFAULT 0.3,
    future_ar_60d DOUBLE,
    daily_returns_60d JSON,
    quality_flags VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_ticker (ticker),
    INDEX idx_report_date (report_date),
    INDEX idx_ticker_date (ticker, report_date)
);

CREATE TABLE IF NOT EXISTS analysis_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_id BIGINT NOT NULL,
    ticker VARCHAR(10) NOT NULL,
    recommendation VARCHAR(20) NOT NULL,
    recommendation_score INT NOT NULL,
    confidence DOUBLE NOT NULL,
    weight DOUBLE NOT NULL,
    rationale TEXT,
    objective_summary TEXT,
    subjective_summary TEXT,
    key_evidence JSON,
    risk_factors JSON,
    signal_score DOUBLE,
    quality_score DOUBLE,
    api_latency_ms BIGINT,
    api_model VARCHAR(50),
    api_error TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (report_id) REFERENCES reports(id),
    INDEX idx_report_id (report_id),
    INDEX idx_ticker (ticker)
);

CREATE TABLE IF NOT EXISTS portfolios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    mode VARCHAR(20) NOT NULL,
    capital DOUBLE NOT NULL,
    universe_size INT,
    selected_positions INT,
    long_positions INT,
    short_positions INT,
    gross_exposure DOUBLE,
    net_exposure DOUBLE,
    settings JSON,
    api_summary JSON,
    methodology TEXT,
    generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_generated_at (generated_at)
);

CREATE TABLE IF NOT EXISTS portfolio_positions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    portfolio_id BIGINT NOT NULL,
    ticker VARCHAR(10) NOT NULL,
    side VARCHAR(10) NOT NULL,
    recommendation VARCHAR(20),
    recommendation_score INT,
    confidence DOUBLE,
    objective_ratio DOUBLE,
    subjective_ratio DOUBLE,
    subjective_weight DOUBLE,
    volatility_20d DOUBLE,
    signal_score DOUBLE,
    quality_score DOUBLE,
    target_weight DOUBLE,
    target_dollar DOUBLE,
    hold_days INT,
    stop_loss_pct DOUBLE,
    take_profit_pct DOUBLE,
    notes JSON,
    FOREIGN KEY (portfolio_id) REFERENCES portfolios(id) ON DELETE CASCADE,
    INDEX idx_portfolio_id (portfolio_id)
);

CREATE TABLE IF NOT EXISTS backtest_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    portfolio_id BIGINT NOT NULL,
    hold_days INT,
    stop_loss_pct DOUBLE,
    take_profit_pct DOUBLE,
    cumulative_return DOUBLE,
    annualized_return DOUBLE,
    annualized_volatility DOUBLE,
    sharpe DOUBLE,
    sortino DOUBLE,
    max_drawdown DOUBLE,
    daily_win_rate DOUBLE,
    var_95 DOUBLE,
    cvar_95 DOUBLE,
    position_count INT,
    winner_count INT,
    loser_count INT,
    avg_position_return DOUBLE,
    avg_holding_days DOUBLE,
    source_breakdown JSON,
    daily_returns JSON,
    by_side JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (portfolio_id) REFERENCES portfolios(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS stress_test_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    portfolio_id BIGINT NOT NULL,
    scenario VARCHAR(100) NOT NULL,
    market_move DOUBLE,
    portfolio_pnl DOUBLE,
    portfolio_return DOUBLE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (portfolio_id) REFERENCES portfolios(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS sensitivity_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    portfolio_id BIGINT NOT NULL,
    min_confidence DOUBLE,
    min_objective_ratio DOUBLE,
    selected_positions INT,
    long_positions INT,
    short_positions INT,
    gross_exposure DOUBLE,
    expected_return_60d DOUBLE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (portfolio_id) REFERENCES portfolios(id) ON DELETE CASCADE
);
