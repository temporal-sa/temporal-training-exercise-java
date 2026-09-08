---
slug: unit-testing
id: gtp1xx6ylujq
type: challenge
title: 'Exercise 6: Unit Testing Workflows'
teaser: Test a Workflow that waits for a human, with time skipping and mocked Activities.
  No dev server involved.
notes:
- type: text
  contents: |-
    # How do you unit test a Workflow that waits two days for approval?

    You do not wait two days. You do not mock out the wait either.

    The test environment skips the clock forward the instant nothing is
    left to run, so a two-day timer resolves in microseconds and the
    Workflow never knows.
- type: text
  contents: |-
    # No server needed

    `TestWorkflowRule` spins up an in-process test server. These tests run
    in CI with nothing installed, and the whole suite finishes in a few
    seconds.
tabs:
- id: 3njk0ae3pf2h
  title: Code Editor
  type: code
  hostname: workshop
  path: /root/workshop/src/test/java/com/temporal/training/exercise6
- id: xzoiao2d9x2f
  title: Worker
  type: terminal
  hostname: workshop
  workdir: /root/workshop
- id: hwvzgpz6sctr
  title: Terminal
  type: terminal
  hostname: workshop
  workdir: /root/workshop
- id: jj06ozogoiuu
  title: Temporal UI
  type: service
  hostname: workshop
  path: /
  port: 8233
- id: ulbs5dzffpn3
  title: Solution
  type: code
  hostname: workshop
  path: /root/workshop/src/test/java/com/temporal/training/solution6
- id: 0ju88eiuaq3a
  title: Network Control Panel
  type: service
  hostname: workshop
  path: /
  port: 5000
difficulty: intermediate
timelimit: 1800
enhanced_loading: null
---

# Unit Testing Workflows

No Worker to start. No dev server to talk to. One test file, nine TODOs.

> [!IMPORTANT]
> This is the only exercise whose code lives under `src/test`, not
> `src/main`. The [button label="Code Editor" background="#444CE7"](tab-0) and
> [button label="Solution" background="#444CE7"](tab-4) tabs are already
> pointed there. The Workflow under test is finished — you are writing the
> tests, not the Workflow.

# The Code

Open the [button label="Code Editor" background="#444CE7"](tab-0) and work
through `MoneyTransferWorkflowTest.java`.

### The rule

`TestWorkflowRule` is built with `setDoNotStart(true)`, which is deliberate.
Search attributes have to be registered **before** the environment starts, so
each test registers the attribute and the mocked Activities first, then calls
`start()`:

```java
testWorkflowRule.getTestEnvironment().registerSearchAttribute(
    "AccountId", IndexedValueType.INDEXED_VALUE_TYPE_TEXT
);
testWorkflowRule.getWorker().registerActivitiesImplementations(mockActivities);
testWorkflowRule.getTestEnvironment().start();
```

### The mock

```java
BankingActivities mockActivities = Mockito.mock(
        BankingActivities.class,
        withSettings().withoutAnnotations()
);
```

> [!WARNING]
> `withoutAnnotations()` is not optional. Without it Mockito copies Temporal's
> `@ActivityInterface` annotations onto the mock, the SDK sees a type it did
> not expect, and registration fails with an error that has nothing to do with
> your test.

### The Signal, under time skipping

You cannot send the approval after `workflow.transfer(request)` — that call
blocks until the Workflow finishes. Queue it up first and let the test clock
deliver it:

```java
testWorkflowRule.getTestEnvironment().registerDelayedCallback(
    java.time.Duration.ofSeconds(1),
    () -> workflow.approve(true)
);

String result = workflow.transfer(request);
```

### The three tests

**`testSuccessfulTransfer`** — approve with `true`. Assert the result is
`"Transfer completed successfully"`, the status is `COMPLETED`, `withdraw` and
`deposit` both ran with the right arguments, and `refund` never ran.

**`testRejectedTransfer`** — approve with `false`. Assert
`"Transfer rejected and refunded"`, status `CANCELLED`, `withdraw` and `refund`
ran, `deposit` did not.

**`testQueryStatus`** — register a callback at 500ms that asserts the status is
`PENDING`, another at one second that approves, then assert `COMPLETED` at the
end.

```java
verify(mockActivities).withdraw("account-123", 100.0);
verify(mockActivities, never()).refund(anyString(), anyDouble());
```

> [!NOTE]
> These are **JUnit 4** tests: `org.junit.Test`, `@Rule`, `@After`. The
> imports are already at the top of the file. A test written with JUnit 5
> annotations compiles and then silently never runs.

# Run the Tests

Click the [button label="Terminal" background="#444CE7"](tab-2):

```bash,run
./gradlew test --rerun --tests "com.temporal.training.exercise6.MoneyTransferWorkflowTest"
```

Three tests. All three should pass.

`--rerun` matters: Gradle caches test results, so without it a second run
reports `UP-TO-DATE` and tells you nothing.

The HTML report lands in `build/reports/tests/test/index.html` when you want to
see which assertion failed.

> [!WARNING]
> An empty `@Test` method passes. If all three go green before you have written
> any assertions, that is what happened — the Check gates on the assertions
> being there for exactly this reason.

Click **Check** when all three pass.

# Key Takeaways

- `TestWorkflowRule` gives you a test server in-process, no dev server required.
- Time skipping fast-forwards timers, so testing a long wait costs nothing.
- `registerDelayedCallback` is how a Signal reaches a Workflow that a blocking
  test call is already waiting on.
- Mock Activity interfaces with `withSettings().withoutAnnotations()`.
- Register search attributes before `start()`, which is why the rule is built
  with `setDoNotStart(true)`.
