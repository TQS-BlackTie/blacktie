Feature: Product details page

  Scenario: Open product details and see booking option
    Given the frontend is available
    When I open a product details page
    Then I should see product title and booking option
