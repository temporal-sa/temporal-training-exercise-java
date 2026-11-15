---
slug: signals
type: challenge
title: 'Exercise 2: Money Transfer with Signals'
teaser: A transfer that stops and waits for a human to approve it, then deposits or
  refunds.
notes:
- type: text
  contents: |-
    # A Workflow is waiting on a human. Who pays for the wait?

    The money left the source account. Now a person has to approve the
    deposit. That person is at lunch.

    No polling loop. No cron job. No row in a "pending approvals" table.
    The Workflow just waits, and the wait costs you nothing.
- type: text
  contents: |-
    # Did you know?

    In the Java SDK a cached Workflow parked on `Workflow.await` does hold
    a thread. But the cache is only an optimization. The Worker can evict
    the Workflow at any time, or crash, and Temporal rebuilds it by replay
    when the Signal arrives. The wait lives in the server's history, not in
    the Worker's memory, so ten seconds and ten months cost the same.
tabs:
- title: Code Editor
  type: code
  hostname: workshop
  path: /root/workshop/src/main/java/com/temporal/training/exercise2
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
  path: /root/workshop/src/main/java/com/temporal/training/solution2
- title: Network Control Panel
  type: service
  hostname: workshop
  path: /
  port: 5000
difficulty: basic
timelimit: 1800
enhanced_loading: null
---

# Money Transfer with Signals

Withdraw. Wait for a human. Deposit if they said yes, refund if they said no.

# The Code

Open the [button label="Code Editor" background="#444CE7"](tab-0).
`TransferRequest.java` is a finished record, and `BankingActivities.java` is a
finished interface whose leftover TODO you can ignore. Five files are yours.

### `BankingActivitiesImpl.java`

Implement `withdraw`, `deposit` and `refund`. Each one logs what it did. The
first two also throw a `RuntimeException` some of the time:

```java
if (Math.random() < 0.1) {
    throw new RuntimeException("Withdrawal failed - insufficient funds");
}
```

The random failure is deliberate. It is what makes Temporal's automatic
Activity retries visible in the next step.

### `MoneyTransferWorkflow.java`

Put `@SignalMethod` on `approve(boolean approved)`. That annotation is what
makes the method callable from outside a running Workflow.

### `MoneyTransferWorkflowImpl.java`

- Build the Activity stub with a `startToCloseTimeout`, the way you did in
  exercise 1.
- In `transfer`: call `withdraw`, then block until the approval arrives, then
  call `deposit` on approval or `refund` on rejection, and return a status
  string.
- In `approve`: record the decision and flip a flag the Workflow is waiting on.

The wait is one line:

```java
Workflow.await(() -> approvalReceived);
```

Use a separate `approvalReceived` flag rather than waiting on `approved`
itself. Otherwise a rejection (`approve(false)`) never unblocks the wait.

### `StartWorker.java`

Write the Worker from scratch this time: service stubs, client, factory, a
Worker on `TASK_QUEUE`, register the Workflow type and the Activities, and
`factory.start()`. Exercise 1's `StartWorker` is the template.

### `StartWorkflow.java`

Start the Workflow without blocking, wait three seconds, send the Signal
through the same stub, then wait for the result:

```java
CompletableFuture<String> result = WorkflowClient.execute(workflow::transfer, request);
Thread.sleep(3000);
workflow.approve(true);
System.out.println("Transfer result: " + result.get());
```

`WorkflowClient.execute` returns as soon as the Workflow has started. Calling
`workflow.transfer(request)` directly would block until the Workflow finished,
and the Workflow cannot finish until you send the Signal.

# Start the Worker

Click the [button label="Worker" background="#444CE7"](tab-1):

```bash,run
./gradlew -q execute -PmainClass=com.temporal.training.exercise2.StartWorker
```

# Run It

Click the [button label="Terminal" background="#444CE7"](tab-2):

```bash,run
./gradlew -q execute -PmainClass=com.temporal.training.exercise2.StartWorkflow
```

The starter waits three seconds, then sends the approval. Watch the Worker
tab. If an Activity hit its simulated failure you will see the exception, then
the same Activity running again. You did not write that retry. Run the starter
a few times if you want to catch one.

# Signal It Yourself

The Signal does not have to come from the starter. Any client that can reach
the server can move the Workflow along, including the CLI. Paste the Workflow
ID the starter used and run this from the
[button label="Terminal" background="#444CE7"](tab-2):

```bash
WORKFLOW_ID=money-transfer-1788758722478

temporal workflow signal \
  --workflow-id "$WORKFLOW_ID" \
  --name approve \
  --input true
```

This particular run already got its approval from the starter three seconds
in, so the CLI reports that the Workflow has already completed. Exercise 7
uses this same command against a Workflow that is genuinely waiting.

# Look at the Wait

Click the [button label="Temporal UI" background="#444CE7"](tab-3). Open your
Workflow, then the **Event History**. Find `WorkflowExecutionSignaled`. Above
it, the Workflow was doing nothing at all. Had the Worker died during that
gap, a restarted Worker would have picked the Workflow up exactly where it was.

Click **Check** when the transfer completes.

# Key Takeaways

- `@SignalMethod` handlers mutate Workflow state from outside.
- `Workflow.await` blocks on that state. The wait survives Worker restarts
  because it lives in history, not in memory.
- Failed Activities retry on their own. The default retry policy is already on.
- The Signal is recorded in history, so replay makes the same decision.
