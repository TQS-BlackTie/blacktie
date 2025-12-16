@booking
Feature: Booking Management
  As a renter
  I want to create and manage bookings for formal wear
  So that I can rent suits for my events

  Background:
    Given an owner with email "owner@blacktie.com"
    And a renter with email "renter@blacktie.com"
    And a product "Classic Black Tuxedo" owned by "owner@blacktie.com" priced at 50.0 per day

  Scenario: Create a new booking
    When "renter@blacktie.com" creates a booking for "Classic Black Tuxedo" from tomorrow for 3 days
    Then the booking should be created with status "PENDING_APPROVAL"
    And the total price should be 150.0

  Scenario: Owner approves a booking
    Given a booking exists for "Classic Black Tuxedo" by "renter@blacktie.com" with status "PENDING_APPROVAL"
    When "owner@blacktie.com" approves the booking with delivery method "PICKUP" and location "Store #1"
    Then the booking status should be "APPROVED"
    And the delivery method should be "PICKUP"

  Scenario: Owner rejects a booking
    Given a booking exists for "Classic Black Tuxedo" by "renter@blacktie.com" with status "PENDING_APPROVAL"
    When "owner@blacktie.com" rejects the booking with reason "Item unavailable"
    Then the booking status should be "REJECTED"

  Scenario: Renter pays for approved booking
    Given a booking exists for "Classic Black Tuxedo" by "renter@blacktie.com" with status "APPROVED"
    When "renter@blacktie.com" pays for the booking
    Then the booking status should be "PAID"
    And a delivery code should be generated

  Scenario: Renter cancels pending booking
    Given a booking exists for "Classic Black Tuxedo" by "renter@blacktie.com" with status "PENDING_APPROVAL"
    When "renter@blacktie.com" cancels the booking
    Then the booking status should be "CANCELLED"

  Scenario: Owner cancels booking
    Given a booking exists for "Classic Black Tuxedo" by "renter@blacktie.com" with status "PENDING_APPROVAL"
    When "owner@blacktie.com" cancels the booking
    Then the booking status should be "CANCELLED"
    And the renter should be notified of the cancellation

  Scenario: Unauthorized user cannot cancel booking
    Given another renter with email "other@blacktie.com"
    And a booking exists for "Classic Black Tuxedo" by "renter@blacktie.com" with status "PENDING_APPROVAL"
    When "other@blacktie.com" tries to cancel the booking
    Then a forbidden error should be returned
