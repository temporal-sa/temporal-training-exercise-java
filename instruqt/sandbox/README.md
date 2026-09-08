# Sandbox Preset

This directory is an Instruqt **sandbox preset**, slug
`temporal-in-practice-sandbox-java`. It is a first-class Instruqt entity with
its own versioned release cycle, pushed and published independently of the
track.

```
sandbox/
├── config.yml                    Preset definition: name, slug, containers   [PUSHED]
├── scripts/
│   └── setup-workshop            All provisioning, git included              [PUSHED]
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
and lets tracks **share a hot-start pool**. It also removes a bootstrap
problem: because `scripts/setup-workshop` ships with the preset rather than
inside a git clone, it can install its own dependencies, `git` among them. The
base image is relied on for nothing but a Debian userland, and `git clone`
fetches only the exercise code.

The track therefore has **no `config.yml` and no `track_scripts/`**. It points
here from `track.yml`:

```yaml
sandbox_preset: temporal-in-practice-sandbox-java
```

## How this ships

Two entities, two commands, two different moments:

| Change | Command | Takes effect |
|--------|---------|--------------|
| `config.yml`, `scripts/` | `instruqt sandbox push` then `instruqt sandbox publish` | Next sandbox **provision**, for every track on the preset |
| `proxy/` | `git push` to `WORKSHOP_REF` | Next sandbox provision |
| `../track/` (assignments, `track.yml`) | `instruqt track push` | Next lab start |

> [!IMPORTANT]
> `instruqt track push` does not touch this preset, and `instruqt sandbox
> push` does not touch the track. They are separate artifacts.

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
uncommitted code:

```bash
docker rm -f instruqt-test 2>/dev/null
docker run -d --name instruqt-test --platform linux/amd64 \
  -p 7299:7233 -p 8299:8233 -p 5099:5000 \
  ubuntu:24.04 sleep infinity

docker exec instruqt-test mkdir -p /opt/instruqt/bootstrap
docker exec instruqt-test touch /opt/instruqt/bootstrap/host-bootstrap-completed

docker exec instruqt-test mkdir -p /root/workshop
tar --exclude='./.git' --exclude='./build' --exclude='./.gradle' \
    -cf - . | docker exec -i instruqt-test tar -xf - -C /root/workshop

docker cp instruqt/sandbox/scripts/setup-workshop instruqt-test:/tmp/setup-workshop
docker exec instruqt-test chmod +x /tmp/setup-workshop
docker exec instruqt-test /tmp/setup-workshop
```

Run those from the repository root. `sleep infinity` keeps stock Ubuntu alive
without a TTY; Instruqt supplies its own long-running command. Temporal UI
lands on <http://localhost:8299>, the control panel on <http://localhost:5099>.

Exclude `build/` and `.gradle/` from the tar. They hold absolute paths from
your machine, and Gradle will trip over them inside the container.

Then walk at least one challenge end to end:

```bash
CH=04-search-attributes
docker exec instruqt-test mkdir -p /tmp/ch
for s in setup check solve cleanup; do
  docker cp "instruqt/track/$CH/$s-workshop" "instruqt-test:/tmp/ch/$s"
done
docker exec instruqt-test chmod -R +x /tmp/ch
docker exec instruqt-test /tmp/ch/setup
docker exec instruqt-test /tmp/ch/solve
docker exec instruqt-test /tmp/ch/check
docker exec instruqt-test /tmp/ch/cleanup

