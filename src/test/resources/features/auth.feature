Feature: AuthController authentication API
  As an API client
  I want to register and authenticate through the auth controller
  So that I can create a session and access protected endpoints

  Background:
    Given the API base path is "/api/auth"

  Rule: Account registration respects uniqueness and password validation rules
    # Sign-up uses @Valid on LoginInfo.
    # MethodArgumentNotValidException is translated by GlobalExceptionHandler into 400 Bad Request.
    Scenario Outline: Sign-up creates a new user
      Given the following sign-up payload:
        | username | <username> |
        | password | <password> |
      When the client POSTs the payload to "/sign-up"
      Then the response status should be 201
      And the response JSON should contain the field "username" with value "<username>"

      Examples:
        | username  | password    |
        | newuser1  | password123 |
        | alice2026 | s3cretPass  |

    Scenario: Sign-up rejects an existing username
      Given a user already exists with username "existingUser"
      And the following sign-up payload:
        | username | existingUser |
        | password | anotherPass  |
      When the client POSTs the payload to "/sign-up"
      Then the response status should be 409
      But the response JSON should contain the field "message" with value "User already exist"

    Scenario Outline: Sign-up rejects an invalid payload through the global validation handler
      Given the following sign-up payload:
        | username | <username> |
        | password | <password> |
      When the client POSTs the payload to "/sign-up"
      Then the response status should be 400
      And the response JSON should contain the field "message"

      Examples:
        | username  | password  |
        | tiny      | password1 |
        |           | password1 |
        | validname |           |
        | validname | p123      |

  Rule: Session creation reflects credential validity
    # Sign-in does not use @Valid on LoginInfo.
    # Unknown or mismatched credentials are converted by AuthController's local exception handler into 401 Unauthorized.
    Scenario: Sign-in returns the username and creates a session for valid credentials
      Given an existing user with username "existingUser" and password "password123"
      And the following sign-in payload:
        | username | existingUser |
        | password | password123  |
      When the client POSTs the payload to "/sign-in"
      Then the response status should be 201
      And the response JSON should contain the field "username" with value "existingUser"
      And the response should include a Set-Cookie header containing "SESSION"

    Scenario Outline: Sign-in rejects credentials that cannot be resolved to a valid user
      Given an existing user with username "existingUser" and password "password123"
      And the following sign-in payload:
        | username | <username> |
        | password | <password> |
      When the client POSTs the payload to "/sign-in"
      Then the response status should be 401
      And the response JSON should contain the field "message" with value "User not found"

      Examples:
        | username     | password |
        | ghostUser    | whatever |
        | existingUser | badpass  |
