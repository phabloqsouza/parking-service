-- Refactor entity relationships
-- This migration adds garage_id to pricing_strategy, removes garage_id and sector_id from parking_session

-- Step 1: Add garage_id column to pricing_strategy table
ALTER TABLE pricing_strategy 
ADD COLUMN garage_id CHAR(36) NULL;

-- Step 2: Assign existing pricing strategies to the default garage
-- Get the default garage ID and assign all existing pricing strategies to it
UPDATE pricing_strategy ps
SET garage_id = (
    SELECT id FROM garage WHERE is_default = TRUE LIMIT 1
)
WHERE garage_id IS NULL;

-- Step 3: Make garage_id NOT NULL and add foreign key constraint
ALTER TABLE pricing_strategy 
MODIFY COLUMN garage_id CHAR(36) NOT NULL,
ADD CONSTRAINT fk_pricing_strategy_garage 
    FOREIGN KEY (garage_id) REFERENCES garage(id) ON DELETE CASCADE;

-- Step 4: Update unique constraint to include garage_id
ALTER TABLE pricing_strategy 
DROP INDEX uk_pricing_strategy_range;

ALTER TABLE pricing_strategy 
ADD CONSTRAINT uk_pricing_strategy_garage_range 
    UNIQUE (garage_id, occupancy_min_percentage, occupancy_max_percentage);

-- Step 5: Update index to include garage_id
ALTER TABLE pricing_strategy 
DROP INDEX idx_active_occupancy;

ALTER TABLE pricing_strategy 
ADD INDEX idx_active_occupancy (garage_id, is_active, occupancy_min_percentage, occupancy_max_percentage);

-- Step 6: Drop old indexes from parking_session that reference garage_id and sector_id
ALTER TABLE parking_session 
DROP INDEX idx_garage_vehicle_exit,
DROP INDEX idx_garage_sector_entry,
DROP INDEX idx_sector_exit,
DROP INDEX idx_garage_entry_time;

-- Step 7: Add new indexes for spot_id relationships
ALTER TABLE parking_session 
ADD INDEX idx_spot_vehicle_exit (spot_id, vehicle_license_plate, exit_time),
ADD INDEX idx_spot_entry_time (spot_id, entry_time);

-- Step 8: Drop foreign key constraint for garage_id (keep sector_id as it's now a relationship)
ALTER TABLE parking_session 
DROP FOREIGN KEY fk_session_garage;

-- Step 9: Drop garage_id column from parking_session (keep sector_id as foreign key for relationship)
ALTER TABLE parking_session 
DROP COLUMN garage_id;

-- Note: sector_id column is kept as it's now used for @ManyToOne Sector relationship