docker rm -f instruqt-test
```

Exercise 7 is the one worth testing by hand as well as by script: its solve
path sends two Signals, and a change to the Workflow's status enum or Signal
names breaks it silently.

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

`push` refuses with `nothing to push, no changes between remote and local
config` when local and remote genuinely match. `--force` pushes anyway.
`publish` refuses with `no draft to publish` when there is no pending draft.

`instruqt sandbox pull <slug>` fetches the latest version, preferring a draft
over the published one. Note that Instruqt's serializer strips comments from
`config.yml`, so a pull looks different from what is committed here; `diff` is
semantic and reports no change.

Then commit, because `proxy/` and the script are both source of truth in git:

```bash
git add instruqt/sandbox/ && git commit -m "Sandbox: <what changed>"
git push
```

### 3. Re-warm the Hot Start pool

A warm pool holds sandboxes provisioned from the preset version and
`WORKSHOP_REF` that were current at warm-up. They keep serving that state no
matter what you publish or push afterwards.

**After any preset publish, exercise change, or `WORKSHOP_REF` bump, re-warm
the pool in the Instruqt web UI.** Then launch one sandbox from the pool and
click through a challenge: an upstream blip during warm-up (apt, Maven Central,
Gradle's distribution service, `temporal.download`) produces broken pooled
sandboxes silently.

## What setup-workshop provisions

Nine steps, in order. This script owns every binary dependency; nothing is
inherited from the base image.

1. System packages: `git`, `curl`, `jq`, `unzip`, `ca-certificates`, editors,
   `python3` for the proxy, and `openjdk-17-jdk-headless`.
2. `git clone --depth 1` of the exercise repo to `/root/workshop`.
3. The Temporal CLI, symlinked to `/usr/local/bin/temporal`.
4. `./gradlew classes testClasses` to warm the Gradle cache.
5. mitmproxy and Flask in `/opt/proxy-venv`, and the mitmproxy CA cert trusted
   system-wide **and** imported into the JVM truststore.
6. Gradle proxy settings in `/root/.gradle/gradle.properties`.
7. `proxy/` staged to `/root/proxy/` from the clone.
8. Shell environment written to `/etc/profile.d/workshop.sh`.
9. mitmproxy, the control panel, and `temporal server start-dev` started, then
   a health wait.

Step 4 is the expensive one: it pulls `gradle-8.8-all.zip`, the whole Temporal
SDK dependency tree, and then compiles fifteen packages plus the test sources.
Reckon on several minutes. It is also why Hot Start is not optional for this
track.

### Adding a dependency

Edit step 1. Not the base image, and not the track.

### Changing the Java version

`JAVA_VERSION` at the top of the script, and keep it in step with the
`toolchain` block in `build.gradle`. Gradle's toolchain resolution has no
auto-download configured, so it can only use a JDK that apt has already
installed.

### Changing the base image

`config.yml` in this directory. It needs a Debian or Ubuntu userland with
`apt-get` and an `openjdk-17-jdk-headless` package.

## Four things that are easy to break

**Toolchain on the default PATH.** `temporal` is symlinked into
`/usr/local/bin` and the JDK lands in `/usr/bin` on purpose. Check and solve
scripts run non-interactively and never source a profile, so a `PATH` export in
a profile is invisible to them. The lifecycle scripts deliberately depend on
nothing from `/etc/profile.d/workshop.sh`; they set `GRADLE_USER_HOME`
themselves and otherwise rely on defaults.

**The JVM does not read `HTTP_PROXY`.** Setting the environment variables routes
`curl` and the Temporal CLI through mitmproxy and does nothing at all for
Gradle. Gradle needs `systemProp.http.proxyHost` and friends, which step 6
writes to `/root/.gradle/gradle.properties`. `nonProxyHosts` keeps loopback off
the proxy so the kill switch cannot break the lab itself.

**The JVM does not read `/etc/ssl/certs` either.** `update-ca-certificates`
makes the mitmproxy CA trusted for everything except Java. Without the
`keytool -importcert` into `$JAVA_HOME/lib/security/cacerts`, every HTTPS fetch
Gradle makes through the proxy fails certificate validation.

**Cache warming has to happen before the proxy exists.** Step 4 runs while the
provisioning shell still has direct outbound access, which is why it is ahead
of steps 5 and 6. Move it after them and a cold cache has to come through
mitmproxy on a CA cert that has not been trusted yet.

## Network control panel

Ported from [temporal-sa/ai-agents-webinar](https://github.com/temporal-sa/ai-agents-webinar)
by way of the Python track. mitmproxy listens on `127.0.0.1:8888`, every shell
routes outbound HTTP and HTTPS through it, and the Flask panel on port 5000
flips toggles by writing `state.json`. The addon re-reads that file on every
request, so a toggle takes effect immediately with no restart.

| Toggle | Hosts | Effect when off |
|--------|-------|-----------------|
| `maven` | `repo1.maven.org`, `repo.maven.apache.org`, `plugins.gradle.org`, `services.gradle.org`, `downloads.gradle.org` | Gradle cannot resolve dependencies or fetch its own distribution |
| `github` | `github.com`, `raw.githubusercontent.com`, `codeload.github.com` | Git and release downloads fail |
| `httpbin` | `httpbin.org` | A request-echo endpoint for proving the proxy is in the path |
| Kill switch | everything | HTTP 503 on all outbound traffic |

`NO_PROXY=127.0.0.1,localhost` and the matching `nonProxyHosts` keep the
Temporal SDK's gRPC traffic and the dev server's own HTTP off the proxy, so the
kill switch never breaks the lab itself.

These seven exercises call no external HTTP APIs of their own, so the toggles
do not disrupt any Workflow as shipped. The panel is wired for a future chapter
that does, and it works as an instructor demo now: flip `maven` off, run
`./gradlew classes --refresh-dependencies`, watch it fail, flip it back. The
warm cache means an ordinary build still succeeds with `maven` off, which is
itself worth showing.

To add a toggle: add the key and its hostnames to `SERVICE_HOSTS` in
`toggle_addon.py`, add the key to `state.json`, and add a display label to
`SERVICE_LABELS` in `controlpanel.py`. The UI renders itself from
`SERVICE_LABELS`, so there is no HTML to edit. These files ship on a
`git push`, not a `sandbox push`.

## Logs

When a sandbox misbehaves:

| File | What |
|------|------|
| `/tmp/proxy.log` | mitmproxy |
| `/tmp/controlpanel.log` | Flask control panel |
| `/tmp/temporal-server.log` | Temporal dev server |
| `/tmp/mitm-init.log` | The throwaway run that generates the CA cert |
| `/tmp/solve-0N-worker.log` | The Worker a solve script started, per challenge |

The provisioning output itself goes to the sandbox's lifecycle-script log,
visible in the Instruqt web UI.
