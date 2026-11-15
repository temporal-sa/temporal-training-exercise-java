---
slug: activity-summaries
type: challenge
title: 'Exercise 5: Activity Summaries and Metadata'
teaser: Label every Activity and the Workflow itself, so the Web UI reads like a sentence.
notes:
- type: text
  contents: |-
    # It is 3am. Which of these forty Activities moved the money?

    The Event History says `withdraw`, `deposit`, `withdraw`, `deposit`.
    It does not say which account, or which direction.

    A one-line summary on each call fixes that, and costs nothing at
    runtime.
- type: text
  contents: |-
    # Did you know?

    Summaries are user metadata. They ride along on the events, show up in
    the Web UI and the CLI, and never change how the Workflow executes or
    replays.
tabs:
- title: Code Editor
  type: code
  hostname: workshop
  path: /root/workshop/src/main/java/com/temporal/training/exercise5
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
  path: /root/workshop/src/main/java/com/temporal/training/solution5
- title: Network Control Panel
  type: service
  hostname: workshop
  path: /
  port: 5000
difficulty: basic
timelimit: 1800
enhanced_loading: null
---

# Activity Summaries and Metadata

The Workflow already works. This exercise makes it legible to whoever debugs
it at 3am.

# The Code

Open the [button label="Code Editor" background="#444CE7"](tab-0). Two files
carry TODOs.

### `MoneyTransferWorkflowImpl.java`

Four TODOs, all in the `ActivityFactory` inner class. Summaries are set on
`ActivityOptions`, so each Activity gets its own stub with its own summary.
Give `createActivity` a `summary` parameter and pass it to `setSummary`:

```java
private static BankingActivities createActivity(String summary) {
    return Workflow.newActivityStub(
        BankingActivities.class,
        ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(5))
            .setSummary(summary)
            .build()
    );
}
```

Then have the three factory methods build their summary from the account:

| Method | Summary |
|--------|---------|
| `createWithdrawActivity` | `"Withdrawing funds from account " + accountId` |
| `createDepositActivity` | `"Depositing funds to account " + accountId` |
| `createRefundActivity` | `"Refunding funds to account " + accountId` |

`transfer` already calls the right factory method before each Activity.

### `StartWorkflow.java`

One TODO for a summary on the execution itself. Build a string from the
request and pass it to `setStaticSummary` on the `WorkflowOptions` builder:

```java
String staticSummaryDetails =
    "Money transfer workflow for " + request.fromAccount() + " to " + request.toAccount();
```

```java
.setStaticSummary(staticSummaryDetails)
```

# Run It

Click the [button label="Worker" background="#444CE7"](tab-1):

```bash,run
./gradlew -q execute -PmainClass=com.temporal.training.exercise5.StartWorker
```

Click the [button label="Terminal" background="#444CE7"](tab-2):

```bash,run
./gradlew -q execute -PmainClass=com.temporal.training.exercise5.StartWorkflow
```

# See the Difference

Click the [button label="Temporal UI" background="#444CE7"](tab-3). Click your
Workflow at the top of the list. Refresh if it is not there.

The static summary shows at the top of the execution page. Open the
**Event History** and look at the Activity rows: each one now says which
account it touched and which direction the money went. Compare that against
an exercise 4 execution further down the list.

The CLI sees it too:

```bash,run
WORKFLOW_ID=$(temporal workflow list --query 'TaskQueue="MoneyTransferTaskQueue"' --limit 1 -o json | jq -r '.[0].execution.workflowId')
temporal workflow describe --workflow-id "$WORKFLOW_ID" -o json | jq '.executionConfig.userMetadata'
```

Click **Check** when the summaries show up.

# Key Takeaways

- `ActivityOptions.setSummary()` labels one Activity in the UI. One stub per
  distinct summary.
- `WorkflowOptions.setStaticSummary()` labels the whole execution.
- Both are user metadata. They do not affect Workflow logic or replay.
- Keep summaries short and specific. They are read at a glance, under pressure.
