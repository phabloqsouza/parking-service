-- Refactor entity relationships
-- This migration removes garage_id from parking_session (keeping sector_id as relationship)


-- Add new indexes matching entity definition
ALTER TABLE parking_session 
ADD INDEX idx_spot_exit (spot_id, exit_time),
ADD INDEX idx_spot_vehicle_exit (spot_id, vehicle_license_plate, exit_time),
ADD INDEX idx_spot_entry_time (spot_id, entry_time),
ADD INDEX idx_sector_exit (sector_id, exit_time);

