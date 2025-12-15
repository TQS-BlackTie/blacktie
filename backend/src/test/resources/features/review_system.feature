@review
Feature: Review System
  As a user
  I want to leave and receive reviews after completed bookings
  So that I can build trust on the platform

  Background:
    Given an owner with email "owner@blacktie.com"
    And a renter with email "renter@blacktie.com"
    And a product "Classic Tuxedo" owned by "owner@blacktie.com" priced at 50.0
    And a completed booking for "Classic Tuxedo" by "renter@blacktie.com"

  Scenario: Renter reviews the owner after completed booking
    When "renter@blacktie.com" reviews the booking with rating 5 and comment "Excellent suit!"
    Then the review should be created with type "OWNER"
    And the owner's average rating should be 5.0

  Scenario: Owner reviews the renter after completed booking
    When "owner@blacktie.com" reviews the booking with rating 4 and comment "Good renter"
    Then the review should be created with type "RENTER"
    And the renter's average rating should be 4.0

  Scenario: Both parties can review the same booking
    When "renter@blacktie.com" reviews the booking with rating 5 and comment "Great!"
    And "owner@blacktie.com" reviews the booking with rating 4 and comment "Nice!"
    Then the booking should have 2 reviews

  Scenario: Cannot review the same booking twice
    Given "renter@blacktie.com" has already reviewed the booking
    When "renter@blacktie.com" tries to review the booking again
    Then the review should fail with message "already reviewed"

  Scenario: Cannot review a pending booking
    Given a booking exists for "Classic Tuxedo" by "renter@blacktie.com" with status "PENDING_APPROVAL"
    When "renter@blacktie.com" tries to review the pending booking
    Then the review should fail with message "Only completed bookings can be reviewed"

  Scenario: Get reviews for a product
    Given "renter@blacktie.com" has reviewed the booking with rating 5
    When I get reviews for product "Classic Tuxedo"
    Then I should see 1 review
    And the review rating should be 5
