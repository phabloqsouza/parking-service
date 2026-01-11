Feature: Capacity Management - Sector Closure at 100%
  As a parking garage system
  I want to manage sector capacity
  So that I can prevent overbooking

  Background:
    Given the garage is initialized with default garage
    And the garage has sector "A" with max capacity 2 (for testing)
    And the sector "A" has 2 parking spots

  Scenario: Entry allowed when sector not full
    Given sector "A" has 1 occupied spot (50% capacity)
    When I send ENTRY event for vehicle "OK123" at "2025-01-01T10:00:00.000Z"
    Then the response status should be 200
    And the sector "A" occupied count should be 2
    And the entry should be successful

  Scenario: Entry blocked when sector is full (100% capacity)
    Given sector "A" has 2 occupied spots (100% capacity)
    When I send ENTRY event for vehicle "FULL123" at "2025-01-01T10:00:00.000Z"
    Then the response status should be 409 or 400
    And the error should indicate sector is full
    And the sector "A" occupied count should remain 2
    And no parking session should be created for vehicle "FULL123"

  Scenario: Entry allowed after exit frees capacity
    Given sector "A" has 2 occupied spots (100% capacity)
    And vehicle "V1" has active session in sector "A"
    When I send EXIT event for vehicle "V1" at "2025-01-01T11:00:00.000Z"
    Then the sector "A" occupied count should be 1
    When I send ENTRY event for vehicle "NEW123" at "2025-01-01T11:05:00.000Z"
    Then the response status should be 200
    And the sector "A" occupied count should be 2
    And vehicle "NEW123" should have parking session created

  Scenario: PARKED event counts in capacity (Spot Not Found case)
    When I send ENTRY event for vehicle "ENTRY-ONLY" at "2025-01-01T10:00:00.000Z"
    Then the sector "A" occupied count should be 1 (capacity reserved on ENTRY)
    When I send PARKED event for vehicle "ENTRY-ONLY" with invalid coordinates (spot not found)
    Then the response status should be 200
    And the sector "A" occupied count should remain 1 (already counted on ENTRY, not incremented again)
    And the vehicle should still count toward total capacity
    And the parking session spot_id should be null

  Scenario: PARKED event counts in capacity (Already Parked case)
    When I send ENTRY event for vehicle "DUP-ENTRY" at "2025-01-01T10:00:00.000Z"
    Then the sector "A" occupied count should be 1 (capacity reserved on ENTRY)
    When I send PARKED event for vehicle "DUP-ENTRY" with valid coordinates
    Then the sector "A" occupied count should remain 1 (already counted on ENTRY, not incremented again)
    When I send PARKED event for vehicle "DUP-ENTRY" again (duplicate PARKED event)
    Then the sector "A" occupied count should remain 1 (not incremented on duplicate PARKED)
    And the capacity should reflect only ENTRY events, not PARKED events

  Scenario: Exit decrements capacity
    Given sector "A" has 2 occupied spots
    And vehicle "EXIT123" has active session in sector "A"
    When I send EXIT event for vehicle "EXIT123" at "2025-01-01T11:00:00.000Z"
    Then the sector "A" occupied count should be 1 (decremented)
    And the vehicle should be removed from active sessions
