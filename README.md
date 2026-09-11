# Temporal Training Exercise - Java Solution

Complete Java implementation for progressive Temporal training exercises.

## Prerequisites
- Java 17+
- Temporal CLI

## Setup

1. Start Temporal dev server with AccountId search attribute:
```bash
temporal server start-dev --search-attribute AccountId=Text
```

2. Build project:
```bash
./gradlew build
```

## Exercise Progression

### [Exercise 1: Hello Temporal](src/main/java/com/temporal/training/exercise1) (30 min)
- Basic Workflow and Activity setup
- Worker registration and execution

### [Exercise 2: Money Transfer Basics](src/main/java/com/temporal/training/exercise2) (45 min)
- Multiple activities (withdraw, deposit, refund)
- Signal-based approval mechanism
- Workflow.await() for conditional waiting
- Basic error handling and compensation

### [Exercise 3: Query Handlers](src/main/java/com/temporal/training/exercise3) (35 min)
- Query methods for workflow state inspection
- Status tracking throughout execution
- External workflow monitoring
- Signal vs Query differences

### [Exercise 4: Visibility & Monitoring](src/main/java/com/temporal/training/exercise4) (30 min)
- Custom Search Attributes (AccountId)
- Upsert Search Attributes from workflows
- Workflow filtering and discovery

### [Exercise 5: User Metadata & Activity Summaries](src/main/java/com/temporal/training/exercise5) (30 min)
- Activity summaries for better observability
- Static workflow summaries
- Enhanced monitoring in Temporal Web UI

### [Exercise 6: Testing Strategy](src/main/java/com/temporal/training/exercise6) (45 min)
- Unit tests with TestWorkflowExtension (JUnit 5)
- Time skipping for fast tests
- Activity mocking with Mockito
- Search attribute registration in tests

### [Exercise 7: Manual Activity Retry](src/main/java/com/temporal/training/exercise7) (40 min)
- Manual retry pattern using signals
- Invalid data handling scenarios
- Disabling automatic retries
- Interactive retry commands

## Running Exercises

### Exercise 1 (Hello Temporal)
```bash
# Start worker
./gradlew execute -PmainClass=com.temporal.training.exercise1.StartWorker
# Run workflow (in another terminal)
./gradlew execute -PmainClass=com.temporal.training.exercise1.StartWorkflow
```

### Exercise 2 (Money Transfer Basics)
```bash
# Start worker
./gradlew execute -PmainClass=com.temporal.training.exercise2.StartWorker
# Run workflow (in another terminal)
./gradlew execute -PmainClass=com.temporal.training.exercise2.StartWorkflow
```

### Exercise 3 (Query Handlers)
```bash
# Start worker
./gradlew execute -PmainClass=com.temporal.training.exercise3.StartWorker
# Run workflow (in another terminal)
./gradlew execute -PmainClass=com.temporal.training.exercise3.StartWorkflow
```

### Exercise 4 (Visibility & Monitoring)
```bash
# Start worker
./gradlew execute -PmainClass=com.temporal.training.exercise4.StartWorker
# Run workflow (in another terminal)
./gradlew execute -PmainClass=com.temporal.training.exercise4.StartWorkflow
```

### Exercise 5 (User Metadata & Activity Summaries)
```bash
# Start worker
./gradlew execute -PmainClass=com.temporal.training.exercise5.StartWorker
# Run workflow (in another terminal)
./gradlew execute -PmainClass=com.temporal.training.exercise5.StartWorkflow
```

### Exercise 6 (Testing Strategy)
```bash
# Run tests. --rerun matters: Gradle caches test results, and without it a
# second run reports UP-TO-DATE without executing anything.
./gradlew test --rerun --tests "com.temporal.training.exercise6.MoneyTransferWorkflowTest"
```

### Exercise 7 (Manual Activity Retry)
```bash
# Start worker
./gradlew execute -PmainClass=com.temporal.training.exercise7.StartWorker
# Run workflow (in another terminal). It uses invalid-account-456 on purpose,
# starts the workflow fire-and-forget, and prints nothing.
./gradlew execute -PmainClass=com.temporal.training.exercise7.StartWorkflow

# The workflow id is money-transfer-<random UUID>, so look it up
WORKFLOW_ID=$(temporal workflow list \
  --query 'TaskQueue="money-transfer-task-queue" AND ExecutionStatus="Running"' \
  --limit 1 -o json | jq -r '.[0].execution.workflowId')

# Approve, let the deposit fail, then send the corrected account
temporal workflow signal --workflow-id "$WORKFLOW_ID" --name approve --input true
temporal workflow query  --workflow-id "$WORKFLOW_ID" --type getStatus   # RETRYING
temporal workflow signal \
  --workflow-id "$WORKFLOW_ID" \
  --name retry \
  --input '{"key":"toAccount","value":"account-456"}'
```

## Task queues

Each exercise polls its own task queue. Two exercises sharing a queue means a
worker left running from an earlier one picks up tasks for a workflow it cannot
replay, so leave these distinct:

| Package | Task queue |
|---|---|
| `exercise1`, `solution1` | `hello-task-queue` |
| `exercise2`, `solution2` | `money-transfer-signals-task-queue` |
| `exercise3`, `solution3` | `money-transfer-queries-task-queue` |
| `exercise4`, `solution4` | `money-transfer-search-attributes-task-queue` |
| `exercise5`, `solution5` | `money-transfer-summaries-task-queue` |
| `exercise6`, `solution6` | `money-transfer-testing-task-queue` |
| `exercise7`, `solution7` | `money-transfer-task-queue` |

## Running Solutions

Replace `exercise` with `solution` in the class names above to run complete implementations.

## Key Features Implemented

- **Java 17 Records**: Modern data classes
- **Activity Retry**: Configurable retry policies
- **Signal/Query**: Workflow interaction patterns
- **Search Attributes**: Custom filtering
- **User Metadata**: Activity context
- **Compensation**: Saga pattern for rollbacks
- **Testing**: Time-skipping unit tests
- **Error Handling**: Distinguishes activity vs workflow failures
- **Manual Retry**: Signal-based retry for failed activities

---

## Instruqt Track: Temporal in Practice - Java

These seven exercises are also packaged as an Instruqt hands-on lab. Attendees
get a container sandbox with a Temporal dev server already running, a warm
Gradle cache, the native code editor pointed at the exercise package, and the
finished solution one tab away.

```
instruqt/
├── README.md     How to push the track and the sandbox
├── track/        The track definition: track.yml + 7 challenge directories
└── sandbox/      An Instruqt sandbox preset, pushed separately from the track
                  (has its own README on provisioning and the proxy panel)
```

There is no image to build. The preset's `scripts/setup-workshop` installs the
toolchain on stock `ubuntu:24.04`, clones this repo, warms the Gradle cache by
compiling every package, and starts the Temporal dev server. That is minutes of
work, so Hot Start pre-provisions all of it ahead of the session.

The track and the sandbox are separate artifacts with separate commands, and
this repo's exercise code is part of neither push — it arrives through the git
clone that provisioning performs:

```bash
cd instruqt/track && instruqt track push      # assignments, track.yml
cd instruqt/sandbox && instruqt sandbox push  # then: instruqt sandbox publish
git push                                      # exercise code, proxy files
```

See [instruqt/README.md](instruqt/README.md) for which change needs which
command, what differs from the Python track, and the pre-flight checklist
before a live session.
