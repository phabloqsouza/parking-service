-- Create parking_session table
CREATE TABLE IF NOT EXISTS parking_session (
    id CHAR(36) PRIMARY KEY,
    garage_id CHAR(36) NOT NULL,
    vehicle_license_plate VARCHAR(20) NOT NULL,
    spot_id CHAR(36) NULL,
    sector_id CHAR(36) NOT NULL,
    entry_time TIMESTAMP NOT NULL,
    exit_time TIMESTAMP NULL,
    base_price DECIMAL(19,2) NOT NULL,
    final_price DECIMAL(19,2) NULL,
    version INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY fk_session_garage (garage_id) REFERENCES garage(id) ON DELETE CASCADE,
    FOREIGN KEY fk_session_spot (spot_id) REFERENCES parking_spot(id) ON DELETE SET NULL,
    FOREIGN KEY fk_session_sector (sector_id) REFERENCES sector(id) ON DELETE CASCADE,
    INDEX idx_session_garage_plate_exit (garage_id, vehicle_license_plate, exit_time),
    INDEX idx_session_garage_sector_entry (garage_id, sector_id, entry_time),
    INDEX idx_session_spot_exit (spot_id, exit_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
