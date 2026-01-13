-- Create parking_spot table
CREATE TABLE IF NOT EXISTS parking_spot (
    id BINARY(16) PRIMARY KEY,
    sector_id BINARY(16) NOT NULL,
    latitude DECIMAL(10,8) NOT NULL,
    longitude DECIMAL(11,8) NOT NULL,
    is_occupied BOOLEAN NOT NULL,
    version INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY fk_spot_sector (sector_id) REFERENCES sector(id) ON DELETE CASCADE,
    INDEX idx_sector_occupied (sector_id, is_occupied),
    INDEX idx_sector_coordinates (sector_id, latitude, longitude),
    INDEX idx_coordinates (latitude, longitude)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
