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

    A Workflow blocked on `Workflow.await` holds no thread and no memory.
    Temporal takes it off the Worker entirely and brings it back when the
    Signal arrives. A wait of ten seconds and a wait of ten months cost
    the same.
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
---

# Money Transfer with Signals

Withdraw. Wait for a human. Deposit if they said yes, refund if they said no.

# The Code

Open the [button label="Code Editor" background="#444CE7"](tab-0). Six files
carry TODOs. `TransferRequest.java` is already done.

### `BankingActivities.java` and `BankingActivitiesImpl.java`

Declare and implement `withdraw`, `deposit` and `refund`. Each one logs what it
did and simulates a failure some of the time. The random failure is deliberate:
it is what makes Temporal's automatic Activity retries visible in a moment.

### `MoneyTransferWorkflow.java`

The interface needs a `@SignalMethod` alongside the `@WorkflowMethod`:

```java
@SignalMethod
void approve(boolean approved);
```

### `MoneyTransferWorkflowImpl.java`

- Build the Activity stub with a `startToCloseTimeout`.
- Execute `withdraw`.
- Block on `Workflow.await(() -> approvalReceived)`.
- Execute `deposit` on approval, `refund` on rejection.
- Record the decision in the `approve` handler.

> [!IMPORTANT]
> One boolean is not enough. `approved` tells you *what* the human said;
> `approvalReceived` tells you *that* they said something. Waiting on
> `approved` alone means a rejection never wakes the Workflow up.

### `StartWorker.java` and `StartWorkflow.java`

Register both implementations on the Worker, then send the `approve` Signal
from the client after a short sleep.

# Start the Worker

Click the [button label="Worker" background="#444CE7"](tab-1):

```bash,run
./gradlew execute -PmainClass=com.temporal.training.exercise2.StartWorker -q --console=plain
```

# Run It

Click the [button label="Terminal" background="#444CE7"](tab-2):

```bash,run
./gradlew execute -PmainClass=com.temporal.training.exercise2.StartWorkflow -q --console=plain
```

The starter waits three seconds, then sends the approval. Watch the Worker tab.
If an Activity hit its simulated failure you will see the exception, then the
same Activity running again. You did not write that retry.

# Signal It Yourself

The Signal does not have to come from the starter. Any client that can reach
the server can move the Workflow along, including the CLI. From the
[button label="Terminal" background="#444CE7"](tab-2):

```bash,run
WORKFLOW_ID=$(temporal workflow list --query 'TaskQueue="money-transfer-signals-task-queue"' --limit 1 -o json | jq -r '.[0].execution.workflowId')
echo "$WORKFLOW_ID"

temporal workflow signal \
  --workflow-id "$WORKFLOW_ID" \
  --name approve \
  --input true
```

This particular run already got its approval from the starter three seconds in,
so the Signal lands on a Workflow that has finished. Exercise 7 uses this same
command against a Workflow that is genuinely waiting.

# Look at the Wait

Click the [button label="Temporal UI" background="#444CE7"](tab-3). Open your
Workflow, then the **Event History**. Find `WorkflowExecutionSignaled`. Above
it, the Workflow was doing nothing at all, and it was not consuming a Worker
while it did.

Click **Check** when the transfer completes.

# Key Takeaways

- `@SignalMethod` handlers mutate Workflow state from outside.
- `Workflow.await(supplier)` blocks on that state without burning a thread.
- Failed Activities retry on their own. The default retry policy is already on.
- The Signal is recorded in history, so replay makes the same decision.
