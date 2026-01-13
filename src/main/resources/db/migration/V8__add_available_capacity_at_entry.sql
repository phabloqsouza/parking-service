-- Add available_capacity_at_entry column to parking_session table
ALTER TABLE parking_session 
ADD COLUMN available_capacity_at_entry BIGINT NULL;
