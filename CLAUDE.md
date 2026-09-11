# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repo is

A progressive Temporal Java SDK training course. Seven exercises, each a self-contained package
under `src/main/java/com/temporal/training/`:

- `exerciseN/` — student-facing stubs. Code is deliberately incomplete, marked with `// TODO:` comments.
- `solutionN/` — the completed reference for the same exercise.
- `solutionfinal/` — a standalone "polished app" variant, not exercise 8 (see below).

The same class names (`MoneyTransferWorkflow`, `BankingActivities`, `TransferRequest`,
`TransferStatus`, `StartWorker`, `StartWorkflow`) are duplicated in every package by design, so each
exercise stands alone and evolves independently. There is no shared/common module — do not try to
factor the duplication away.

Everything lives in one Gradle module, so **`./gradlew build` compiles all 15+ packages at once**: a
change to any exercise must keep every other package compiling.

## Commands

```bash
# Temporal dev server — the AccountId search attribute is required by exercises 4-7
temporal server start-dev --search-attribute AccountId=Text

./gradlew build          # compile + run tests
./gradlew test           # tests only
./gradlew test --tests "com.temporal.training.solution6.MoneyTransferWorkflowTest"   # single test class

# Run any main class (the `execute` task in build.gradle)
./gradlew execute -PmainClass=com.temporal.training.solution2.StartWorker
./gradlew execute -PmainClass=com.temporal.training.greeting.Starter -Pargs="Mark"
```

Running an exercise always needs two terminals plus the dev server: `StartWorker` in one,
`StartWorkflow` in the other. Java 17 toolchain, Gradle 9.7.1; `temporal-sdk` and `temporal-testing`
both 1.38.0 — keep them on the same version, since a skew pulls two copies of the SDK onto the test
classpath.

## Task queues differ per exercise

The worker and the starter must agree. Every exercise has its **own** queue, and
that is load-bearing, not cosmetic — see below.

| Package | Task queue |
|---|---|
| `exercise1`, `solution1` | `hello-task-queue` (via `StartWorker.TASK_QUEUE`) |
| `exercise2`, `solution2` | `money-transfer-signals-task-queue` |
| `exercise3`, `solution3` | `money-transfer-queries-task-queue` |
| `exercise4`, `solution4` | `money-transfer-search-attributes-task-queue` |
| `exercise5`, `solution5` | `money-transfer-summaries-task-queue` |
| `exercise6`, `solution6` | `money-transfer-testing-task-queue` |
| `exercise7`, `solution7` | `money-transfer-task-queue` (hardcoded string in both files) |
| `solutionfinal` | `money-transfer-queue` |

Exercises 2–6 used to share `MoneyTransferTaskQueue`, and every one of them
registers a different `MoneyTransferWorkflowImpl`. A worker left running from an
earlier exercise picks up tasks it cannot replay, which surfaces as
`Unknown query type: getStatus, knownTypes=[]` on a query, or a
`NonDeterministicException` naming an event the current implementation never
emits. **Do not collapse these names back together.** Exercises 2–6 all take
`StartWorker.TASK_QUEUE`, so each rename is one line in one file.

## Exercise arc

1. **1** — workflow/activity/worker basics (`greet`).
2. **2** — money transfer: withdraw/deposit/refund, `@SignalMethod approve`, `Workflow.await()`, compensation.
3. **3** — `@QueryMethod getStatus()` and a `TransferStatus` enum threaded through execution.
4. **4** — `Workflow.upsertTypedSearchAttributes` with `SearchAttributeKey.forKeyword("AccountId")`.
5. **5** — user metadata: `ActivityOptions.setSummary(...)` (one stub per activity call, see the
   `ActivityFactory` in `solution5`) and `WorkflowOptions.setStaticSummary(...)`.
6. **6** — testing (see below).
7. **7** — manual retry: activities throw `ApplicationFailure.newNonRetryableFailure` when an account
   id contains `"invalid"`; the workflow wraps calls in an `executeWithRetry` loop that flips status to
   `RETRYING` and `Workflow.await(() -> retryRequested)`; a `retry(RetryUpdate)` signal patches the
   request via `TransferRequest.withFromAccount/withToAccount/withAmount` copy methods.
   `StartWorkflow` intentionally passes `invalid-account-456` to trigger the path, and the workflow id
   is `money-transfer-<random UUID>` — look it up before signalling (the root README's
   `--workflow-id money-transfer-workflow` example is stale).

`solutionfinal` is a different design, not a continuation of `solution7`: subpackages
(`workflow/`, `activity/`, `model/`), a `void` workflow method, separate `approve()`/`reject()`
signals, an approval timeout via `Workflow.await(Duration, ...)`, `RetryOptions` with 3 attempts, and
`Async.procedure` compensation.

## Testing conventions

Tests are **JUnit 5**, not 4. `build.gradle` declares only `org.junit.jupiter:junit-jupiter` and
calls `useJUnitPlatform()` — a test written with JUnit 4 annotations (`org.junit.Test`, `@Rule`)
will silently not run. Use `org.junit.jupiter.api.Test` / `@RegisterExtension` and assertions from
`org.junit.jupiter.api.Assertions`.

