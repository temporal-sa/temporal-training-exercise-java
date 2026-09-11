---
slug: activity-summaries
id: 6xxdtfxnmy6f
type: challenge
title: 'Exercise 5: Activity Summaries and Metadata'
teaser: Make the event history readable. Attach a one-line summary to every Activity
  and to the execution itself.
notes:
- type: text
  contents: |-
    # Three Activities named withdraw, deposit and refund. Which account?

    You are on a call. Someone shares an event history. It says
    `withdraw`, `deposit`, `refund`.

    It does not say whose money, or how much. That detail was in the
    payload, and nobody wants to click into three payloads on a call.
- type: text
  contents: |-
    # Did you know?

    Activity summaries are user metadata. They render in the Web UI beside
    the event, they cost nothing at runtime, and they are not part of
    Workflow logic. Changing a summary string does not break replay.
tabs:
- id: dvpacgx4npcv
  title: Code Editor
  type: code
  hostname: workshop
  path: /root/workshop/src/main/java/com/temporal/training/exercise5
- id: 7nwp9lqhscyi
  title: Worker
  type: terminal
  hostname: workshop
  workdir: /root/workshop
- id: 0hzqoikjyrfr
  title: Terminal
  type: terminal
  hostname: workshop
  workdir: /root/workshop
- id: f9n6tzjdsa80
  title: Temporal UI
  type: service
  hostname: workshop
  path: /
  port: 8233
- id: xbyxlqgwnatw
  title: Solution
  type: code
  hostname: workshop
  path: /root/workshop/src/main/java/com/temporal/training/solution5
- id: 9rsh2qzy4ofr
  title: Network Control Panel
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

Open the [button label="Code Editor" background="#444CE7"](tab-0).

### `MoneyTransferWorkflowImpl.java`

In Java the summary lives on `ActivityOptions`, which means one stub per
summary rather than one stub for the whole Workflow. That is what the
`ActivityFactory` inner class is for. Four TODOs: thread a `String summary`
through `createActivity`, then give each of the three factory methods its own
text.

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

static BankingActivities createWithdrawActivity(String accountId) {
    return createActivity("Withdrawing funds from account " + accountId);
}
```

Do the same for the deposit and refund factories.

### `StartWorkflow.java`

One TODO. Pass a summary for the execution itself on `WorkflowOptions`:

```java
.setStaticSummary("Money transfer workflow for " + request.fromAccount()
                  + " to " + request.toAccount())
```

> [!NOTE]
> `setStaticSummary` is the metadata field the UI renders as the execution's
> title. It is not the same thing as a memo: `setMemo` is arbitrary key-value
> data that the UI shows in its own panel.

# Run It

Click the [button label="Worker" background="#444CE7"](tab-1):

```bash,run
./gradlew execute -PmainClass=com.temporal.training.exercise5.StartWorker -q --console=plain
```

Click the [button label="Terminal" background="#444CE7"](tab-2):

```bash,run
./gradlew execute -PmainClass=com.temporal.training.exercise5.StartWorkflow -q --console=plain
```

# See the Difference

Click the [button label="Temporal UI" background="#444CE7"](tab-3). Click your
Workflow at the top of the list. Refresh if it is not there.

The static summary shows next to the execution name. Open the **Event History**
and look at the Activity rows: each one now says which account it touched and
which direction the money went. Compare that against the exercise 2 execution
further down the list.

Click **Check** when the summaries show up.

# Key Takeaways

- `ActivityOptions.setSummary()` labels one Activity in the UI. It is per-stub,
  so distinct summaries mean distinct stubs.
- `WorkflowOptions.setStaticSummary()` labels the whole execution.
- Both are user metadata. They do not affect Workflow logic or replay.
- Keep summaries short and specific. They are read at a glance, under pressure.
