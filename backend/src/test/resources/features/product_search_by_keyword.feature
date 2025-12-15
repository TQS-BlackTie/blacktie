Feature: Product search by keyword

  Scenario: Search for a product keyword and see results
    Given the frontend is available
    When I open the homepage
    And I search for "tuxedo"
    Then I should see search results
