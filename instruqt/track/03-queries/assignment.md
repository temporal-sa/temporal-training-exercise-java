---
slug: queries
type: challenge
title: 'Exercise 3: Money Transfer with Queries'
teaser: Same transfer, now it can tell you what state it is in while it runs.
notes:
- type: text
  contents: |-
    # Where is my money right now?

    The transfer is halfway done. Support wants to know whether it is
    waiting on approval or already depositing.

    You could write the status to a database on every step. Or you could
    just ask the Workflow.
- type: text
  contents: |-
    # Did you know?

    A Query never enters the event history. The Worker answers it from the
    Workflow's in-memory state, replaying the history first if the Workflow
    was not cached. That is why a Query handler must never change anything:
    nothing it did would be recorded, so replay could not reproduce it.
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
enhanced_loading: null
---

# Money Transfer with Queries

Same transfer as exercise 2. This time it can answer questions while it runs.

# The Code

Open the [button label="Code Editor" background="#444CE7"](tab-0). The
Activities, the Worker and the approval Signal are already done. Three files
carry TODOs.

### `MoneyTransferWorkflow.java`

Put `@QueryMethod` on `TransferStatus getStatus()`.

### `MoneyTransferWorkflowImpl.java`

The Workflow keeps a `status` field that starts at `PENDING`. Move it along as
the transfer runs, then return it from `getStatus()`:

| When | Set `status` to |
|------|-----------------|
| Approval received, before the deposit | `TransferStatus.APPROVED` |
| Deposit finished | `TransferStatus.COMPLETED` |
| Rejected and refunded | `TransferStatus.CANCELLED` |

Optionally, wrap the body of `transfer` in a `try`/`catch` that sets
`TransferStatus.FAILED` and rethrows. The Solution tab does this.

> [!WARNING]
> A Query handler can run during replay. Do not mutate state in one, do not
> call an Activity from one, and do not sleep in one. Read a field and return
> it.

### `StartWorkflow.java`

Uncomment the three `workflow.getStatus()` calls so you can see the state move:
once before the approval, once after, and once after the result.

# Start the Worker

Click the [button label="Worker" background="#444CE7"](tab-1):

```bash,run
./gradlew -q execute -PmainClass=com.temporal.training.exercise3.StartWorker
```

# Run It

Click the [button label="Terminal" background="#444CE7"](tab-2):

```bash,run
./gradlew -q execute -PmainClass=com.temporal.training.exercise3.StartWorkflow
```

```bash,nocopy
Starting money transfer workflow...
Current status: PENDING
Sending approval signal...
Status after approval: APPROVED
Transfer result: Transfer completed successfully
Final status: COMPLETED
```

The middle line varies from run to run. It can read `PENDING`, `APPROVED` or
`COMPLETED`, because the Query goes out right behind the Signal and races both
the Signal handler and the deposit.

# Query From Outside

The starter is not special. Ask the most recent Workflow yourself, from the
[button label="Terminal" background="#444CE7"](tab-2):

```bash,run
WORKFLOW_ID=$(temporal workflow list --query 'TaskQueue="MoneyTransferTaskQueue"' --limit 1 -o json | jq -r '.[0].execution.workflowId')
echo "$WORKFLOW_ID"
temporal workflow query --workflow-id "$WORKFLOW_ID" --name getStatus
```

In Java the Query name defaults to the method name, `getStatus`. It works on a
completed Workflow too: the Worker replays the history to rebuild `status`,
then reads it.

> [!NOTE]
> A Query is answered by a Worker, not by the server. Leave the Worker running
> in the [button label="Worker" background="#444CE7"](tab-1) tab or the Query
> times out waiting for one.

Click **Check** when the transfer completes.

# Key Takeaways

- `@QueryMethod` exposes Workflow state to any client. Read-only.
- Query results never enter the event history, which is why mutation is banned.
- The state lives in plain Java fields. Temporal rebuilds them by replay.
- Signals write, Queries read. Reach for a Signal when you need to change something.
