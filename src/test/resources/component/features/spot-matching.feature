Feature: Spot Matching - PARKED Event Coordinate Matching
  As a parking garage system
  I want to match PARKED events to existing parking spots by coordinates
  So that I can track which specific spot a vehicle parked in

  Background:
    Given the garage is initialized with default garage
    And the garage has sector "A" with base price 10.00
    And the garage has spots in sector "A":
      | spot_id | latitude   | longitude   |
      | spot1   | -23.561684 | -46.655981  |
      | spot2   | -23.561685 | -46.655982  |
      | spot3   | -23.561686 | -46.655983  |
    And the coordinate tolerance is 0.000001 degrees (approximately 0.1 meters)

  Scenario: Spot found by coordinates within tolerance (exact match)
    Given vehicle "MATCH123" entered at "2025-01-01T10:00:00.000Z"
    When I send PARKED event for vehicle "MATCH123" with lat -23.561684 and lng -46.655981
    Then the response status should be 200
    And the parking session spot_id should be assigned to spot "spot1"
    And the spot "spot1" should be marked as occupied

  Scenario: Spot found by coordinates with GPS drift (within tolerance)
    Given vehicle "DRIFT123" entered at "2025-01-01T10:00:00.000Z"
    When I send PARKED event for vehicle "DRIFT123" with lat -23.5616845 and lng -46.6559815 (within 0.000001 tolerance)
    Then the response status should be 200
    And the parking session spot_id should be assigned (matched within tolerance)
    And the matched spot should be within tolerance range

  Scenario: Spot not found - coordinates outside tolerance
    Given vehicle "NOTFOUND123" entered at "2025-01-01T10:00:00.000Z"
    Then the sector "A" occupied count should be 1 (capacity reserved on ENTRY)
    When I send PARKED event for vehicle "NOTFOUND123" with lat -23.000000 and lng -46.000000 (far from any spot)
    Then the response status should be 200 (gracefully handled)
    And the parking session spot_id should remain null
    And a warning should be logged about spot not found
    And the sector "A" occupied count should remain 1 (already counted on ENTRY)
    And the EXIT event should still be processable

  Scenario: Multiple spots match (Ambiguous Match)
    Given vehicle "AMBIG123" entered at "2025-01-01T10:00:00.000Z"
    And there are multiple spots very close together (within tolerance range) at lat -23.561684 and lng -46.655981
    When I send PARKED event for vehicle "AMBIG123" with coordinates that match multiple spots
    Then the response status should be 400 (Bad Request)
    And the error message should indicate ambiguous spot match
    And the parking session spot_id should remain null
    And no spot should be marked as occupied

  Scenario: Spot already occupied (concurrency issue)
    Given spot "spot1" is already occupied by vehicle "OCCUPIED123"
    And vehicle "NEW123" entered at "2025-01-01T10:00:00.000Z"
    When I send PARKED event for vehicle "NEW123" with coordinates matching spot "spot1"
    Then the response status should be 409 (Conflict) or 400 (Bad Request)
    And the error should indicate spot is already occupied
    And the parking session spot_id should remain null
    And the spot "spot1" should remain occupied by vehicle "OCCUPIED123"

  Scenario: Spot matching validates sector consistency
    Given vehicle "SECTOR123" entered sector "A" at "2025-01-01T10:00:00.000Z"
    And the garage has sector "B" with a spot at coordinates lat -23.561684 and lng -46.655981
    When I send PARKED event for vehicle "SECTOR123" with lat -23.561684 and lng -46.655981
    Then the matched spot should belong to sector "A" (same as parking session sector)
    And the matched spot should not belong to sector "B"
    And the parking session spot_id should be assigned to a spot in sector "A"

  Scenario: Coordinate matching searches within sector
    Given vehicle "SECTOR-SEARCH" entered sector "A" at "2025-01-01T10:00:00.000Z"
    And sector "B" has spots with different coordinates
    When I send PARKED event for vehicle "SECTOR-SEARCH" with coordinates from sector "A"
    Then the matched spot should be from sector "A" only
    And the coordinate matching should search within sector "A" scope
