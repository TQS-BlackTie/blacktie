Feature: Booking flow (smoke)

  Scenario: Open bookings page
    Given the frontend is available
    When I open the bookings page
    Then I should see the bookings list or prompt to login
