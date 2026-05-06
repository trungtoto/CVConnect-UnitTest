# Unit Testing Report - user-service (user-tests)

Date: 2026-04-15
Scope owner: user-tests module
Project: CVConnect

## 1.1 Tools and Libraries
- Test framework: JUnit 5 (via spring-boot-starter-test)
- Mocking: Mockito (via spring-boot-starter-test)
- Assertions: AssertJ (via spring-boot-starter-test)
- Build and test runner: Maven Wrapper (mvnw)
- Coverage:
  - JaCoCo agent during test run (jacoco.exec)
  - JaCoCo CLI report generation (HTML, XML, CSV)

## 1.2 Scope of Testing

### Files / Classes that are tested
- com.cvconnect.service.impl.UserServiceImpl
- com.cvconnect.service.impl.OrgMemberServiceImpl
- com.cvconnect.utils.JwtUtils
- com.cvconnect.service.impl.JobConfigServiceImpl
- com.cvconnect.job.JobScheduler
- com.cvconnect.service.impl.FailedRollbackServiceImpl
- com.cvconnect.job.failedRollback.FailedRollbackHandlerRegistry
- com.cvconnect.job.failedRollback.FailedRollbackRetryJob

Test classes implemented in user-tests:
- com.cvconnect.userservice.user.UserServiceImplTest
- com.cvconnect.userservice.orgmember.OrgMemberServiceImplTest
- com.cvconnect.userservice.security.JwtUtilsTest
- com.cvconnect.userservice.job.JobConfigServiceImplTest
- com.cvconnect.userservice.job.JobSchedulerTest
- com.cvconnect.userservice.failedrollback.FailedRollbackServiceImplTest
- com.cvconnect.userservice.failedrollback.FailedRollbackHandlerRegistryTest
- com.cvconnect.userservice.failedrollback.FailedRollbackRetryJobTest

### Files / Classes that do not need direct unit tests (for this phase)
- dto/*:
  - Reason: mostly data carriers, no business branch logic.
- entity/*:
  - Reason: persistence mapping objects, typically validated by integration/JPA tests.
- repository/*:
  - Reason: query correctness belongs to integration tests with real DB.
- controller/*:
  - Reason: preferred via slice/integration tests (MockMvc/WebMvc/WebFlux).
- config/*:
  - Reason: wiring/security/filter behavior is better validated in integration context.
- service interfaces (com.cvconnect.service.*):
  - Reason: contracts only, no executable business logic.
- main application boot class:
  - Reason: smoke/startup check, not business unit logic.

## 1.3 Unit Test Cases
- Full test case matrix (Excel-friendly CSV):
  - UNIT_TEST_CASE_MATRIX_USER_SERVICE_2026-04-15.csv
- Total test cases: 33
- Structure per instructor format:
  - Test Case ID / Test Objective / Input / Expected Output / Notes
- Requirement mapping:
  - CheckDB covered through repository interaction verification on write/read paths.
  - Rollback covered through no-save-on-validation-fail cases and failed-rollback retry compensation flows.

## 1.4 Project Link
- Repository URL: https://github.com/trungtoto/CVConnect-Unit-Test.git
- Local module path: BE/Test_script/CVConnect-Unit-Test/user-tests

## 1.5 Execution Report

### Command used
- Set Java 17 in session and run:
  - BE/user-service/mvnw.cmd -f BE/Test_script/CVConnect-Unit-Test/user-tests/pom.xml -DskipTests=false test

### Result summary
- Tests run: 33
- Failures: 0
- Errors: 0
- Skipped: 0
- Status: PASS

### Execution evidence (for screenshot capture)
- Surefire XML reports:
  - BE/Test_script/CVConnect-Unit-Test/user-tests/target/surefire-reports/TEST-com.cvconnect.userservice.user.UserServiceImplTest.xml
  - BE/Test_script/CVConnect-Unit-Test/user-tests/target/surefire-reports/TEST-com.cvconnect.userservice.orgmember.OrgMemberServiceImplTest.xml
  - BE/Test_script/CVConnect-Unit-Test/user-tests/target/surefire-reports/TEST-com.cvconnect.userservice.security.JwtUtilsTest.xml
  - BE/Test_script/CVConnect-Unit-Test/user-tests/target/surefire-reports/TEST-com.cvconnect.userservice.job.JobSchedulerTest.xml
  - BE/Test_script/CVConnect-Unit-Test/user-tests/target/surefire-reports/TEST-com.cvconnect.userservice.job.JobConfigServiceImplTest.xml
  - BE/Test_script/CVConnect-Unit-Test/user-tests/target/surefire-reports/TEST-com.cvconnect.userservice.failedrollback.FailedRollbackRetryJobTest.xml
  - BE/Test_script/CVConnect-Unit-Test/user-tests/target/surefire-reports/TEST-com.cvconnect.userservice.failedrollback.FailedRollbackServiceImplTest.xml
  - BE/Test_script/CVConnect-Unit-Test/user-tests/target/surefire-reports/TEST-com.cvconnect.userservice.failedrollback.FailedRollbackHandlerRegistryTest.xml

## 1.6 Code Coverage Report

### Coverage generation
- JaCoCo execution file:
  - BE/Test_script/CVConnect-Unit-Test/user-tests/target/jacoco.exec
- JaCoCo report artifacts:
  - BE/Test_script/CVConnect-Unit-Test/user-tests/target/site/jacoco/index.html
  - BE/Test_script/CVConnect-Unit-Test/user-tests/target/site/jacoco/jacoco.xml
  - BE/Test_script/CVConnect-Unit-Test/user-tests/target/site/jacoco/jacoco.csv

### Coverage summary (overall, from jacoco.xml)
- Instruction: 1214 / 7921 = 15.33%
- Branch: 34 / 462 = 7.36%
- Line: 251 / 1860 = 13.49%
- Method: 50 / 343 = 14.58%
- Class: 20 / 74 = 27.03%

### Coverage evidence for screenshot capture
- Open and capture:
  - BE/Test_script/CVConnect-Unit-Test/user-tests/target/site/jacoco/index.html

## 1.7 References and Prompts Used

### References
- Internal course reference document requested by instructor:
  - https://drive.google.com/file/d/1mcGQTYDVWEl2mBprHM6fjk6zQ99kHnCE/view
- Existing project documents:
  - BE/Test_script/CVConnect-Unit-Test/UNIT_TEST_ASSESSMENT_2026-04-15.md
  - BE/Test_script/CVConnect-Unit-Test/UNIT_TEST_REFERENCE_COMBINED_2026-04-15.md

### Prompts used
- My role is to test user-service through user-tests, help me fulfill instructor requirement
- Unit Testing Report requirements (items 1.1 to 1.7 and section 2 script requirements)

## 2. Script Requirement Compliance Checklist
- Detailed comments added for clarity in all new test classes.
- Every test method includes a Test Case ID comment.
- Naming convention is descriptive and meaningful for tests, variables, and methods.
- CheckDB implemented by verifying repository interactions and persisted field values.
- Rollback behavior validated via:
  - no-save assertions on invalid input branches
  - retry/compensation behavior in FailedRollbackRetryJob tests
