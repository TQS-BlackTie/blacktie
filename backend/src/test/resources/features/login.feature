Feature: User can view login page

  Scenario: Login page visibility
    Given the frontend is available
    When I open the login page
    Then I should see the login form
