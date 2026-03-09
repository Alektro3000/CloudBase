Feature: UserController current user API
  As an authenticated client
  I want to fetch the current user profile
  So that I can identify the logged-in user

  Background:
    Given the API base path is "/api/user"

  Scenario: The authenticated session is represented by the current profile
    Given an authenticated session exists for "john"
    When the current profile is requested
    Then the response status should be 200
    And the response JSON should contain the field "username" with value "john"

  Scenario: Anonymous access is rejected before a profile is returned
    Given the client is unauthenticated
    When the current profile is requested
    Then the response status should be 401
    But the response JSON should contain the field "message" with value "Unauthorized"
