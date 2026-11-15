# Temporal in Practice: Instruqt Track (Java)

The seven exercises in this repo, packaged as an Instruqt lab. Attendees get a
container sandbox with a Temporal dev server already running, JDK 17 and a
warm Gradle daemon, the code editor on the exercise package, and the finished
solution one tab away.

This is the Java counterpart of the Python track in
[temporal-sa/temporal-training-exercise-python](https://github.com/temporal-sa/temporal-training-exercise-python/tree/main/instruqt),
and follows the same layout and shipping model.

```
instruqt/
├── track/       7 challenge directories + track.yml   ->  instruqt track push
└── sandbox/     config.yml + scripts/                 ->  instruqt sandbox push
                 proxy/                                ->  git push
```

| Challenge | Exercise | What the check verifies |
|---|---|---|
| `01-hello-temporal` | Workflow, Activity, Worker | TODOs gone; a Workflow on `hello-task-queue` completed and returned a greeting, not `null` |
| `02-signals` | `@SignalMethod`, `Workflow.await` | TODOs gone; annotation and await present; a completed transfer has an `approve` Signal in its history |
| `03-queries` | `@QueryMethod`, status tracking | TODOs gone; annotation present; status set to `APPROVED`, `COMPLETED`, `CANCELLED`; a transfer completed |
| `04-search-attributes` | `upsertTypedSearchAttributes` | TODOs gone; a Workflow is found by `AccountId="account-123"` |
| `05-activity-summaries` | `setSummary`, `setStaticSummary` | TODOs gone; a completed transfer's history carries the static summary and at least two Activity summaries |
| `06-unit-testing` | `TestWorkflowRule`, mocks, time skipping | TODOs gone; tests register `AccountId`, schedule Signals, assert and verify; the JUnit report shows 3 passing tests |
| `07-manual-retry` | Non-retryable failure, Signal-driven retry | TODOs gone; non-retryable failure and retry wiring present; a completed transfer has a failed Activity and a `retry` Signal |

Several checks go further than "no TODOs and something completed". In this
repo the shipped stubs make that weak form pass too easily: exercise 1's stub
Activity returns `null` and the Workflow still completes, and exercise 6's
stub tests compile and pass with empty bodies.

## Three ways a change ships

| What you edited | How it ships | Live at |
|---|---|---|
| `track/` — assignments, lifecycle scripts, `track.yml` | `instruqt track push` | next lab start |
| `sandbox/config.yml`, `sandbox/scripts/` | `instruqt sandbox push` then `publish` | next sandbox provision |
| `sandbox/proxy/`, `src/` (exercises and solutions), `build.gradle` | `git push` to `WORKSHOP_REF` | next sandbox provision |

The track and the sandbox preset are separate Instruqt entities:
`instruqt track push` does not touch the preset, and `instruqt sandbox push`
does not touch the track. The track carries no `config.yml` and no
`track_scripts/` — `track.yml` names the preset instead:

```yaml
sandbox_preset: temporal-in-practice-sandbox-java
```

> [!IMPORTANT]
> The third row is the one that catches people. Exercise code, `build.gradle`
> and the proxy files are part of neither push. They reach the sandbox through
> the `git clone` that `sandbox/scripts/setup-workshop` runs at provision time,
> so an edit is live only once it is pushed to the ref `WORKSHOP_REF` names
> (default `main` of `taonic/temporal-training-exercise-java`). Pushing the
> track ten times will not pick it up.

Authenticate once with `instruqt auth login`.

## First push

Neither entity exists on Instruqt yet, so nothing here carries an `id:`.
Create the preset first, because `instruqt track validate` resolves
`sandbox_preset` remotely and fails until it exists:

```bash
git push                    # instruqt/sandbox/proxy/ must be on WORKSHOP_REF first
cd instruqt/sandbox
instruqt sandbox push
instruqt sandbox publish --message="Initial Java sandbox"
cd ../track
instruqt track validate
instruqt track push         # creates temporal-in-practice-java
instruqt track pull         # writes the assigned track, challenge and tab ids
git add . && git commit -m "Instruqt: record ids from first push"
```

## Pushing the track

Once it exists, a push updates it in place. Run from `instruqt/track/`:

```bash
cd instruqt/track
instruqt track validate    # local only, no push
instruqt track push
instruqt track pull        # ids for any new challenge or tab, then commit
instruqt track test        # runs every solve script in order, end to end
```

`instruqt track push` rewrites `track.yml` and every `assignment.md`
**frontmatter** in place: comments stripped, keys reordered, block scalars
collapsed, `checksum` injected. Commit that as-is rather than prettifying it,
and keep durable explanation in this README instead of in YAML comments.

`instruqt track diff` is missing from some CLI versions; compare with
`instruqt track checksum` instead.

> [!IMPORTANT]
> Never edit the track in the web UI while editing locally — it corrupts the
> `.remote` cache. Some settings are web-UI authoritative no matter what you
> push: `idle_timeout`, Hot Start, and invite configuration.

Two things that break a push or silently reorder the lab:

- **Every file in a challenge directory is parsed as a lifecycle script.** A
  stray diagram next to `assignment.md` breaks the track. Shared assets belong
  under `sandbox/` or inline in the markdown.
- **Challenge order comes from directory sort order**, so the `NN-` prefixes
  have to stay sequential and `track.yml` must have no `challenges:` block.
  Lifecycle script names carry the preset's hostname (`-workshop`).

## Lifecycle scripts

Per challenge, `setup-workshop` and `cleanup-workshop` only kill leftover
Workers and starters. `pkill -f StartWorker` matches both the `./gradlew`
client and the JVM it forks, and leaves the Gradle daemon running.

`solve-workshop` runs on Skip and on `instruqt track test`. Each one copies
`solutionN` over `exerciseN` and drives the exercise to a state the check
accepts. Three Java-specific details:

- **The copy rewrites the package.** Solution files declare
  `package com.temporal.training.solutionN`, so a plain `cp` does not compile.
  The scripts pipe each file through `sed`.
- **They compile before starting anything.** The Worker and the starter are
  two concurrent Gradle builds. If both reach `compileJava` with stale
  classes, one deletes class files the other is reading and fails with
  `NoSuchFileException`. A `./gradlew -q compileJava` up front leaves both
  builds up to date.
- **They wait for a fresh poller.** The server keeps listing a killed
  Worker's poller for minutes, and exercises 2 to 5 share
  `MoneyTransferTaskQueue`. The wait only counts pollers whose
  `lastAccessTime` is after the script started.

`07-manual-retry/solve-workshop` walks the whole retry path from the CLI
(start, approve, wait for `RETRYING`, Signal the corrected `toAccount`),
because the exercise 7 starter targets `invalid-account-456` and returns
immediately.

## Pushing the sandbox preset

`instruqt track validate` never looks at `sandbox/`, and a broken setup script
fails at provision time — which, with Hot Start, means a pool of broken
sandboxes. Test it in Docker first; `sandbox/README.md` has the container loop,
including a loop that runs every challenge's setup, check, solve and cleanup.

```bash
cd instruqt/sandbox
instruqt sandbox diff        # local vs remote, covers config.yml AND scripts/
instruqt sandbox push        # -> draft
instruqt sandbox publish --message="What changed"
git add instruqt/sandbox/ && git commit -m "Sandbox: <what changed>" && git push
```

`push` creates a draft; `publish` makes it live. `push` refuses with `nothing
to push` when local and remote match (`--force` overrides), and `publish`
refuses with `no draft to publish` when there is none.

> [!WARNING]
> `instruqt sandbox publish` moves **every track on this preset** to the newly
> published version. There is no per-track pinning. Do not publish during a
> live session.

## After any change: re-warm Hot Start

A warm pool holds sandboxes built from the preset version and `WORKSHOP_REF`
that were current when it was warmed. They keep serving that state no matter
what you publish or push afterwards.

Re-warm the pool in the web UI after any preset publish, exercise change, or
`WORKSHOP_REF` bump. Then launch one pooled sandbox and click through a
challenge: an upstream blip during warm-up (apt, Maven Central,
`services.gradle.org`, `temporal.download`) produces broken pooled sandboxes
silently.

Provisioning takes about two and a half minutes, most of it the Gradle
distribution and dependency download. Hot Start absorbs it, and it is the
supported mechanism above roughly five simultaneous starts.

## Live workshop pre-flight

- [ ] **`WORKSHOP_REF` pinned** to a tag or commit in
      `sandbox/scripts/setup-workshop`. On the default `main`, pools warmed at
      different times contain different code.
- [ ] **`WORKSHOP_REPO` points where you expect.** It defaults to
      `taonic/temporal-training-exercise-java`, which must stay public for the
      unauthenticated clone to work.
- [ ] **Exercise and `proxy/` changes pushed** to that ref.
- [ ] **Preset published**, if `config.yml` or `scripts/` changed.
- [ ] **`maintenance: false`** in `track.yml`, pushed. It ships `true`, so only
      owners and authors can launch.
- [ ] **Hot Start enabled**, pool sized to attendees plus 10 to 20 percent,
      warmed well ahead of the session and re-warmed after the last change.
- [ ] **`idle_timeout` set in the web UI** (Track, Settings, Additional
      Settings) to workshop length plus 20 minutes. The web UI wins after track
      creation, so a pushed `3600` can still show as `600`.
- [ ] **`feedback_recap_enabled` and `feedback_tab_enabled` set to `false`** in
      `track.yml` if feedback is collected out of band. Both ship `true`, which
      suits a self-paced run.
- [ ] **`instruqt track test` passes.** It catches regressions a manual
      click-through misses.

Afterwards, disable Hot Start if the pool was sized for this session — idle
pre-provisioned sandboxes cost money.

No `secrets:` are declared, so there is nothing to populate under Track
Settings, Secrets.
