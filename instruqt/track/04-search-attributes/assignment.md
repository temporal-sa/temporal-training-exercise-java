---
slug: search-attributes
type: challenge
title: 'Exercise 4: Money Transfer with Search Attributes'
teaser: Tag each transfer with its account so you can find every Workflow for a customer.
notes:
- type: text
  contents: |-
    # Which transfers touched account-123?

    A customer calls. You have ten thousand running Workflows and a
    Workflow ID scheme that does not include the account number.

    You need to filter on business data, not on IDs.
- type: text
  contents: |-
    # Did you know?

    Search attributes are indexed by Temporal's visibility store, separately
    from the event history. They are for finding Workflows, not for
    carrying state. Keep them small and keep the set of names stable.
tabs:
- title: Code Editor
  type: code
  hostname: workshop
  path: /root/workshop/src/main/java/com/temporal/training/exercise4
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
  path: /root/workshop/src/main/java/com/temporal/training/solution4
- title: Network Control Panel
  type: service
  hostname: workshop
  path: /
  port: 5000
difficulty: basic
timelimit: 1800
enhanced_loading: null
---

# Money Transfer with Search Attributes

One file. One import and one call. A Workflow you can find by account number.

# The Code

Open the [button label="Code Editor" background="#444CE7"](tab-0) and open
`MoneyTransferWorkflowImpl.java`. It has two TODOs.

At the top, import the typed key:

```java
import io.temporal.common.SearchAttributeKey;
```

Near the top of `transfer`, before the first Activity, tag the execution with
the source account:

```java
Workflow.upsertTypedSearchAttributes(
    SearchAttributeKey.forKeyword("AccountId").valueSet(request.fromAccount())
);
```

Doing it before the first Activity means the tag is on the execution from the
start, so the Workflow shows up in a filtered search even while it is still
waiting for approval.

> [!IMPORTANT]
> `AccountId` is already registered on the dev server in this sandbox, which
> was started with `--search-attribute AccountId=Text`. Upserting an
> unregistered attribute fails the Workflow Task. That shows up as a Workflow
> that starts and then goes nowhere, with the error in the Worker tab.

# Run It

Click the [button label="Worker" background="#444CE7"](tab-1):

```bash,run
./gradlew -q execute -PmainClass=com.temporal.training.exercise4.StartWorker
```

Click the [button label="Terminal" background="#444CE7"](tab-2):

```bash,run
./gradlew -q execute -PmainClass=com.temporal.training.exercise4.StartWorkflow
```

The starter uses `account-123` as the source account.

# Find It By Account

From the [button label="Terminal" background="#444CE7"](tab-2):

```bash,run
temporal workflow list --query 'AccountId="account-123"'
```

Now filter on an account that never sent anything:

```bash,run
temporal workflow list --query 'AccountId="account-999"'
```

Empty. The filter is doing real work.

# See It In the UI

Click the [button label="Temporal UI" background="#444CE7"](tab-3). Click your
Workflow, then look at the summary panel. `AccountId` is listed there with its
value.

Back on the Workflows list, put this in the search box:

```bash,nocopy
AccountId="account-123"
```

Click **Check** when the query returns your Workflow.

# Key Takeaways

- `Workflow.upsertTypedSearchAttributes()` tags an execution with business data.
- `SearchAttributeKey` gives you a typed key, so the compiler checks the value type.
- Custom attributes must be registered on the cluster first. The dev server
  takes `--search-attribute Name=Type`.
- Search attributes are for finding Workflows, not for storing Workflow state.
  Keep the payload small.
- The same query syntax works in the CLI, the Web UI, and the client API.
