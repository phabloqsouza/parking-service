-- Create sector table
CREATE TABLE IF NOT EXISTS sector (
    id BINARY(16) PRIMARY KEY,
    garage_id BINARY(16) NOT NULL,
    sector_code VARCHAR(10) NOT NULL,
    base_price DECIMAL(19,2) NOT NULL,
    max_capacity INTEGER NOT NULL,
    occupied_count INTEGER NOT NULL,
    version INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY fk_sector_garage (garage_id) REFERENCES garage(id) ON DELETE CASCADE,
    UNIQUE KEY uk_sector_garage_code (garage_id, sector_code),
    INDEX idx_garage_id (garage_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
