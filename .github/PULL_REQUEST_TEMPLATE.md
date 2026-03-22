## Purpose

Briefly describe the purpose of this pull request. What feature does it add or what issue does it address within the
Spring Boot backend application?

## List of Changes

Explain your changes in detail:

- Implemented a new service layer for managing database transactions.
- Refactored the controller to handle new API endpoints for user authentication.

## Linked Issues

Mention any related issues here (if applicable):

- Fixes #123
- Related to #456

## How to Test

Provide instructions on how to test the new changes:

1. Update your local MySQL database schema to match the latest changes in `schema.sql`.
2. Run the Spring Boot application with `mvn spring-boot:run`.
3. Use Postman to send a GET request to `localhost:8093/ebikes-assignments/`.

## Additional Information

Mention any additional information or context that would help in the review of this PR, such as database migrations or
new environment variables.