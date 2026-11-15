---
slug: manual-retry
type: challenge
title: 'Exercise 7: Manual Activity Retry'
teaser: The transfer stops on a bad account number and waits for you to Signal the
  fix. The withdrawal that already went through stays done.
notes:
- type: text
  contents: |-
    # Some failures will not fix themselves

    A network blip goes away if you retry in a second. A typo in an account
    number does not, no matter how many times you retry.

    For that kind of failure you want the Workflow to stop, keep everything
    it has already done, and wait for a human to correct the data.
- type: text
  contents: |-
    # Did you know?

    An Activity failure is an ordinary exception inside the Workflow. You
    can catch it, loop, and wait on a Signal like any other state. The
    failed attempt, the Signal, and the successful retry all land in the
    event history.
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
difficulty: intermediate
timelimit: 1800
enhanced_loading: null
---

# Manual Activity Retry

The transfer stops on a bad account number and waits for you to fix it. The
withdrawal that already went through does not get undone.

# The Code

Open the [button label="Code Editor" background="#444CE7"](tab-0). Five TODOs
across two files. The interfaces, `RetryUpdate`, the Worker and the starter
are done.

### `BankingActivitiesImpl.java`

Two TODOs. `withdraw` and `deposit` each already check for an account name
containing `invalid`. Make that check throw a non-retryable failure:

```java
if (accountId.contains("invalid")) {
    throw ApplicationFailure.newNonRetryableFailure(
        "Invalid account ID: " + accountId, "InvalidAccount");
}
```

Import `io.temporal.failure.ApplicationFailure`. Non-retryable is the whole
trick. A plain exception would be retried automatically, forever by default,
and the Workflow would never get the chance to intervene.

### `MoneyTransferWorkflowImpl.java`

Three TODOs. `executeWithRetry(Runnable operation)` is already written: it
runs the operation, and on failure sets the status to `RETRYING`, clears
`retryRequested`, and waits on `Workflow.await` until a `retry` Signal sets it
again. Read it, then:

1. Run the withdrawal through it. Read the account from `updatedRequest`
   inside the lambda, not from `request`, so a correction from the Signal is
   picked up on the next attempt:

   ```java
   executeWithRetry(() -> activities.withdraw(updatedRequest.fromAccount(), updatedRequest.amount()));
   ```

2. Do the same for the deposit, with `toAccount`.
3. Implement `retry(RetryUpdate update)`. Replace `updatedRequest` with a
   corrected copy, using the `with...` helpers on `TransferRequest`, then set
   `retryRequested = true`. The keys are `fromAccount`, `toAccount` and
   `amount`:

   ```java
   updatedRequest = switch (update.key()) {
       case "fromAccount" -> updatedRequest.withFromAccount(update.value());
       case "toAccount" -> updatedRequest.withToAccount(update.value());
       case "amount" -> updatedRequest.withAmount(Double.parseDouble(update.value()));
       default -> updatedRequest;
   };
   ```

# Start the Worker

Click the [button label="Worker" background="#444CE7"](tab-1):

```bash,run
./gradlew -q execute -PmainClass=com.temporal.training.exercise7.StartWorker
```

This exercise uses its own task queue, `money-transfer-task-queue`.

# Start a Transfer to a Bad Account

Click the [button label="Terminal" background="#444CE7"](tab-2):

```bash,run
./gradlew -q execute -PmainClass=com.temporal.training.exercise7.StartWorkflow
```

The starter sends `100.0` from `account-123` to `invalid-account-456` and
exits right away. It starts the Workflow and does not wait for the result.
Everything from here on you do from the CLI. Grab the Workflow ID:

```bash,run
WORKFLOW_ID=$(temporal workflow list --query 'TaskQueue="money-transfer-task-queue" AND ExecutionStatus="Running"' --limit 1 -o json | jq -r '.[0].execution.workflowId')
echo "$WORKFLOW_ID"
```

# Approve It

The withdrawal has run and the Workflow is waiting for approval. Send it:

```bash,run
temporal workflow signal --workflow-id "$WORKFLOW_ID" --name approve --input true
```

Watch the [button label="Worker" background="#444CE7"](tab-1) tab. The deposit
to `invalid-account-456` fails once, with no retry.

# Check Where It Parked

```bash,run
temporal workflow query --workflow-id "$WORKFLOW_ID" --name getStatus
```

```bash,nocopy
Query result:
  QueryResult  "RETRYING"
```

That is the Workflow parked mid-transfer, holding the state of a withdrawal
that already succeeded.

# Fix It

Send the corrected account. The input is JSON for the `RetryUpdate` record:

```bash,run
temporal workflow signal \
  --workflow-id "$WORKFLOW_ID" \
  --name retry \
  --input '{"key":"toAccount","value":"account-456"}'
```

Then wait for the result:

```bash,run
temporal workflow result --workflow-id "$WORKFLOW_ID"
```

The deposit ran against the corrected account. Withdraw never ran twice.

Open the [button label="Temporal UI" background="#444CE7"](tab-3) and look at
the Event History: `ActivityTaskFailed` for the deposit, then the `retry`
Signal, then a fresh `ActivityTaskScheduled` for the same Activity.

> [!NOTE]
> Lost `$WORKFLOW_ID` because you opened a new terminal? Run the
> `temporal workflow list` line again. It finds the Workflow that is still
> running.

Click **Check** when the transfer completes.

# Key Takeaways

- `ApplicationFailure.newNonRetryableFailure` stops automatic retry for failures
  that data changes, not time, will fix.
- A Workflow can catch the failure, park on `Workflow.await`, and resume.
- Work already completed stays completed. That is the difference from
  restarting the process.
- Signals carry the corrected data in, so no redeploy and no manual database edit.
