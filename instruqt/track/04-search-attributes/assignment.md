---
slug: search-attributes
id: nd3vppca8zk8
type: challenge
title: 'Exercise 4: Money Transfer with Search Attributes'
teaser: Tag a Workflow with a business identifier, then find it by that identifier
  instead of its Workflow ID.
notes:
- type: text
  contents: |-
    # Support asks about account-123. You have 40,000 Workflows. Now what?

    You do not have the Workflow ID. You have an account number.

    Two lines of code make that account number queryable across every
    execution the cluster has ever seen.
- type: text
  contents: |-
    # Two lines. Fifteen minutes.

    The shortest exercise in the workshop. Custom search attributes have to
    be registered on the cluster before a Workflow can set one, so
    `AccountId=Text` is already registered on your dev server.

    Try it without that registration in production and the Workflow Task
    fails outright.
tabs:
- id: zakiz90exwro
  title: Code Editor
  type: code
  hostname: workshop
  path: /root/workshop/src/main/java/com/temporal/training/exercise4
- id: j1koaps0orwg
  title: Worker
  type: terminal
  hostname: workshop
  workdir: /root/workshop
- id: 830grt6wmfnk
  title: Terminal
  type: terminal
  hostname: workshop
  workdir: /root/workshop
- id: hb7tbqyx9yfg
  title: Temporal UI
  type: service
  hostname: workshop
  path: /
  port: 8233
- id: v0spfbhvhqxn
  title: Solution
  type: code
  hostname: workshop
  path: /root/workshop/src/main/java/com/temporal/training/solution4
- id: nwfk0loupm6x
  title: Network Control Panel
  type: service
  hostname: workshop
  path: /
  port: 5000
difficulty: basic
timelimit: 1800
enhanced_loading: null
---

# Money Transfer with Search Attributes

Two TODOs, both in one file. A Workflow you can find by account number.

# The Code

Open the [button label="Code Editor" background="#444CE7"](tab-0) and find the
two TODOs in `MoneyTransferWorkflowImpl.java` — one for the import, one near
the top of `transfer`.

```java
import io.temporal.common.SearchAttributeKey;

Workflow.upsertTypedSearchAttributes(
    SearchAttributeKey.forKeyword("AccountId").valueSet(request.fromAccount())
);
```

Do it before the first Activity, so the tag is on the execution from the start.

> [!IMPORTANT]
> `AccountId=Text` is already registered on the dev server in this sandbox.
> Setting an unregistered attribute fails the Workflow Task, which shows up
> as a Workflow that starts and then goes nowhere.

# Run It

Click the [button label="Worker" background="#444CE7"](tab-1):

```bash,run
./gradlew execute -PmainClass=com.temporal.training.exercise4.StartWorker -q --console=plain
```

Click the [button label="Terminal" background="#444CE7"](tab-2):

```bash,run
./gradlew execute -PmainClass=com.temporal.training.exercise4.StartWorkflow -q --console=plain
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
- `SearchAttributeKey` gives you a typed handle: `forKeyword`, `forText`,
  `forLong`, `forDouble`, `forBoolean`, `forInstant`.
- Custom attributes must be registered on the cluster first. The dev server
  takes `--search-attribute Name=Type`.
- Search attributes are for finding Workflows, not for storing Workflow state.
  Keep the payload small.
- The same query syntax works in the CLI, the Web UI, and the client API.
