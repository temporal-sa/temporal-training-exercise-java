---
slug: unit-testing
type: challenge
title: 'Exercise 6: Unit Testing Workflows'
teaser: Test the transfer in-process with TestWorkflowRule, mocked Activities and
  time skipping.
notes:
- type: text
  contents: |-
    # How do you test a Workflow that waits for a human?

    The approval might take a day. Your CI run cannot.

    Temporal's test environment runs a server in-process and skips time
    forward whenever the Workflow is blocked, so a day-long wait finishes
    in milliseconds.
- type: text
  contents: |-
    # Did you know?

    The test environment's clock skips ahead whenever every Workflow in it
    is blocked. A delayed callback registered for "one second from now"
    runs as soon as the Workflow parks on `Workflow.await`, and an hour
    long timer fires just as fast.
tabs:
- title: Code Editor
  type: code
  hostname: workshop
  path: /root/workshop/src/test/java/com/temporal/training/exercise6
- title: Worker
  type: terminal
  hostname: workshop
  workdir: /root/workshop
- title: Terminal
  type: terminal
  hostname: workshop
  workdir: /root/workshop
- title: Temporal UI
  type: service
  hostname: workshop
  path: /
  port: 8233
- title: Solution
  type: code
  hostname: workshop
  path: /root/workshop/src/test/java/com/temporal/training/solution6
- title: Network Control Panel
  type: service
  hostname: workshop
  path: /
  port: 5000
- title: Workflow Under Test
  type: code
  hostname: workshop
  path: /root/workshop/src/main/java/com/temporal/training/exercise6
difficulty: intermediate
timelimit: 1800
enhanced_loading: null
---

# Unit Testing Workflows

No Worker to start. No dev server to talk to. One test file, three tests.

> [!NOTE]
> This is the only exercise that does not need the Temporal dev server. Run
> everything from the [button label="Terminal" background="#444CE7"](tab-2).
> The Workflow being tested is the finished exercise 5 transfer. It is in the
> [button label="Workflow Under Test" background="#444CE7"](tab-6) tab, and
> you do not need to change it.

# Green Is Not the Goal

Run the tests before you change anything:

```bash,run
./gradlew test --tests "com.temporal.training.exercise6.MoneyTransferWorkflowTest" --rerun
```

`BUILD SUCCESSFUL`. All three tests pass, because none of them asserts
anything yet. Your job is to make them pass *and* make them capable of
failing.

# The Code

Open the [button label="Code Editor" background="#444CE7"](tab-0) on
`MoneyTransferWorkflowTest.java`. The `TestWorkflowRule` is already declared
with `setDoNotStart(true)`, so each test registers its Activities before the
environment starts.

### `registerSearchAttribute()`

The Workflow upserts `AccountId`, and the in-process server knows nothing
about it until you register it:

```java
testWorkflowRule.getTestEnvironment().registerSearchAttribute(
    "AccountId", IndexedValueType.INDEXED_VALUE_TYPE_TEXT
);
```

### `testSuccessfulTransfer()`

1. Mock the Activities. `withoutAnnotations()` stops Mockito from copying the
   `@ActivityInterface` annotations onto the mock class, which Temporal would
   otherwise reject:

   ```java
   BankingActivities mockActivities = Mockito.mock(
       BankingActivities.class, withSettings().withoutAnnotations());
   ```

2. Call `registerSearchAttribute()`, register the mock with
   `testWorkflowRule.getWorker().registerActivitiesImplementations(...)`, then
   `testWorkflowRule.getTestEnvironment().start()`.
3. Build a Workflow stub from `testWorkflowRule.getWorkflowClient()` on
   `testWorkflowRule.getTaskQueue()`.
4. Schedule the approval Signal one second into the Workflow's run:

   ```java
   testWorkflowRule.getTestEnvironment().registerDelayedCallback(
       java.time.Duration.ofSeconds(1), () -> workflow.approve(true));
   ```

   The file does not import `java.time.Duration`, hence the full name.

5. Call `workflow.transfer(request)`. It blocks until the Workflow finishes,
   which happens almost immediately because time skips to the callback.
6. Assert the result is `"Transfer completed successfully"`, `getStatus()` is
   `COMPLETED`, `withdraw` and `deposit` were each called with the right
   account and amount, and `refund` was `never()` called.

### `testRejectedTransfer()`

Same shape with `approve(false)`. Assert `"Transfer rejected and refunded"`,
`CANCELLED`, `withdraw` and `refund` called, `deposit` never called.

### `testQueryStatus()`

Register two callbacks: one at 500 ms that asserts `getStatus()` is `PENDING`,
and one at one second that approves. After `transfer` returns, assert the
status is `COMPLETED`.

# Run the Tests

From the [button label="Terminal" background="#444CE7"](tab-2):

```bash,run
./gradlew test --tests "com.temporal.training.exercise6.MoneyTransferWorkflowTest" --rerun
```

`--rerun` makes Gradle run the tests even when nothing changed since the last
run. On a failure Gradle names the test and the assertion. To list each
result:

```bash,run
grep -o 'testcase name="[^"]*"\|<failure message="[^"]*"' build/test-results/test/TEST-com.temporal.training.exercise6.MoneyTransferWorkflowTest.xml
```

# Prove the Tests Bite

Open the [button label="Workflow Under Test" background="#444CE7"](tab-6)
tab and, in `MoneyTransferWorkflowImpl.java`, change the approved branch to
call `refund` instead of `deposit`. Run the tests again.
`testSuccessfulTransfer` should now fail on one of your `verify` lines. Put
`deposit` back and run once more before you click **Check**.

Click **Check** when all three pass.

# Key Takeaways

- `TestWorkflowRule` runs an in-process test server with a time-skipping clock.
- `registerDelayedCallback` is how a test sends a Signal "later" without
  sleeping.
- Mock the Activity interface with `withoutAnnotations()`, and `verify` the
  calls the Workflow made.
- A test with no assertions passes forever. Make sure yours can fail.
