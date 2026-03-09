Feature: FileController resource API
  As an authenticated client
  I want to upload, search, move, download, and delete resources
  So that I can manage stored files

  Background:
    Given the API base path is "/api/resource"

  Scenario: Authenticated client uploads a file
    Given the client is authenticated as "john"
    And the file service will accept uploaded files
    When the client uploads a file to "":
      | filename    | hello.txt   |
      | content     | hello world |
      | contentType | text/plain  |
      | path        | docs/       |
    Then the response status should be 201
    And the response body should equal "result"
    And the file service should have been asked to upload file "hello.txt" to "docs/" for user "john"

  Scenario: Authenticated client deletes a resource
    Given the client is authenticated as "john"
    And the file service will accept deletions
    When the client DELETEs "" with query parameters:
      | path | docs/hello.txt |
    Then the response status should be 201
    And the response body should equal "result"
    And the file service should have been asked to remove path "docs/hello.txt" for user "john"

  Scenario: Authenticated client searches resources
    Given the client is authenticated as "john"
    And the file service will return the following search results:
      | path  | name       | size | type |
      | docs/ | report.txt | 42   | File |
    When the client GETs "/search" with query parameters:
      | query | report |
    Then the response status should be 200
    And the response JSON array should have size 1
    And the response JSON at index 0 should contain the field "name" with value "report.txt"
    And the file service should have been asked to search for "report" for user "john"

  Rule: Move does not move any files if it has any conflicts with destination
    Scenario: Authenticated client moves a resource
      Given the client is authenticated as "john"
      And the file service will move the resource result:
        | path  | name     | size | type |
        | docs/ | done.txt | 42   | File |
      When the client GETs "/move" with query parameters:
        | from | docs/report.txt |
        | to   | docs/done.txt   |
      Then the response status should be 200
      And the response JSON should contain the field "name" with value "done.txt"
      And the file service should have been asked to move from "docs/report.txt" to "docs/done.txt" for user "john"

    Scenario: Move propagates destination conflicts through the global handler
      Given the client is authenticated as "john"
      And the file service will fail move with destination conflict "File already exists"
      When the client GETs "/move" with query parameters:
        | from | docs/report.txt |
        | to   | docs/done.txt   |
      Then the response status should be 409
      But the response JSON should contain the field "message" with value "File already exists"

  Rule: Resource download works with files and folders
    Scenario: Authenticated client downloads a resource
      Given the client is authenticated as "john"
      And the file service will return downloadable content "hello world"
      When the client GETs "/download" with query parameters:
        | path | docs/hello.txt |
      Then the response status should be 200
      And the response header "Content-Disposition" should contain "docs/hello.txt.zip"
      And the response content type should contain "application/octet-stream"
      And the response body should equal "hello world"
      And the file service should have been asked to download path "docs/hello.txt" for user "john"

    Scenario: Download propagates missing-file errors through the global handler
      Given the client is authenticated as "john"
      And the file service will fail download with missing file "Missing"
      When the client GETs "/download" with query parameters:
        | path | docs/missing.txt |
      Then the response status should be 404
      And the response JSON should contain the field "message" with value "Missing"

  Rule: Unauthenticated client cannot upload files
    Scenario: Unauthenticated client cannot upload files
      Given the client is unauthenticated
      When the client uploads a file to "":
        | filename    | hello.txt   |
        | content     | hello world |
        | contentType | text/plain  |
        | path        | docs/       |
      Then the response status should be 401
      And the response JSON should contain the field "message" with value "Unauthorized"