The `TestWorkflowExtension` setup in `solution6/MoneyTransferWorkflowTest.java` is the canonical
pattern:

- `@RegisterExtension` on a `public static final` field. Search attributes go on the builder
  (`registerSearchAttribute("AccountId", IndexedValueType.INDEXED_VALUE_TYPE_TEXT)`) — they must
  exist before the environment is created, and there is no post-construction hook for them.
  Drop that line and the workflow task fails in a loop and the test **hangs** rather than failing.
- `setDoNotStart(true)`, then register the mock activities on the injected `Worker`, then
  `testEnv.start()`.
- Test methods take their dependencies as parameters — `TestWorkflowEnvironment`, `Worker`, and the
  workflow interface itself (the extension injects a stub bound to that test's task queue). Each
  test gets a fresh environment and the extension closes it, so there is no teardown method.
- Mock activity interfaces with `Mockito.mock(X.class, withSettings().withoutAnnotations())` —
  without `withoutAnnotations()` the Temporal annotations break the mock.
- Drive signals with `testEnv.registerDelayedCallback(...)` for time skipping; never sleep.

`exercise6`'s test methods are TODO stubs with empty bodies, so they pass vacuously — real coverage
lives only in the `solution6` test, which is also the only thing CI runs
(`.github/workflows/test-solution6.yml`).

## When editing exercises

- Change an `exerciseN` package and the matching `solutionN` package together; the solution is the
  answer key for the exercise's TODOs.
- Each exercise directory carries a `README.md` (the assignment) and usually a `SOLUTION.md` (code
  snippets of the answer). `exercise6` folds its snippets into the README instead. Update those docs
  alongside code, and keep the root `README.md` exercise list and run commands in sync.
- `com.temporal.training.greeting` is plain Java with no Temporal involvement — a scratch demo, not
  part of the exercise sequence.
- `caches/`, `daemon/`, `jdks/`, `native/`, `wrapper/` at the repo root are stray Gradle-home
  artifacts — an entire `GRADLE_USER_HOME` dumped into the project, several hundred MB of it. They
  are gitignored — never commit them, and exclude them from searches. Do not confuse the root
  `wrapper/` with `gradle/wrapper/`, which is the real, tracked wrapper.

  They come from IntelliJ's **Gradle user home** being set to the project directory. That setting is
  IDE-global, not per project, so `.idea/gradle.xml` will not mention it — it is `serviceDirectoryPath`
  in `~/Library/Application Support/JetBrains/<IDE>/options/gradle.settings.xml`. Blank means
  `~/.gradle`, which is what it should be. Fix it in Settings → Build Tools → Gradle rather than by
  editing the XML, which the IDE rewrites on exit.

  Opening the project in `.devcontainer/` makes IntelliJ store that path with its container prefix
  baked into the value — `/$devcontainer.ij/<container-id>@/IdeaProjects/<project>` — and Gradle
  inside the container then dies with `Could not create parent directory for lock file
  /$devcontainer.ij/...`, because it tries to mkdir that literally at `/`. The failure only surfaces
  when the wrapper must download a distribution it has not cached, so bumping the Gradle version is
  the usual trigger — the setting was already wrong before.
- A change to `exerciseN` or `solutionN` usually needs a matching change under `instruqt/track/`.
  The check scripts grep for specific filenames and API markers, and the solve scripts copy every
  `.java` from `solutionN` into `exerciseN` with a `sed` on the package declaration. Rename a file,
  add one, or change the marker an exercise turns on, and the corresponding challenge breaks. See
  `instruqt/README.md`.

## Instruqt track

`instruqt/` packages the seven exercises as a hands-on lab, mirroring the layout of the sister repo
`temporal-training-exercise-python`. Two independently pushed Instruqt entities plus a git-delivered
payload:

```
instruqt/
├── track/     track.yml + 7 NN-name/ challenge dirs   → instruqt track push
└── sandbox/   config.yml + scripts/setup-workshop     → instruqt sandbox push, then publish
    proxy/     + all exercise code                     → git push to WORKSHOP_REF
```

Exercise code is in neither push — it arrives through the `git clone` that provisioning runs.

Each challenge directory holds exactly five files, and **every file in one is parsed as a lifecycle
script**, so nothing else can live there. Challenge order comes from directory sort order.

Java-specific things that differ from the Python track, all of them load-bearing:

- Check scripts run a `./gradlew classes` gate before the Temporal gate, because one Gradle module
  compiles all 15 packages and a broken exercise blocks every other one.
- Solve scripts `sed` the `package` declaration rather than `cp`-ing files.
- `pkill` patterns match the JVM command line (`com.temporal.training.*StartWorker`).
- Exercise 6's tabs point at `src/test/java/...`, and its check counts `assertEquals`/`verify` calls
  because the shipped `@Test` methods have empty bodies and pass vacuously.
- Exercise 7's solve script sends the `approve` and `retry` Signals itself; the starter is
  fire-and-forget and parks in `RETRYING` forever without them.
