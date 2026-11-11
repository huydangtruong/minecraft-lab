# Minecraft Lab — AI Instructions

## Start here

Read [docs/README.md](docs/README.md), then [docs/system/00-environment.md](docs/system/00-environment.md) (versions, paths, Java) and [docs/system/02-findings.md](docs/system/02-findings.md) (API names already verified on 26.3). Owner decisions: [docs/system/01-decisions.md](docs/system/01-decisions.md); don't reverse one without asking. If you are a dev session, read [docs/workflow/team.md](docs/workflow/team.md) and your plan in `docs/plans/` first and open only the files the plan names.

## Owner

Huy writes in Vietnamese and wants short, plain answers. Owner-facing text (README, decisions, backlog, playtest, install guides) in Vietnamese; code, comments, file names, plans in English. Record every new decision in `docs/system/01-decisions.md`.

## Repository

| Path | What |
|---|---|
| `mods/<id>/` | One Fabric mod = one Gradle subproject (needs `gradle.properties` with `mod_id`, `mod_name`, `mod_version`, `mod_description`). Folders starting with `_` are ignored |
| `build.gradle`, `settings.gradle`, `gradle.properties` | Shared build for all mods: Loom, Java 25, Fabric API, JUnit, server game tests. Versions are pinned in `gradle.properties` |
| `scripts/install.sh` | Build and copy mods into Huy's profile "26.3 Fabric Essential" |
| `scripts/package.sh` | Zip mods + Fabric API + Vietnamese install note for a friend (`dist/`) |
| `docs/` | `system/` (current truth), `mods/` (per-mod idea + spec), `plans/` (P-NNN), `backlog/`, `workflow/` |

## Build and test

JDK 25 is required (`/usr/libexec/java_home -v 25`; installed at `~/Library/Java/JavaVirtualMachines/temurin-25.jdk`). The scripts and `export JAVA_HOME=$(/usr/libexec/java_home -v 25)` handle it.

```sh
./gradlew :permahearts:build         # compile + JUnit + server game tests (jar in mods/permahearts/build/libs/)
./gradlew :permahearts:test          # JUnit only
./gradlew :permahearts:runClient     # dev Minecraft with the mod, in mods/permahearts/run/ (own saves)
scripts/install.sh [mod-id]          # build + install into the Essential profile
scripts/package.sh                   # zip for a friend
```

## Rules

- **Minecraft 26.3 is not obfuscated**: use Mojang names directly (no Yarn, no mappings). Look names up with `javap` on `~/Library/Application Support/minecraft/versions/26.3/26.3.jar` (see 00-environment.md). Never guess an API name from older versions; check it and add what you learn to 02-findings.md.
- Keep rules in pure Java classes (no Minecraft imports) so JUnit can test them; Minecraft glue stays thin.
- Never touch Huy's real saves or other mods in the profile. `install.sh` replaces only jars of the mods it builds (and installs Fabric API if missing). Test with `runClient` / game tests.
- Don't bump pinned versions (Minecraft, Loader, Loom, Fabric API) without Huy's go-ahead; the friend must run the same ones.
- Commit each coherent step. Branches `feat/P-NNN-<slug>`; push the branch, never `main`. Commit messages in English.
- Downloads or anything outside this repo and `~/.gradle` need Huy's go-ahead.
