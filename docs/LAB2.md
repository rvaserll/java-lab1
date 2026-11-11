# Lab 2 — testing the intergalactic market

## Run locally

Use `./gradlew clean build` (Windows: `gradlew.bat clean build`). The wrapper downloads
Gradle and dependencies on first use. The build runs tests and enforces at least
80% line and branch coverage for each service class. A coverage failure fails the build.

Reports:

- Tests: `build/reports/tests/test/index.html`
- Coverage: `build/reports/coverage/index.html`
- Machine-readable coverage: `build/reports/coverage/coverage.xml`

The HTML coverage report contains the entire application, including untested models.
The enforced threshold applies to the service layer, as required by the assignment;
application-wide coverage is not the same as service coverage.

## Implemented checks

- `ProductServiceTest`: 17 cases for CRUD, missing products/categories, pagination,
  size boundaries, large page offsets, stable ordering and preservation of data on failure.
- `ProductControllerTest`: 53 cases using standalone MockMvc with the real service,
  Bean Validation and exception handler. Includes POST/PUT validation, valid boundaries,
  malformed input, HTTP status codes, Location, pagination, 404 and safe 500 responses.
  Mockito is used to reproduce an unexpected service failure without leaking its details.
- `ProductApiIntegrationTest`: one complete product lifecycle using the actual Spring
  Boot context, including validation, persistence across requests, update and deletion.
- Existing `JavaLab1ApplicationTests`: application context startup.

Local verification on 2026-11-11: 72 tests, no failures or errors;
ProductService: 46/46 lines and 14/14 branches covered (100% each).
Whole application: 142/189 lines covered (approximately 75%).
Local runtime: Oracle Java 25; application compilation targets Java 17.
The GitHub workflow separately uses Temurin Java 17. A successful local run does not
confirm the GitHub runner result.

## Configuration decisions

The existing Maven-to-Gradle migration was preserved and completed. Testing files are
adapted from Ekatereana/intergalactic-market-java-course PR #1 (head 1644d84).
The workflow explicitly runs `clean build`, uses supported artifact-upload actions,
and uploads both test and coverage reports, including on failure.
JaCoCo 0.8.16 is used with the locally installed Java 25 runtime.

This project uses one default test task, including its Spring integration test.
Separate integration-test tasks are unnecessary here; no instructor task graph was
copied blindly. No application classes or public API behavior were changed.

The provided peer PR was consulted for comparison, not copied into this project:
https://github.com/Volodymyr-Buryak/cosmo-cats-market/pull/3

## Submission and remaining work

Base branch: `lab1-fixes`, not `main`, while Lab 1 is still awaiting approval.
Do not merge Lab 1 without the instructor's approval.

- Confirm the remote GitHub Actions run is green after publishing the branch/PR.
- Obtain the instructor's chaos-test scenarios. These tests are not present in the
  supplied assignment or project. Do not claim they have passed. For each failure,
  record the reproduction, diagnosis, fix and regression test.
- WireMock bonus is not implemented: the current product service has no external
  HTTP dependency. Agree on a meaningful scenario before extending application scope.
- Add personal PR comments describing actual decisions and AI corrections. The AI
  implemented this suite after the user's instruction to proceed. There were no
  subsequent user corrections during this implementation; do not invent them.

Suggested places for personal review comments: service coverage scope, pagination
boundaries, POST/PUT validation parity, standalone MockMvc versus full-context checks,
and adaptations to the instructor's Gradle/workflow examples.
