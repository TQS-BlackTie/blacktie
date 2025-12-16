@product
Feature: Product Management
  As an owner
  I want to manage my formal wear listings
  So that renters can find and book my items

  Background:
    Given an owner with email "owner@blacktie.com"
    And a renter with email "renter@blacktie.com"

  Scenario: Owner creates a new product
    When "owner@blacktie.com" creates a product "Navy Blue Suit" with description "Elegant navy suit" and price 75.0
    Then the product "Navy Blue Suit" should exist
    And the product should be available for booking

  Scenario: Renter cannot create products
    When "renter@blacktie.com" tries to create a product "Fake Product" with price 50.0
    Then the product creation should fail with message "Only owners can create products"

  Scenario: Search products by name
    Given a product "Black Tuxedo" owned by "owner@blacktie.com" priced at 100.0
    And a product "White Dinner Jacket" owned by "owner@blacktie.com" priced at 80.0
    When I search for products with name "Tuxedo"
    Then I should find 1 product
    And the product name should be "Black Tuxedo"

  Scenario: Filter products by max price
    Given a product "Budget Suit" owned by "owner@blacktie.com" priced at 30.0
    And a product "Premium Tuxedo" owned by "owner@blacktie.com" priced at 200.0
    When I search for products with max price 50.0
    Then I should find 1 product
    And the product name should be "Budget Suit"

  Scenario: Owner deletes their product
    Given a product "Delete Me Suit" owned by "owner@blacktie.com" priced at 60.0
    When "owner@blacktie.com" deletes the product "Delete Me Suit"
    Then the product should be marked as unavailable

  Scenario: User cannot delete another owner's product
    Given another owner with email "other.owner@blacktie.com"
    And a product "Not Yours Suit" owned by "owner@blacktie.com" priced at 70.0
    When "other.owner@blacktie.com" tries to delete the product "Not Yours Suit"
    Then a forbidden error should be returned

  Scenario: Owner sees only their own products
    Given another owner with email "other.owner@blacktie.com"
    And a product "My Suit" owned by "owner@blacktie.com" priced at 50.0
    And a product "Their Suit" owned by "other.owner@blacktie.com" priced at 60.0
    When "owner@blacktie.com" views their products
    Then they should see 1 product
    And the product name should be "My Suit"

  Scenario: Renter sees all available products
    Given another owner with email "other.owner@blacktie.com"
    And a product "First Suit" owned by "owner@blacktie.com" priced at 50.0
    And a product "Second Suit" owned by "other.owner@blacktie.com" priced at 60.0
    When "renter@blacktie.com" browses products
    Then they should see 2 products
