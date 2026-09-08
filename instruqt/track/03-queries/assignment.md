---
slug: queries
type: challenge
title: 'Exercise 3: Money Transfer with Queries'
teaser: Ask a running Workflow what it is doing right now, without touching a database.
notes:
- type: text
  contents: |-
    # Where do you look to find out what a Workflow is doing?

    Not a status table. Not a log aggregator. Not a metrics dashboard.

    The Workflow itself holds the state, in ordinary Java fields. A Query
    reads those fields out of a running execution.
- type: text
  contents: |-
    # Signals in, Queries out

    A Signal changes Workflow state and gets written to history. A Query
    reads state and is never written to history at all.

    That is why a Query handler must not mutate anything and must not
    call an Activity. Break that rule and replay stops matching.
tabs:
- title: Code Editor
  type: code
  hostname: workshop
  path: /root/workshop/src/main/java/com/temporal/training/exercise3
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
  path: /root/workshop/src/main/java/com/temporal/training/solution3
- title: Network Control Panel
  type: service
  hostname: workshop
  path: /
  port: 5000
difficulty: basic
timelimit: 1800
---

# Money Transfer with Queries

Same transfer as exercise 2. This time it can answer questions while it runs.

# The Code

Open the [button label="Code Editor" background="#444CE7"](tab-0). The
Activities and the Worker are already written. Three files carry TODOs.

### `MoneyTransferWorkflow.java`

Add the Query to the interface:

```java
@QueryMethod
TransferStatus getStatus();
```

### `MoneyTransferWorkflowImpl.java`

`TransferStatus` is an enum, and the transfer has to move through it:
`PENDING` on entry, `APPROVED` once the Signal says yes, `COMPLETED` after the
deposit, `CANCELLED` after a refund. Then return that field from `getStatus()`.

> [!WARNING]
> A Query handler runs during replay. Do not mutate state in one, do not
> execute an Activity from one, and do not sleep in one. Read a field and
> return it.

### `StartWorkflow.java`

Call `getStatus()` on the Workflow stub between the start and the approval, so
you can watch the state move.

# Start the Worker

Click the [button label="Worker" background="#444CE7"](tab-1):

```bash,run
./gradlew execute -PmainClass=com.temporal.training.exercise3.StartWorker -q --console=plain
```

# Run It

Click the [button label="Terminal" background="#444CE7"](tab-2):

```bash,run
./gradlew execute -PmainClass=com.temporal.training.exercise3.StartWorkflow -q --console=plain
```

The status should read `PENDING` before the approval goes out and `COMPLETED`
after.

# Query From Outside

The starter is not special. Ask the most recent Workflow yourself, from the
[button label="Terminal" background="#444CE7"](tab-2):

```bash,run
WORKFLOW_ID=$(temporal workflow list --query 'TaskQueue="money-transfer-queries-task-queue"' --limit 1 -o json | jq -r '.[0].execution.workflowId')
echo "$WORKFLOW_ID"
temporal workflow query --workflow-id "$WORKFLOW_ID" --type getStatus
```

`temporal workflow describe` lists the handlers a Workflow exposes if you
forget the name.

> [!NOTE]
> A Query is answered by a Worker, not by the server. Leave the Worker running
> in the [button label="Worker" background="#444CE7"](tab-1) tab or you get
> `no poller seen for task queue recently`.

# Key Takeaways

- `@QueryMethod` exposes Workflow state to any client. Read-only.
- Query results never enter the event history, which is why mutation is banned.
- The state lives in plain Java fields. Temporal rebuilds them by replay.
- Signals write, Queries read. Reach for a Signal when you need to change something.

Click **Check** when the transfer completes.
