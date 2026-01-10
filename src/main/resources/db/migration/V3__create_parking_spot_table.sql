-- Create parking_spot table
CREATE TABLE IF NOT EXISTS parking_spot (
    id CHAR(36) PRIMARY KEY,
    sector_id CHAR(36) NOT NULL,
    latitude DECIMAL(10,8) NOT NULL,
    longitude DECIMAL(11,8) NOT NULL,
    is_occupied BOOLEAN DEFAULT FALSE,
    version INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY fk_spot_sector (sector_id) REFERENCES sector(id) ON DELETE CASCADE,
    INDEX idx_spot_sector_occupied (sector_id, is_occupied),
    INDEX idx_spot_sector_coordinates (sector_id, latitude, longitude)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
