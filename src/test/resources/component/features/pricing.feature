Feature: Pricing Calculation - Dynamic Pricing and Fee Calculation
  As a parking garage system
  I want to calculate parking fees with dynamic pricing
  So that pricing adjusts based on occupancy

  Background:
    Given the garage is initialized with default garage
    And the garage has sector "A" with base price 10.00 and max capacity 100
    And the pricing strategies are configured in database:
      | occupancy_min | occupancy_max | multiplier |
      | 0.00          | 24.99         | 0.90       |
      | 25.00         | 49.99         | 1.00       |
      | 50.00         | 74.99         | 1.10       |
      | 75.00         | 100.00        | 1.25       |

  Scenario: Low occupancy discount (< 25%)
    Given sector "A" has 20 occupied spots (20% occupancy)
    When I send ENTRY event for vehicle "LOW123" at "2025-01-01T10:00:00.000Z"
    Then the parking session base_price should be 9.00 (10.00 * 0.90)
    And the parking session base_price should have precision of 2 decimal places
    And the dynamic pricing multiplier should be 0.90

  Scenario: Normal occupancy pricing (25-50%)
    Given sector "A" has 35 occupied spots (35% occupancy)
    When I send ENTRY event for vehicle "NORMAL123" at "2025-01-01T10:00:00.000Z"
    Then the parking session base_price should be 10.00 (10.00 * 1.00)
    And the dynamic pricing multiplier should be 1.00

  Scenario: High occupancy increase (50-75%)
    Given sector "A" has 65 occupied spots (65% occupancy)
    When I send ENTRY event for vehicle "HIGH123" at "2025-01-01T10:00:00.000Z"
    Then the parking session base_price should be 11.00 (10.00 * 1.10)
    And the dynamic pricing multiplier should be 1.10

  Scenario: Full occupancy increase (75-100%)
    Given sector "A" has 85 occupied spots (85% occupancy)
    When I send ENTRY event for vehicle "FULL123" at "2025-01-01T10:00:00.000Z"
    Then the parking session base_price should be 12.50 (10.00 * 1.25)
    And the dynamic pricing multiplier should be 1.25

  Scenario: First 30 minutes are free
    Given vehicle "FREE30" entered at "2025-01-01T10:00:00.000Z" with base price 10.00
    When I send EXIT event for vehicle "FREE30" at "2025-01-01T10:29:00.000Z" (29 minutes)
    Then the parking session final_price should be 0.00
    And the final_price should have precision of 2 decimal places

  Scenario: Exactly 30 minutes is free
    Given vehicle "EXACT30" entered at "2025-01-01T10:00:00.000Z" with base price 10.00
    When I send EXIT event for vehicle "EXACT30" at "2025-01-01T10:30:00.000Z" (exactly 30 minutes)
    Then the parking session final_price should be 0.00

  Scenario: 31 minutes charges 1 hour (round up)
    Given vehicle "MIN31" entered at "2025-01-01T10:00:00.000Z" with base price 10.00
    When I send EXIT event for vehicle "MIN31" at "2025-01-01T10:31:00.000Z" (31 minutes)
    Then the parking session final_price should be 10.00 (1 hour * 10.00, rounded up with CEILING)

  Scenario: 91 minutes charges 2 hours (round up)
    Given vehicle "MIN91" entered at "2025-01-01T10:00:00.000Z" with base price 10.00
    When I send EXIT event for vehicle "MIN91" at "2025-01-01T11:31:00.000Z" (91 minutes total)
    Then the parking session final_price should be 20.00 (2 hours * 10.00, rounded up - 30 min free, 61 min remaining = 2 hours)

  Scenario: Fee calculation with dynamic pricing applied
    Given sector "A" has 70 occupied spots (70% occupancy) with base price 10.00
    When I send ENTRY event for vehicle "DYNAMIC123" at "2025-01-01T10:00:00.000Z"
    Then the parking session base_price should be 11.00 (10.00 * 1.10, high occupancy multiplier)
    When I send EXIT event for vehicle "DYNAMIC123" at "2025-01-01T11:35:00.000Z" (95 minutes total)
    Then the parking session final_price should be 22.00 (2 hours * 11.00, rounded up)
    And the final_price should use base_price with dynamic pricing multiplier applied at entry

  Scenario: Pricing precision validation (BigDecimal scale 2)
    Given vehicle "PREC123" entered at "2025-01-01T10:00:00.000Z" with base price 10.555
    When I send EXIT event for vehicle "PREC123" at "2025-01-01T11:00:00.000Z"
    Then the parking session base_price should be 10.56 (rounded to 2 decimal places)
    And the parking session final_price should have scale 2

  Scenario: Multiple hours calculation with rounding
    Given vehicle "HOURS123" entered at "2025-01-01T10:00:00.000Z" with base price 10.00
    When I send EXIT event for vehicle "HOURS123" at "2025-01-01T12:01:00.000Z" (2 hours 1 minute)
    Then the parking session final_price should be 20.00 (2 hours * 10.00 - first 30 min free, remaining 91 min = 2 hours rounded up)
