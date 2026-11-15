# Sandbox Preset

This directory is an Instruqt **sandbox preset**, slug
`temporal-in-practice-sandbox-java`. It is a first-class Instruqt entity with
its own versioned release cycle, pushed and published independently of the
track.

```
sandbox/
├── config.yml                    Preset definition: name, slug, containers   [PUSHED]
├── scripts/
│   └── setup-workshop            All provisioning, git and the JDK included  [PUSHED]
├── proxy/                        Network control panel, fetched via git clone
│   ├── toggle_addon.py           mitmproxy addon, re-reads state.json per request
│   ├── controlpanel.py           Flask UI + REST API on port 5000
│   ├── state.json                Default toggle state
│   └── static/index.html         The toggle UI
└── README.md                     This file
```

Only `config.yml` and `scripts/` are part of the preset payload. `proxy/` and
this README sit in the same directory for cohesion but are not uploaded; a
`sandbox pull` returns just those two. `proxy/` reaches the sandbox through the
git clone that `setup-workshop` performs.

## Why a preset

A preset shares one sandbox definition across tracks, prevents config drift,
and lets tracks **share a hot-start pool**. Because `scripts/setup-workshop`
ships with the preset rather than inside a git clone, it can install its own
dependencies, `git` and the JDK among them. The base image is relied on for
nothing but a Debian userland, and `git clone` fetches only the exercise code.

The track therefore has **no `config.yml` and no `track_scripts/`**. It points
here from `track.yml`:

```yaml
sandbox_preset: temporal-in-practice-sandbox-java
```

## How this ships

| Change | Command | Takes effect |
|--------|---------|--------------|
| `config.yml`, `scripts/` | `instruqt sandbox push` then `instruqt sandbox publish` | Next sandbox **provision**, for every track on the preset |
| `proxy/` | `git push` to `WORKSHOP_REF` | Next sandbox provision |
| `../track/` (assignments, `track.yml`) | `instruqt track push` | Next lab start |

> [!WARNING]
> `instruqt sandbox publish` moves **every track using this preset** to the
> newly published version. There is no per-track version pinning. Do not
> publish during a live session.

## Updating the preset

### 1. Edit and test locally

`instruqt track validate` does not look at this directory, and a broken script
fails at provision time. With Hot Start that means a pool of broken sandboxes,
so test before pushing.

Boot the base image and run the script against your working tree. Seeding
`/root/workshop` first makes the script skip its clone, so it uses your
uncommitted code. `--memory 4g` matches `config.yml`:

```bash
docker rm -f instruqt-test 2>/dev/null
docker run -d --name instruqt-test --memory 4g \
  -p 7299:7233 -p 8299:8233 -p 5099:5000 \
  ubuntu:24.04 sleep infinity

docker exec instruqt-test mkdir -p /opt/instruqt/bootstrap /root/workshop
docker exec instruqt-test touch /opt/instruqt/bootstrap/host-bootstrap-completed

tar --exclude='./.git' --exclude='./build' --exclude='./.gradle' --exclude='./bin' \
    -cf - . | docker exec -i instruqt-test tar -xf - -C /root/workshop

docker cp instruqt/sandbox/scripts/setup-workshop instruqt-test:/tmp/setup-workshop
docker exec instruqt-test chmod +x /tmp/setup-workshop
docker exec instruqt-test /tmp/setup-workshop
```

Run those from the repository root. `sleep infinity` keeps stock Ubuntu alive
without a TTY; Instruqt supplies its own long-running command. Temporal UI
lands on <http://localhost:8299>, the control panel on <http://localhost:5099>.
Add `--platform linux/amd64` to match Instruqt's architecture exactly; nothing
in the script is architecture-specific, and the JVM is much slower under
emulation.

Then walk every challenge end to end. For each one the check must fail on the
untouched exercise and pass after the solve script:

```bash
docker cp instruqt/track instruqt-test:/tmp/track
docker exec instruqt-test bash -c '
for ch in /tmp/track/0*; do
  echo "#### $(basename $ch)"
  bash $ch/setup-workshop >/dev/null
  bash $ch/check-workshop >/dev/null && echo "!! passed before solving"
  bash $ch/solve-workshop >/dev/null 2>&1
  bash $ch/check-workshop || echo "!! FAILED after solving"
  bash $ch/cleanup-workshop >/dev/null
done'

docker rm -f instruqt-test
```

Run this loop against a freshly provisioned container. The checks query the
dev server's history, so Workflows left over from an earlier run can make a
check pass for the wrong reason.

Keep ad hoc commands like these in a script file rather than in a
`bash -c '...'` string when they include `pkill -f StartWorker`: the pattern
matches the `bash -c` command line itself and kills your own shell. The
lifecycle scripts are files, so they are not affected.

To exercise the real clone path instead, skip the seeding step. `WORKSHOP_REF`
then has to name a ref you have already pushed:

```bash
docker exec -e WORKSHOP_REF=my-branch instruqt-test /tmp/setup-workshop
```

### 2. Push and publish

Run from this directory. `push` creates a remote **draft**; `publish` makes it
live.

```bash
cd instruqt/sandbox
instruqt sandbox diff        # local vs remote, covers config.yml AND scripts/
instruqt sandbox push        # -> draft
instruqt sandbox publish --message="What changed"
```

`instruqt sandbox pull <slug>` fetches the latest version, preferring a draft
over the published one. Instruqt's serializer strips comments from
`config.yml`, so a pull looks different from what is committed here; `diff` is
semantic and reports no change.

Then commit, because `proxy/` and the script are both source of truth in git.

### 3. Re-warm the Hot Start pool

