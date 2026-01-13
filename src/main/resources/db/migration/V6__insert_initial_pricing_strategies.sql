-- Insert initial pricing strategies
-- Note: Using fixed UUIDs for initial data. In production, consider generating UUIDs programmatically
INSERT INTO pricing_strategy (id, occupancy_min_percentage, occupancy_max_percentage, multiplier, description,
                              is_active)
VALUES (UUID_TO_BIN(UUID()), 0.00, 24.99, 0.90, 'Low occupancy discount (-10%)', TRUE),
       (UUID_TO_BIN(UUID()), 25.00, 49.99, 1.00, 'Normal occupancy pricing (0%)', TRUE),
       (UUID_TO_BIN(UUID()), 50.00, 74.99, 1.10, 'High occupancy increase (+10%)', TRUE),
       (UUID_TO_BIN(UUID()), 75.00, 100.00, 1.25, 'Full occupancy increase (+25%)', TRUE);
