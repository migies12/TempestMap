# Example M5: Testing and Code Review

## 1. Change History

| **Change Date**   | **Modified Sections** | **Rationale** |
| ----------------- | --------------------- | ------------- |
| _Nothing to show_ |

---

## 2. Back-end Test Specification: APIs

### 2.1. Locations of Back-end Tests and Instructions to Run Them

#### 2.1.1. Tests

| **Interface**                 | **Group Location (No Mocks)**         | **Group Location (With Mocks)**   | **Mocked Components**               |
| ----------------------------- | ------------------------------------- | --------------------------------- | ----------------------------------- |
| **GET /**                     | [`tests/unmocked/backend.test.js`](#) | [`tests/mock/backend.test.js`](#) | None                                |
| **POST /test_cron**           |                                       |                                   | axios, External Disaster API        |
| **GET /event**                |                                       |                                   | DynamoDB scan                       |
| **POST /event/custom**        |                                       |                                   | DynamoDB put, uuid generation       |
| **GET /event/firms**          |                                       |                                   | axios, csv-parser                   |
| **POST /comment/:event_id**   |                                       |                                   | DynamoDB update, uuid generation    |
| **GET /comment/:event_id**    |                                       |                                   | DynamoDB get                        |
| **DELETE /comment/:event_id** |                                       |                                   | DynamoDB delete & update operations |
| **POST /user**                |                                       |                                   | DynamoDB put, Firebase Messaging    |
| **GET /user/:user_id**        |                                       |                                   | DynamoDB get                        |
| **POST /user/locations**      |                                       |                                   | DynamoDB update                     |

#### 2.1.2. Commit Hash Where Tests Run

`[Insert Commit SHA here]`

#### 2.1.3. Explanation on How to Run the Tests

1. **Clone the Repository**:

   - Open your terminal and run:
     ```
     git clone https://github.com/example/your-project.git
     ```

2. **...**

### 2.2. GitHub Actions Configuration Location

`~/.github/workflows/backend-tests.yml`

### 2.3. Jest Coverage Report Screenshots With Mocks

_(Placeholder for Jest coverage screenshot with mocks enabled)_

### 2.4. Jest Coverage Report Screenshots Without Mocks

_(Placeholder for Jest coverage screenshot without mocks)_

---

## 3. Back-end Test Specification: Tests of Non-Functional Requirements

### 3.1. Test Locations in Git

| **Non-Functional Requirement**   | **Location in Git**                                                                                                                                                                                                             |
| -------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Performance (Response Time)**  | [`tests/nonfunctional/response_time.test.js`](#)                                                                                                                                                                                |
| **Accessibility Compliance**     | [`frontend/app/src/androidTest/java/com/example/m1/nonfunctional/AccessibilityTest.kt`](https://github.com/migies12/TempestMap/blob/main/frontend/app/src/androidTest/java/com/example/m1/nonfunctional/AccessibilityTest.kt)   |
| **Ease of Use (Error Handling)** | [`frontend/app/src/androidTest/java/com/example/m1/nonfunctional/ErrorHandlingTests.kt`](https://github.com/migies12/TempestMap/blob/main/frontend/app/src/androidTest/java/com/example/m1/nonfunctional/ErrorHandlingTests.kt) |

### 3.2. Test Verification and Logs

- **Performance (Response Time)**

  - **Verification:** This test suite simulates multiple concurrent API calls using Jest along with a load-testing utility to mimic real-world user behavior. The focus is on key endpoints such as user login and study group search to ensure that each call completes within the target response time of 2 seconds under normal load. The test logs capture metrics such as average response time, maximum response time, and error rates. These logs are then analyzed to identify any performance bottlenecks, ensuring the system can handle expected traffic without degradation in user experience.
  - **Log Output**
    ```
    [Placeholder for log]
    ```

- **Accessibility Compliance**

  - **Verification:** This test suite verifies compliance with [WCAG 2.1 Level AA accessibility standards](https://www.w3.org/TR/WCAG21/) using automated testing with Espresso and AccessibilityChecks. The tests validate three key aspects of accessibility: (1) proper content descriptions for screen reader compatibility, (2) color contrast ratios that meet minimum thresholds, and (3) touch target sizes of at least 44dp to ensure usability for individuals with motor impairments. The logs record detailed metrics for each test, including exact color values, contrast ratios, element dimensions, and content description presence. Since there is a clear measurable metric that is measured during the test process, if at any point the measure violates the accessibility standards, the test will fail, and the exact measure will be indicated in the log accordingly.
  - **Log Output**

    ```
    ---------------------------- PROCESS STARTED (29766) for package com.example.m1 ----------------------------
    2025-03-12 17:14:30.782 29766-29796 TestRunner              com.example.m1                       I  started: testTouchTargetSize(com.example.m1.nonfunctional.AccessibilityTest)
    2025-03-12 17:14:31.407 29766-29796 AccessibilityTest       com.example.m1                       D  ================================
    2025-03-12 17:14:31.430 29766-29796 AccessibilityTest       com.example.m1                       D  Starting Accessibility Test
    2025-03-12 17:14:31.432 29766-29796 AccessibilityTest       com.example.m1                       D  ================================
    2025-03-12 17:14:31.438 29766-29796 AccessibilityTest       com.example.m1                       D  Accessibility checks enabled
    2025-03-12 17:14:31.442 29766-29796 AccessibilityTest       com.example.m1                       D  Starting touch target size test
    2025-03-12 17:14:31.659 29766-29796 AccessibilityTest       com.example.m1                       D  Navigating to map screen
    2025-03-12 17:14:32.389 29766-29796 AccessibilityTest       com.example.m1                       D  Checking touch target size for add marker button
    2025-03-12 17:14:32.393 29766-29766 AccessibilityTest       com.example.m1                       D  Touch target size check for fabAddMarker: width=147px, height=147px, min required=115.5px - PASS
    2025-03-12 17:14:32.395 29766-29796 AccessibilityTest       com.example.m1                       D  Touch target size test completed
    2025-03-12 17:14:32.397 29766-29796 TestRunner              com.example.m1                       I  finished: testTouchTargetSize(com.example.m1.nonfunctional.AccessibilityTest)
    2025-03-12 17:14:32.607 29766-29796 TestRunner              com.example.m1                       I  started: testTextContrast(com.example.m1.nonfunctional.AccessibilityTest)
    2025-03-12 17:14:32.838 29766-29796 AccessibilityTest       com.example.m1                       D  ================================
    2025-03-12 17:14:32.840 29766-29796 AccessibilityTest       com.example.m1                       D  Starting Accessibility Test
    2025-03-12 17:14:32.841 29766-29796 AccessibilityTest       com.example.m1                       D  ================================
    2025-03-12 17:14:32.843 29766-29796 AccessibilityTest       com.example.m1                       D  Accessibility checks enabled
    2025-03-12 17:14:32.844 29766-29796 AccessibilityTest       com.example.m1                       D  Starting text contrast test
    2025-03-12 17:14:33.044 29766-29766 AccessibilityTest       com.example.m1                       D  Scanning view hierarchy for text contrast issues
    2025-03-12 17:14:33.046 29766-29766 AccessibilityTest       com.example.m1                       D  TextView contrast check: View(MaterialTextView@81924395) - text color: #FF49454F, bg color: #FFFFFBFE, size: 14.095238dp, ratio: 9.11, required: 4.5 - PASS
    2025-03-12 17:14:33.047 29766-29766 AccessibilityTest       com.example.m1                       D  TextView contrast check: navigation_bar_item_small_label_view - text color: #FF1C1B1F, bg color: #FFFFFBFE, size: 12.190476dp, ratio: 16.71, required: 4.5 - PASS
    2025-03-12 17:14:33.049 29766-29766 AccessibilityTest       com.example.m1                       D  TextView contrast check: navigation_bar_item_large_label_view - text color: #FF1C1B1F, bg color: #FFFFFBFE, size: 12.190476dp (bold), ratio: 16.71, required: 4.5 - PASS
    2025-03-12 17:14:33.050 29766-29766 AccessibilityTest       com.example.m1                       D  TextView contrast check: navigation_bar_item_small_label_view - text color: #FF49454F, bg color: #FFFFFBFE, size: 12.190476dp, ratio: 9.11, required: 4.5 - PASS
    2025-03-12 17:14:33.052 29766-29766 AccessibilityTest       com.example.m1                       D  TextView contrast check: navigation_bar_item_large_label_view - text color: #FF49454F, bg color: #FFFFFBFE, size: 12.190476dp (bold), ratio: 9.11, required: 4.5 - PASS
    2025-03-12 17:14:33.056 29766-29766 AccessibilityTest       com.example.m1                       D  TextView contrast check: navigation_bar_item_small_label_view - text color: #FF49454F, bg color: #FFFFFBFE, size: 12.190476dp, ratio: 9.11, required: 4.5 - PASS
    2025-03-12 17:14:33.058 29766-29766 AccessibilityTest       com.example.m1                       D  TextView contrast check: navigation_bar_item_large_label_view - text color: #FF49454F, bg color: #FFFFFBFE, size: 12.190476dp (bold), ratio: 9.11, required: 4.5 - PASS
    2025-03-12 17:14:33.059 29766-29766 AccessibilityTest       com.example.m1                       D  TextView contrast check: navigation_bar_item_small_label_view - text color: #FF49454F, bg color: #FFFFFBFE, size: 12.190476dp, ratio: 9.11, required: 4.5 - PASS
    2025-03-12 17:14:33.060 29766-29766 AccessibilityTest       com.example.m1                       D  TextView contrast check: navigation_bar_item_large_label_view - text color: #FF49454F, bg color: #FFFFFBFE, size: 12.190476dp (bold), ratio: 9.11, required: 4.5 - PASS
    2025-03-12 17:14:33.062 29766-29766 AccessibilityTest       com.example.m1                       D  Text contrast check completed. Passing views: 9, Failing views: 0
    2025-03-12 17:14:33.064 29766-29796 AccessibilityTest       com.example.m1                       D  Text contrast test completed
    2025-03-12 17:14:33.065 29766-29796 TestRunner              com.example.m1                       I  finished: testTextContrast(com.example.m1.nonfunctional.AccessibilityTest)
    2025-03-12 17:14:33.174 29766-29796 TestRunner              com.example.m1                       I  started: testContentDescriptions(com.example.m1.nonfunctional.AccessibilityTest)
    2025-03-12 17:14:33.406 29766-29796 AccessibilityTest       com.example.m1                       D  ================================
    2025-03-12 17:14:33.409 29766-29796 AccessibilityTest       com.example.m1                       D  Starting Accessibility Test
    2025-03-12 17:14:33.414 29766-29796 AccessibilityTest       com.example.m1                       D  ================================
    2025-03-12 17:14:33.419 29766-29796 AccessibilityTest       com.example.m1                       D  Accessibility checks enabled
    2025-03-12 17:14:33.421 29766-29796 AccessibilityTest       com.example.m1                       D  Starting content descriptions test
    2025-03-12 17:14:33.637 29766-29796 AccessibilityTest       com.example.m1                       D  Checking content description for Home Navigation (ID: 2131362175)
    2025-03-12 17:14:33.641 29766-29766 AccessibilityTest       com.example.m1                       D  Content description check for nav_home: "Home" - PASS
    2025-03-12 17:14:33.642 29766-29796 AccessibilityTest       com.example.m1                       D  Checking content description for Map Navigation (ID: 2131362177)
    2025-03-12 17:14:33.648 29766-29766 AccessibilityTest       com.example.m1                       D  Content description check for nav_map: "Map" - PASS
    2025-03-12 17:14:33.649 29766-29796 AccessibilityTest       com.example.m1                       D  Checking content description for Profile Navigation (ID: 2131362178)
    2025-03-12 17:14:33.652 29766-29766 AccessibilityTest       com.example.m1                       D  Content description check for nav_profile: "Profile" - PASS
    2025-03-12 17:14:33.653 29766-29796 AccessibilityTest       com.example.m1                       D  Checking content description for Alerts Navigation (ID: 2131362173)
    2025-03-12 17:14:33.656 29766-29766 AccessibilityTest       com.example.m1                       D  Content description check for nav_alerts: "Alerts" - PASS
    2025-03-12 17:14:33.658 29766-29796 AccessibilityTest       com.example.m1                       D  Content descriptions test completed
    2025-03-12 17:14:33.659 29766-29796 TestRunner              com.example.m1                       I  finished: testContentDescriptions(com.example.m1.nonfunctional.AccessibilityTest)
    ---------------------------- PROCESS ENDED (29766) for package com.example.m1 ----------------------------
    ```

- **Ease of Use (Error Handling)**
  - **Verification:**
  - **Log Output**

---

## 4. Front-end Test Specification

### 4.1. Location in Git of Front-end Test Suite:

`frontend/src/androidTest/java/com/studygroupfinder/`

### 4.2. Tests

- **Use Case: Login**

  - **Expected Behaviors:**
    | **Scenario Steps** | **Test Case Steps** |
    | ------------------ | ------------------- |
    | 1. The user opens â€œAdd Todo Itemsâ€ screen. | Open â€œAdd Todo Itemsâ€ screen. |
    | 2. The app shows an input text field and an â€œAddâ€ button. The add button is disabled. | Check that the text field is present on screen.<br>Check that the button labelled â€œAddâ€ is present on screen.<br>Check that the â€œAddâ€ button is disabled. |
    | 3a. The user inputs an ill-formatted string. | Input â€œ*^*^^OQ#$â€ in the text field. |
    | 3a1. The app displays an error message prompting the user for the expected format. | Check that a dialog is opened with the text: â€œPlease use only alphanumeric charactersâ€. |
    | 3. The user inputs a new item for the list and the add button becomes enabled. | Input â€œbuy milkâ€ in the text field.<br>Check that the button labelled â€œaddâ€ is enabled. |
    | 4. The user presses the â€œAddâ€ button. | Click the button labelled â€œaddâ€. |
    | 5. The screen refreshes and the new item is at the bottom of the todo list. | Check that a text box with the text â€œbuy milkâ€ is present on screen.<br>Input â€œbuy chocolateâ€ in the text field.<br>Click the button labelled â€œaddâ€.<br>Check that two text boxes are present on the screen with â€œbuy milkâ€ on top and â€œbuy chocolateâ€ at the bottom. |
    | 5a. The list exceeds the maximum todo-list size. | Repeat steps 3 to 5 ten times.<br>Check that a dialog is opened with the text: â€œYou have too many items, try completing one firstâ€. |

  - **Test Logs:**
    ```
    [Placeholder]
    ```

- **Use Case: ...**

  - **Expected Behaviors:**

    | **Scenario Steps** | **Test Case Steps** |
    | ------------------ | ------------------- |
    | ...                | ...                 |

  - **Test Logs:**
    ```
    [Placeholder for Espresso test execution logs]
    ```

- **...**

---

## 5. Automated Code Review Results

### 5.1. Commit Hash Where Codacy Ran

`[Insert Commit SHA here]`

### 5.2. Unfixed Issues per Codacy Category

_(Placeholder for screenshots of Codacyâ€™s Category Breakdown table in Overview)_

### 5.3. Unfixed Issues per Codacy Code Pattern

_(Placeholder for screenshots of Codacyâ€™s Issues page)_

### 5.4. Justifications for Unfixed Issues

- **Code Pattern: [Usage of Deprecated Modules](#)**

  1. **Issue**

     - **Location in Git:** [`src/services/chatService.js#L31`](#)
     - **Justification:** ...

  2. ...

- ...
