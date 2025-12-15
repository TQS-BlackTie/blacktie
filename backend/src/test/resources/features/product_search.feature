Feature: Product search and listing

  Scenario: Browse the homepage and see product listings
    Given the frontend is available
    When I open the homepage
    Then I should see a list of products
