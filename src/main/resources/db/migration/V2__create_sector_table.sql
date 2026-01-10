-- Create sector table
CREATE TABLE IF NOT EXISTS sector (
    id CHAR(36) PRIMARY KEY,
    garage_id CHAR(36) NOT NULL,
    sector_code VARCHAR(10) NOT NULL,
    base_price DECIMAL(19,2) NOT NULL,
    max_capacity INTEGER NOT NULL,
    occupied_count INTEGER DEFAULT 0,
    version INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY fk_sector_garage (garage_id) REFERENCES garage(id) ON DELETE CASCADE,
    UNIQUE KEY uk_sector_garage_code (garage_id, sector_code),
    INDEX idx_sector_garage_id (garage_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
