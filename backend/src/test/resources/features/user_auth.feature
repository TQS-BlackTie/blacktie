@auth
Feature: User Authentication
  As a user
  I want to register and login to the platform
  So that I can rent or list formal wear

  Scenario: Register a new renter account
    Given no user exists with email "newrenter@blacktie.com"
    When I register with name "John Renter" and email "newrenter@blacktie.com" and password "SecurePass123"
    Then the user "newrenter@blacktie.com" should exist in the system
    And the user role should be "renter"

  Scenario: Cannot register with existing email
    Given a user exists with email "existing@blacktie.com"
    When I try to register with email "existing@blacktie.com"
    Then the registration should fail with message "Email already exists"

  Scenario: Login with valid credentials
    Given a user exists with email "valid@blacktie.com" and password "ValidPass123"
    When I login with email "valid@blacktie.com" and password "ValidPass123"
    Then I should be authenticated successfully

  Scenario: Login with wrong password fails
    Given a user exists with email "wrongpass@blacktie.com" and password "CorrectPass123"
    When I login with email "wrongpass@blacktie.com" and password "WrongPassword"
    Then the login should fail with message "Invalid email or password"

  Scenario: Login with non-existent email fails
    Given no user exists with email "noexist@blacktie.com"
    When I login with email "noexist@blacktie.com" and password "SomePassword"
    Then the login should fail with message "Invalid email or password"

  Scenario: Change user role from renter to owner
    Given a user exists with email "rolechange@blacktie.com" and role "renter"
    When I change the role of "rolechange@blacktie.com" to "owner"
    Then the user role should be "owner"
