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

    Gradle is warm too. The wrapper, the SDK and every dependency were
    downloaded when the sandbox was built, so the first build is a
    compile, not a download.
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
---

# Hello Temporal

Three files. Six TODOs. A greeting that survives anything.

> [!NOTE]
> **Sandbox Notes:**
> - [button label="Code Editor" background="#444CE7"](tab-0) edits the files in `exercise1`
> - [button label="Worker" background="#444CE7"](tab-1) runs the Worker
> - [button label="Terminal" background="#444CE7"](tab-2) starts Workflows
> - [button label="Temporal UI" background="#444CE7"](tab-3) is the Temporal Web UI
> - [button label="Solution" background="#444CE7"](tab-4) has the finished code
> - [button label="Network Control Panel" background="#444CE7"](tab-5) toggles outbound HTTP
>
> The blue buttons above are clickable. Click any to jump to that tab.
> Instruqt saves editor changes for you. There is no save step.

# The Code

Open the [button label="Code Editor" background="#444CE7"](tab-0). Three files
carry the TODOs. The two interfaces, `GreetingWorkflow.java` and
`GreetingActivity.java`, are already written.

| File | What goes in it |
|------|-----------------|
| `GreetingActivityImpl.java` | Return `"Hello, " + name + "!"` |
| `GreetingWorkflowImpl.java` | Build the Activity stub, then call `createGreeting` |
| `StartWorker.java` | Register the Workflow type and the Activity implementation |

The Activity is where side effects live. The Workflow only orchestrates. The
Worker is the process that runs both.

Two things about the Workflow stub that are easy to get wrong:

```java
private final GreetingActivity activity = Workflow.newActivityStub(
    GreetingActivity.class,
    ActivityOptions.newBuilder()
        .setStartToCloseTimeout(Duration.ofSeconds(10))
        .build()
);
```

`Workflow.newActivityStub` is a proxy. Calling a method on it does not run the
Activity in your Workflow thread — it schedules an Activity Task and blocks
until a Worker somewhere returns a result. And an Activity with no timeout is
rejected outright, which is why `setStartToCloseTimeout` is not optional.

Registration is the other half:

```java
worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);
worker.registerActivitiesImplementations(new GreetingActivityImpl());
```

The Workflow goes in as a **class**, because the Worker constructs a fresh
instance per execution. The Activity goes in as an **instance**, because it is
shared across all of them.

# Start the Worker

Click the [button label="Worker" background="#444CE7"](tab-1):

```bash,run
./gradlew execute -PmainClass=com.temporal.training.exercise1.StartWorker -q --console=plain
```

It compiles, prints `Worker started for task queue: hello-task-queue`, and then
sits there polling. Leave it running. Give the Gradle daemon a few seconds on
the first run.

# Run It

Click the [button label="Terminal" background="#444CE7"](tab-2):

```bash,run
./gradlew execute -PmainClass=com.temporal.training.exercise1.StartWorkflow -q --console=plain
```

```bash,nocopy
Hello, Temporal!
```

> [!NOTE]
> All fifteen packages are one Gradle module, so every build compiles every
> exercise. A syntax error in exercise 5 will stop exercise 1 from running. If
> a command fails with a compile error in a package you have not opened yet,
> that is why.

# Read the History

Click the [button label="Temporal UI" background="#444CE7"](tab-3). Click your
Workflow at the top of the list. Hit refresh if it is not there yet.

Open the **Event History**. Look for `ActivityTaskScheduled`,
`ActivityTaskStarted` and `ActivityTaskCompleted`. Those three events are the
whole point. Temporal wrote down that the Activity ran and what it returned.
Replay reads that back instead of calling the Activity again.

Click **Check** when the greeting prints.

# Key Takeaways

- `@WorkflowInterface` marks the interface, `@WorkflowMethod` marks the entry point.
- Activities do the side effects. Workflows orchestrate and stay deterministic.
- Activity stubs need a timeout. There is no default.
- A Worker only runs what you register with it, on the task queue you name.
- Every Activity result lands in the event history, which is what makes replay work.
