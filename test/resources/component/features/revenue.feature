Feature: Revenue Query - Get Revenue by Date and Sector
  As a parking garage system
  I want to query revenue by date and sector
  So that I can track financial performance

  Background:
    Given the garage is initialized with default garage
    And the garage has sector "A" with base price 10.00
    And the garage has sector "B" with base price 15.00

  Scenario: Get revenue for a sector on a specific date with completed sessions
    Given vehicle "V1" entered sector "A" at "2025-01-01T10:00:00.000Z" and exited at "2025-01-01T11:00:00.000Z" with final price 10.00
    And vehicle "V2" entered sector "A" at "2025-01-01T12:00:00.000Z" and exited at "2025-01-01T13:30:00.000Z" with final price 20.00
    And vehicle "V3" entered sector "B" at "2025-01-01T14:00:00.000Z" and exited at "2025-01-01T15:00:00.000Z" with final price 15.00
    When I send POST request to "/revenue" with body:
      """
      {
        "date": "2025-01-01",
        "sector": "A"
      }
      """
    Then the response status should be 200
    And the response should contain amount 30.00
    And the response should contain currency "BRL"
    And the response should contain timestamp in ISO 8601 format
    And only completed sessions (with exit_time and final_price) should be included in revenue

  Scenario: Get revenue for a sector with no completed sessions
    Given no vehicles exited from sector "A" on "2025-01-01"
    When I send POST request to "/revenue" with body:
      """
      {
        "date": "2025-01-01",
        "sector": "A"
      }
      """
    Then the response status should be 200
    And the amount should be 0.00
    And the currency should be "BRL"

  Scenario: Get revenue excluding active sessions (not yet exited)
    Given vehicle "ACTIVE" entered sector "A" at "2025-01-01T10:00:00.000Z" and has not exited
    And vehicle "COMPLETE" entered sector "A" at "2025-01-01T11:00:00.000Z" and exited at "2025-01-01T12:00:00.000Z" with final price 10.00
    When I send POST request to "/revenue" with body:
      """
      {
        "date": "2025-01-01",
        "sector": "A"
      }
      """
    Then the amount should be 10.00 (only completed session)
    And the active session should not be included in revenue
    And the active session should not have final_price set

  Scenario: Get revenue with BigDecimal precision validation
    Given vehicle "PREC1" entered sector "A" at "2025-01-01T10:00:00.000Z" and exited at "2025-01-01T11:00:00.000Z" with final price 10.555
    When I send POST request to "/revenue" with body:
      """
      {
        "date": "2025-01-01",
        "sector": "A"
      }
      """
    Then the amount should be 10.56 (rounded to 2 decimal places for currency precision)
    And the amount should be a BigDecimal with scale 2

  Scenario: Get revenue for different dates
    Given vehicle "DATE1" entered sector "A" at "2025-01-01T10:00:00.000Z" and exited at "2025-01-01T11:00:00.000Z" with final price 10.00
    And vehicle "DATE2" entered sector "A" at "2025-01-02T10:00:00.000Z" and exited at "2025-01-02T11:00:00.000Z" with final price 15.00
    When I send POST request to "/revenue" with body:
      """
      {
        "date": "2025-01-01",
        "sector": "A"
      }
      """
    Then the amount should be 10.00 (only sessions from 2025-01-01)
    When I send POST request to "/revenue" with body:
      """
      {
        "date": "2025-01-02",
        "sector": "A"
      }
      """
    Then the amount should be 15.00 (only sessions from 2025-01-02)
