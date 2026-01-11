Feature: Parking Events - Entry, Parked, and Exit
  As a parking garage system
  I want to handle vehicle entry, parking, and exit events
  So that I can manage parking operations

  Background:
    Given the garage is initialized with default garage
    And the garage has sector "A" with base price 10.00 and max capacity 100
    And the garage has sector "B" with base price 15.00 and max capacity 50
    And the garage has spots in sector "A" with coordinates

  Scenario: Vehicle enters and parks successfully
    When I send ENTRY event for vehicle "ABC1234" at "2025-01-01T10:00:00.000Z"
    Then the response status should be 200
    And a parking session should be created for vehicle "ABC1234"
    And the parking session should have sector "A"
    And the parking session should have base price calculated with dynamic pricing
    And the sector "A" occupied count should be incremented by 1
    And the parking session spot_id should be null

    When I send PARKED event for vehicle "ABC1234" with lat -23.561684 and lng -46.655981
    Then the response status should be 200
    And the parking session should have spot_id assigned
    And the spot should be marked as occupied

    When I send EXIT event for vehicle "ABC1234" at "2025-01-01T11:00:00.000Z"
    Then the response status should be 200
    And the parking session should have exit_time set
    And the parking session should have final_price calculated
    And the spot should be marked as available
    And the sector "A" occupied count should be decremented by 1
    And the revenue should be recorded

  Scenario: Vehicle enters but parks in different sector
    When I send ENTRY event for vehicle "XYZ5678" at "2025-01-01T10:00:00.000Z"
    And I send PARKED event for vehicle "XYZ5678" with lat from sector "B" coordinates
    Then the parking session should have spot_id assigned from sector "B"
    And the matched spot should belong to sector "B"

  Scenario: Vehicle enters but spot not found (Spot Not Found)
    When I send ENTRY event for vehicle "NOTFOUND" at "2025-01-01T10:00:00.000Z"
    Then the sector "A" occupied count should be incremented by 1 (capacity reserved on ENTRY)
    When I send PARKED event for vehicle "NOTFOUND" with lat 999.000000 and lng 999.000000
    Then the response status should be 200
    And the parking session spot_id should remain null
    And a warning should be logged about spot not found
    And the sector "A" occupied count should remain 1 (already counted on ENTRY)
    When I send EXIT event for vehicle "NOTFOUND" at "2025-01-01T11:00:00.000Z"
    Then the response status should be 200
    And the parking session should have final_price calculated
    And the sector "A" occupied count should be decremented by 1

  Scenario: Duplicate PARKED event (Already Parked)
    When I send ENTRY event for vehicle "DUP123" at "2025-01-01T10:00:00.000Z"
    Then the sector "A" occupied count should be 1
    When I send PARKED event for vehicle "DUP123" with valid coordinates
    Then the parking session spot_id should be assigned
    And the sector "A" occupied count should remain 1 (already counted on ENTRY)
    When I send PARKED event for vehicle "DUP123" again with different coordinates
    Then the response status should be 200 (idempotent)
    And the parking session spot_id should remain from first PARKED event
    And the sector "A" occupied count should remain 1 (not incremented again)

  Scenario: Vehicle exits without PARKED event (spot_id is null)
    When I send ENTRY event for vehicle "NO-PARK" at "2025-01-01T10:00:00.000Z"
    Then the sector "A" occupied count should be 1
    When I send EXIT event for vehicle "NO-PARK" at "2025-01-01T11:00:00.000Z" without PARKED event
    Then the response status should be 200
    And the parking session should have final_price calculated (based on entry time only)
    And the sector "A" occupied count should be decremented to 0
    And no spot should be freed (spot_id was null)

  Scenario: Multiple vehicles enter same sector
    When I send ENTRY event for vehicle "V1" at "2025-01-01T10:00:00.000Z"
    And I send ENTRY event for vehicle "V2" at "2025-01-01T10:05:00.000Z"
    And I send ENTRY event for vehicle "V3" at "2025-01-01T10:10:00.000Z"
    Then the sector "A" occupied count should be 3
    And each vehicle should have separate parking session
    And vehicle "V1" should have parking session
    And vehicle "V2" should have parking session
    And vehicle "V3" should have parking session

  Scenario: Entry blocked when sector is full
    Given sector "A" has 100 occupied spots (100% capacity)
    When I send ENTRY event for vehicle "FULL123" at "2025-01-01T10:00:00.000Z"
    Then the response status should be 409 or 400
    And the error message should indicate sector is full
    And the sector "A" occupied count should remain 100