**After any preset publish, exercise change, or `WORKSHOP_REF` bump, re-warm
the pool in the Instruqt web UI.** Then launch one sandbox from the pool and
click through a challenge.

## What setup-workshop provisions

Eight steps, in order. This script owns every binary dependency; nothing is
inherited from the base image.

1. System packages: `git`, `curl`, `jq`, editors, and `openjdk-17-jdk-headless`.
2. `git clone --depth 1` of the exercise repo to `/root/workshop`.
3. The Temporal CLI, symlinked to `/usr/local/bin/temporal`.
4. `uv`, a private Python for mitmproxy and Flask in `/opt/proxy-venv`, and
   the mitmproxy CA cert trusted system-wide and in the JVM trust store.
5. Gradle pre-warm: the wrapper distribution, every resolvable configuration's
   jars, and `compileJava compileTestJava`. Leaves a Gradle daemon running.
6. `proxy/` staged to `/root/proxy/` from the clone.
7. Shell environment in `/etc/profile.d/workshop.sh`, and Gradle's proxy
   settings appended to `/root/.gradle/gradle.properties`.
8. mitmproxy, the control panel, and `temporal server start-dev` with
   `AccountId=Text` registered, then a health wait.

Measured at about two and a half minutes on `ubuntu:24.04`. Peak memory
across all seven challenges stays under 1.5 GB of the 4 GB container.

### Adding a dependency

A system package goes in step 1. A Java dependency goes in `build.gradle`,
pushed to `WORKSHOP_REF`; step 5 downloads it at provision time.

### Changing the Java version

`JDK_PACKAGE` at the top of the script, kept in step with the toolchain
`languageVersion` in `build.gradle`. The build declares no toolchain resolver,
so Gradle can only use a JDK that is already installed.

### Changing the base image

`config.yml` in this directory. It needs a Debian or Ubuntu userland with
`apt-get` and an OpenJDK package of the right version.

## Four things that are easy to break

**The order of steps 4 and 5.** A JVM reads its trust store once, at startup.
The Gradle daemon from the pre-warm lives on and serves the attendee's
builds, so the mitmproxy CA must be trusted before that daemon starts. Swap
the steps and every Gradle download through the proxy fails with
`PKIX path building failed`.

**A partial pre-warm.** `compileJava compileTestJava` resolves only the
compile classpaths. The `execute` and `test` tasks also need runtime-only
jars, which is what the `resolveAllConfigurations` init script is for. Without
it the first Worker start goes back to Maven Central.

**Daemon arguments after the pre-warm.** Anything that changes the daemon's
JVM arguments, `org.gradle.jvmargs` in particular, makes the warm daemon
incompatible and costs the attendee a cold start. The proxy `systemProp`
lines are applied per build and are safe.

**Toolchain on the default PATH.** `temporal` and `uv` install to
`/usr/local/bin`, and `java` comes from Debian's alternatives in `/usr/bin`.
Check and solve scripts run non-interactively and never source a profile, so
a `PATH` export in a profile is invisible to them.

## Network control panel

Ported from the Python sandbox, itself from
[temporal-sa/ai-agents-webinar](https://github.com/temporal-sa/ai-agents-webinar).
mitmproxy listens on `127.0.0.1:8888`. Shell tools reach it through
`HTTP_PROXY` and `HTTPS_PROXY`. Gradle ignores those variables, so it is
pointed at the proxy with `systemProp.http(s).proxyHost` in
`/root/.gradle/gradle.properties`. The Flask panel on port 5000 flips toggles
by writing `state.json`, and the addon re-reads that file on every request.

| Toggle | Hosts | Effect when off |
|--------|-------|-----------------|
| `maven` | `repo.maven.apache.org`, `repo1.maven.org` | Gradle cannot resolve new dependencies |
| `gradle` | `services.gradle.org`, `downloads.gradle.org`, `plugins.gradle.org` | Wrapper and plugin downloads fail |
| `github` | `github.com`, `raw.githubusercontent.com`, `codeload.github.com` | Git and release downloads fail |
| `httpbin` | `httpbin.org` | A request-echo endpoint for proving the proxy is in the path |
| Kill switch | everything | HTTP 503 on all outbound traffic |

The Worker and test JVMs that Gradle forks inherit neither the environment
proxy settings (the JVM ignores them) nor Gradle's system properties, so the
Temporal SDK's gRPC to `127.0.0.1:7233` never touches the proxy and the kill
switch never breaks the lab itself.

These exercises call no external HTTP APIs, and every dependency is cached by
the pre-warm, so the toggles disrupt nothing as shipped. As an instructor
demo: flip `maven` off, run `./gradlew compileJava --refresh-dependencies`,
watch it fail to resolve `temporal-sdk`, flip it back.

To add a toggle: add the key and its hostnames to `SERVICE_HOSTS` in
`toggle_addon.py`, add the key to `state.json`, and add a display label to
`SERVICE_LABELS` in `controlpanel.py`. The UI renders itself from
`SERVICE_LABELS`. These files ship on a `git push`, not a `sandbox push`.

## Logs

| File | What |
|------|------|
| `/tmp/proxy.log` | mitmproxy |
| `/tmp/controlpanel.log` | Flask control panel |
| `/tmp/temporal-server.log` | Temporal dev server |
| `/tmp/mitm-init.log` | The throwaway run that generates the CA cert |
| `/tmp/solve-0N-worker.log` | The Worker a solve script started for challenge N |
| `/root/.gradle/daemon/*/daemon-*.out.log` | Gradle daemon |

The provisioning output itself goes to the sandbox's lifecycle-script log,
visible in the Instruqt web UI.
