-- Create pricing_strategy table (configurable pricing rules from database)
CREATE TABLE IF NOT EXISTS pricing_strategy (
    id BINARY(16) PRIMARY KEY,
    occupancy_min_percentage DECIMAL(5,2) NOT NULL,
    occupancy_max_percentage DECIMAL(5,2) NOT NULL,
    multiplier DECIMAL(5,2) NOT NULL,
    description VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_pricing_strategy_range (occupancy_min_percentage, occupancy_max_percentage),
    INDEX idx_active_occupancy (is_active, occupancy_min_percentage, occupancy_max_percentage)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
