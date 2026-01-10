-- Insert initial pricing strategies
-- Note: Using fixed UUIDs for initial data. In production, consider generating UUIDs programmatically
INSERT IGNORE INTO pricing_strategy (id, occupancy_min_percentage, occupancy_max_percentage, multiplier, description, is_active) VALUES
    ('00000000-0000-0000-0000-000000000001', 0.00, 24.99, 0.90, 'Low occupancy discount (-10%)', TRUE),
    ('00000000-0000-0000-0000-000000000002', 25.00, 49.99, 1.00, 'Normal occupancy pricing (0%)', TRUE),
    ('00000000-0000-0000-0000-000000000003', 50.00, 74.99, 1.10, 'High occupancy increase (+10%)', TRUE),
    ('00000000-0000-0000-0000-000000000004', 75.00, 100.00, 1.25, 'Full occupancy increase (+25%)', TRUE);
