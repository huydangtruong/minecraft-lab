# Status — where work stands

> Tóm tắt cho Huy: file này ghi việc đang làm dở và bước tiếp theo, để một phiên Claude mới đọc là làm tiếp được.
> Manager cập nhật file này cuối mỗi đợt việc (merge, phát hành, quyết định mới).
> Snapshot: 2026-11-14, after release 0.11.0.

A fresh agent reads, in order: this file → [AGENTS.md](../../AGENTS.md) → [workflow/team.md](../workflow/team.md) → the plan of the work in flight. Do not read the whole repo.

## Releases

| What | Where |
|---|---|
| Last release | `permahearts-v0.11.0` (GitHub Releases on `huydangtruong/minecraft-lab`), `mod_version=0.11.0` |
| 0.11.0 contains | Death Match (P-010, D25), Life Steal Blade bleeding + knockback and Violet Guardian (P-020, D60), Thousand Mile Step (P-019, D58, D59) |
| On `main`, not released | P-021 (D62, D63): Violet Guardian and Warrior Angel kills count for the owner; Warrior Angel technique (lifetime 60 s and cooldown 120 s are manager proposals); P-022 Aqua Marine (D64). Next version 0.12.0, release when Huy says |

Release steps: [workflow/release.md](../workflow/release.md). Release **only when Huy says so**.

## In flight

- **Windows installer bug** (reported by Huy's friend): picking a profile other than "26.3 Fabric Essential" says SUCCESS, but that profile's mods folder still has the old jar. Reading `minecraft-lab.ps1` (`Find-Profiles` / `Select-Profile`) found nothing yet. Waiting for the friend's `Mods folder:` line and that profile's Game directory.

## Open, not scheduled

- **BL-004**: a command to summon the Crimson Master with a chosen quest ([backlog/BACKLOG.md](../backlog/BACKLOG.md)).
- **Not playtested by hand yet:**
  - Death Match with 2 real players.
  - Whether a technique kill counts in Death Match with PvP off. The code says yes, since the caster is the damage source.
  - Violet Guardian in survival.
- **Huy's game profile** "26.3 Fabric Essential" has an older build. Run `scripts/install.sh` only when Huy asks in that message.
- **Many old merged branches** exist locally and on origin. They are harmless; delete them only if Huy asks.

## How the team works (short)

Full text: [workflow/team.md](../workflow/team.md).

- **Manager (Opus):**
  - Writes a plan, or a decision line for small tweaks.
  - Starts **one** Sonnet dev per batch.
  - Reads the dev's ≤10-line report.
  - Merges into `main` and pushes.
  - Does not re-run client tests the dev already ran green.
- **Dev (Sonnet):**
  - Works in a worktree on `feat/P-NNN-dev` and writes the code and tests.
  - Runs the full `./gradlew :permahearts:build` once (client screenshot tests only if UI changed).
  - Merges into `feat/P-NNN-<slug>`.
- **Merge every finished plan into `main` right away.** P-010 sat unmerged for a day and fell 118 commits behind.
- **Git and GitHub:**
  - Commits are made as `huydangtruong` (local git config of this repo).
  - Push and release with `GH_TOKEN="$(gh auth token -u huydangtruong)"`.
  - Never change the global git or gh config, and never push `main` from a dev.
- **Tokens:** auto-compact is set to 240k in `.claude/settings.local.json`. Suggest that Huy opens a fresh session after each release.

## Keeping this file current

At the end of every batch, the manager edits only the sections that changed:
- **Releases:** what is on `main` but not released.
- **In flight:** each plan with its branch, worktree, and how to resume it.
- **Open:** anything Huy deferred.

Add new decisions to [01-decisions.md](01-decisions.md), not here.
