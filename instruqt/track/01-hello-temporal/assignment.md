---
slug: hello-temporal
type: challenge
title: 'Exercise 1: Hello Temporal'
teaser: Write your first Workflow, Activity and Worker. Run it and read the event
  history.
notes:
- type: text
  contents: |-
    # What does a Workflow actually run on?

    You write a method. You never call it. Something else picks it up,
    runs it, and records every step.

    That something is a Worker, and it is polling a task queue right now
    waiting for code you have not written yet.
- type: text
  contents: |-
    # Already running in your sandbox

    A Temporal dev server on 127.0.0.1:7233, with the Web UI on port 8233.
    The AccountId search attribute is already registered, so exercise 4
    works when you get there.

    JDK 17 and Gradle are installed, every dependency is downloaded, and
    all seven exercises are already compiled once. Your first build only
    recompiles what you change.

    The Solution tab has finished code for every exercise. Use it when you
    are stuck, not before.
tabs:
- title: Code Editor
  type: code
  hostname: workshop
  path: /root/workshop/src/main/java/com/temporal/training/exercise1
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
  path: /root/workshop/src/main/java/com/temporal/training/solution1
- title: Network Control Panel
  type: service
  hostname: workshop
  path: /
  port: 5000
difficulty: basic
timelimit: 1800
enhanced_loading: null
---

# Hello Temporal

Three files. Six TODOs. A greeting that survives anything.

> [!NOTE]
> **Sandbox Notes:**
> - [button label="Code Editor" background="#444CE7"](tab-0) edits the files in `exercise1/`
> - [button label="Worker" background="#444CE7"](tab-1) runs the Worker
> - [button label="Terminal" background="#444CE7"](tab-2) starts Workflows
> - [button label="Temporal UI" background="#444CE7"](tab-3) is the Temporal Web UI
> - [button label="Solution" background="#444CE7"](tab-4) has the finished code
> - [button label="Network Control Panel" background="#444CE7"](tab-5) toggles outbound HTTP
>
> The blue buttons above are clickable. Click any to jump to that tab.
> Instruqt saves editor changes for you. There is no save step.

# The Code

Open the [button label="Code Editor" background="#444CE7"](tab-0). The two
interfaces, `GreetingWorkflow` and `GreetingActivity`, are already annotated.
Three implementation files carry the TODOs:

| File | What goes in it |
|------|-----------------|
| `GreetingActivityImpl.java` | Return `"Hello, " + name + "!"` |
| `GreetingWorkflowImpl.java` | Build the Activity stub with `Workflow.newActivityStub` and a `startToCloseTimeout`, then call it |
| `StartWorker.java` | Register the Workflow type and the Activity instance on `hello-task-queue` |

The stub is the part that is new if you have only written plain Java:

```java
private final GreetingActivity activity = Workflow.newActivityStub(
    GreetingActivity.class,
    ActivityOptions.newBuilder()
        .setStartToCloseTimeout(Duration.ofSeconds(10))
        .build()
);
```

Calling `activity.createGreeting(name)` looks like a method call. It is
actually a request to Temporal to schedule an Activity Task, which a Worker
picks up and runs.

`StartWorkflow.java` is already done. Read it: it connects a client and calls
the Workflow through a stub built from the same interface.

# Start the Worker

Click the [button label="Worker" background="#444CE7"](tab-1) terminal:

```bash,run
./gradlew -q execute -PmainClass=com.temporal.training.exercise1.StartWorker
```

It stays in the foreground and polls. Leave it running.

> [!NOTE]
> Gradle compiles before it runs, so a typo shows up here as a compile error.
> Fix it in the editor and run the command again.

# Run It

Click the [button label="Terminal" background="#444CE7"](tab-2):

```bash,run
./gradlew -q execute -PmainClass=com.temporal.training.exercise1.StartWorkflow
```

```bash,nocopy
Hello, Temporal!
```

If the starter hangs, check the Worker tab. A `NullPointerException` there
means the Activity stub is still `null`.

> [!NOTE]
> The Worker and the starter are two separate Gradle builds. If you start the
> second while the first is still compiling, one can fail with
> `NoSuchFileException` or `Cannot access output property`. Nothing is wrong
> with your code. Run the command again.
>
> All seven exercises compile as one project, so a compile error in another
> exercise's package blocks this one too. `git checkout -- <path>` restores
> any file to how it shipped.

# Read the History

Click the [button label="Temporal UI" background="#444CE7"](tab-3). Click
`greeting-workflow` at the top of the list. Hit refresh if it is not there yet.

Open the **Event History**. Look for `ActivityTaskScheduled`,
`ActivityTaskStarted` and `ActivityTaskCompleted`. Those three events are the
whole point. Temporal wrote down that the Activity ran and what it returned.
Replay reads that back instead of calling the Activity again.

Click **Check** when the greeting prints.

# Key Takeaways

- `@WorkflowInterface` and `@WorkflowMethod` mark the Workflow and its entry point.
- `@ActivityInterface` marks the Activities. The Workflow calls them through a
  stub from `Workflow.newActivityStub`, never directly.
- Activities do the side effects. Workflows orchestrate and stay deterministic.
- A Worker only runs what you register with it, on the task queue you name.
- Every Activity result lands in the event history, which is what makes replay work.
