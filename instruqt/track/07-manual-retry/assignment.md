---
slug: manual-retry
type: challenge
title: 'Exercise 7: Manual Activity Retry'
teaser: An Activity fails on bad data. Automatic retry cannot help. Fix the data with
  a Signal and let it continue.
notes:
- type: text
  contents: |-
    # The account number is wrong. How many times should Temporal retry?

    Automatic retry is the right answer when the failure is transient.
    A network blip, a rate limit, a restarted database.

    A typo in an account number is not transient. Retrying it a hundred
    times gets you the same error a hundred times.
- type: text
  contents: |-
    # Did you know?

    `ApplicationFailure.newNonRetryableFailure(...)` tells Temporal to stop
    retrying immediately and surface the failure to the Workflow.

    The Workflow can then park, wait for a human to send corrected data,
    and pick the Activity back up. The withdrawal that already succeeded
    stays succeeded.
tabs:
- title: Code Editor
  type: code
  hostname: workshop
  path: /root/workshop/src/main/java/com/temporal/training/exercise7
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
  path: /root/workshop/src/main/java/com/temporal/training/solution7
- title: Network Control Panel
  type: service
  hostname: workshop
  path: /
  port: 5000
- title: Signals
  type: terminal
  hostname: workshop
  workdir: /root/workshop
difficulty: intermediate
timelimit: 1800
---

# Manual Activity Retry

The transfer stops on a bad account number and waits for you to fix it. The
withdrawal that already went through does not get undone.

> [!IMPORTANT]
> This exercise has a seventh tab. Use
> [button label="Signals" background="#444CE7"](tab-6) to send Signals while
> the Worker keeps logging in the
> [button label="Worker" background="#444CE7"](tab-1) tab.

# The Code

Open the [button label="Code Editor" background="#444CE7"](tab-0). Five TODOs
across two files.

### `BankingActivitiesImpl.java`

Two TODOs. `withdraw` and `deposit` each reject an account name containing
`invalid`, and they have to do it in a way automatic retry will respect:

```java
if (accountId.contains("invalid")) {
    throw ApplicationFailure.newNonRetryableFailure(
        "Invalid toAccount ID: " + accountId, "InvalidAccount");
}
```

That second argument is the failure **type**. Non-retryable is the whole trick:
throw an ordinary exception instead and Temporal retries it forever, so the
Workflow never gets a chance to intervene.

### `MoneyTransferWorkflowImpl.java`

Three TODOs. Implement `executeWithRetry`, then wrap the withdraw and deposit
calls in it:

```java
private void executeWithRetry(Runnable operation) {
    while (true) {
        try {
            operation.run();
            break;
        } catch (Exception e) {
            status = TransferStatus.RETRYING;
            retryRequested = false;
            Workflow.await(() -> retryRequested);
        }
    }
}
```

The `retry` Signal handler is already sketched for you. It takes a
`RetryUpdate` record of `key` and `value`, patches the stored request through
the `withFromAccount` / `withToAccount` / `withAmount` copy methods, and flips
`retryRequested`. The keys are `fromAccount`, `toAccount` and `amount`.

> [!NOTE]
> `updatedRequest` exists because the Workflow has to keep working from the
> corrected data, not the data it was started with. Reading `request` inside
> the retry loop would re-send the same bad account every time.

# Start the Worker

Click the [button label="Worker" background="#444CE7"](tab-1):

```bash,run
./gradlew execute -PmainClass=com.temporal.training.exercise7.StartWorker -q --console=plain
```

# Run It

Click the [button label="Terminal" background="#444CE7"](tab-2):

```bash,run
./gradlew execute -PmainClass=com.temporal.training.exercise7.StartWorkflow -q --console=plain
```

The starter uses `WorkflowClient.start`, not `execute` — it fires the Workflow
off and exits without waiting or printing anything. The transfer is now running
without a client attached to it, which is the point.

It ships with `invalid-account-456` as the destination, so it is already on the
failure path.

# Check Where It Parked

Click the [button label="Signals" background="#444CE7"](tab-6). Find the
Workflow and ask it what it is doing:

```bash,run
WORKFLOW_ID=$(temporal workflow list --query 'TaskQueue="money-transfer-task-queue" AND ExecutionStatus="Running"' --limit 1 -o json | jq -r '.[0].execution.workflowId')
echo "$WORKFLOW_ID"
temporal workflow query --workflow-id "$WORKFLOW_ID" --type getStatus
```

```bash,nocopy
PENDING
```

It is waiting for approval. Send it:

```bash,run
temporal workflow signal \
  --workflow-id "$WORKFLOW_ID" \
  --name approve \
  --input true
```

Now the deposit runs against `invalid-account-456`, fails once with no retry,
and the Workflow parks. Ask again:

```bash,run
temporal workflow query --workflow-id "$WORKFLOW_ID" --type getStatus
```

```bash,nocopy
RETRYING
```

That is the Workflow parked mid-transfer, holding the state of a withdrawal
that already succeeded.

# Fix It

Still in the [button label="Signals" background="#444CE7"](tab-6), send the
corrected account. `$WORKFLOW_ID` is already set from the previous step:

```bash,run
temporal workflow signal \
  --workflow-id "$WORKFLOW_ID" \
  --name retry \
  --input '{"key":"toAccount","value":"account-456"}'
```

Watch the [button label="Worker" background="#444CE7"](tab-1) tab: the deposit
runs against the corrected account and the transfer completes. Withdraw never
ran twice.

Open the [button label="Temporal UI" background="#444CE7"](tab-3) and look at
the event history: one `ActivityTaskFailed` for the deposit, then the Signal,
then a fresh `ActivityTaskScheduled` for the same Activity.

Click **Check** when the transfer completes.

# Key Takeaways

- `newNonRetryableFailure` stops automatic retry for failures that data
  changes, not time, will fix.
- A Workflow can catch the failure, park on `Workflow.await`, and resume.
- Work already completed stays completed. That is the difference from
  restarting the process.
- Signals carry the corrected data in, so no redeploy and no manual database edit.
- `WorkflowClient.start` returns immediately; `WorkflowClient.execute` blocks.
  A long-running Workflow does not need anyone waiting on it.
