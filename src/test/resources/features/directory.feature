Feature: DirectoryController directory API
  As an authenticated client
  I want to browse and create directories
  So that I can manage my storage structure

  Background:
    Given the API base path is "/api/directory"

  Scenario: Authenticated client lists folder contents
    Given the client is authenticated as "john"
    And the file service will return the following folder entries:
      | path  | name      | size | type      |
      | docs/ | notes.txt | 128  | File      |
      | docs/ | images/   | 0    | Directory |
    When the client GETs "" with query parameters:
      | path | docs/ |
    Then the response status should be 200
    And the response JSON array should have size 2
    And the response JSON at index 0 should contain the field "name" with value "notes.txt"
    And the response JSON at index 1 should contain the field "type" with value "Directory"
    And the file service should have been asked to list folder "docs/" for user "john"

  Scenario: Authenticated client creates a folder
    Given the client is authenticated as "john"
    And the file service will create the folder result:
      | path | name     | size | type      |
      |      | reports/ | 0    | Directory |
    When the client POSTs to "" with query parameters:
      | path | reports/ |
    Then the response status should be 201
    And the response JSON should contain the field "name" with value "reports/"
    And the file service should have been asked to create folder "reports/" for user "john"

  Scenario: Directory creation propagates internal errors through the global handler
    Given the client is authenticated as "john"
    And the file service will fail folder creation with internal error "Cannot create folder"
    When the client POSTs to "" with query parameters:
      | path | broken/ |
    Then the response status should be 500
    And the response JSON should contain the field "message" with value "Cannot create folder"

  Scenario: Unauthenticated client cannot list directories
    Given the client is unauthenticated
    When the client GETs "" with query parameters:
      | path | docs/ |
    Then the response status should be 401
    But the response JSON should contain the field "message" with value "Unauthorized"
