# Status — where work stands

> Tóm tắt cho Huy: file này ghi việc đang làm dở và bước tiếp theo, để một phiên Claude mới đọc là làm tiếp được.
> Manager cập nhật file này cuối mỗi đợt việc (merge, phát hành, quyết định mới).
> Snapshot: 2026-11-15, after release 0.14.0.

A fresh agent reads, in order: this file → [AGENTS.md](../../AGENTS.md) → [workflow/team.md](../workflow/team.md) → the plan of the work in flight. Do not read the whole repo.

## Releases

| What | Where |
|---|---|
| Last release | `permahearts-v0.14.0` (GitHub Releases on `huydangtruong/minecraft-lab`), `mod_version=0.14.0` |
| 0.12.0 contains | Warrior Angel (P-021, D62, D63), Aqua Marine (P-022, P-023, D64, D69), chieftain XP x10 and damage x2 (D65, D66), stronger Strength/Swiftness/Mining elixirs (D67), gold quest 64 (D68) |
| 0.13.0 contains | Illusion technique (P-024, D71); D72 numbers (Warrior Angel, Cloud Travel, Undying Jutsu, Heaven Blast, Fireball, Frozen Breath); chieftain health 200 and the leader-bonus fix; Strength +2 (D70); "Death Match (PvP)" line |
| 0.14.0 contains | Illusion work modes and hostile mobs targeting the clones (P-025, D73); Quantum Leap rename (turquoise) and Cosmic Warping portals (P-026, D74, D75); quest reward boxes (P-027, D76); technique slots cost 64, 128, 256, 512 emeralds (D77); clones plant bare farmland and pick up wool near the sheep. Not playtested by hand |
| On `main`, not released | Yellow "Immortal Cultivation <version>" game rule group in Create World, yellow key category in Controls (D78); **fix: reward box cards could not be clicked** (any box), now left or right click |

Release steps: [workflow/release.md](../workflow/release.md). Release **only when Huy says so**.

## In flight

- **Windows installer bug** (reported by Huy's friend): picking a profile other than "26.3 Fabric Essential" says SUCCESS, but that profile's mods folder still has the old jar. Reading `minecraft-lab.ps1` (`Find-Profiles` / `Select-Profile`) found nothing yet. Waiting for the friend's `Mods folder:` line and that profile's Game directory.

## Open, not scheduled

- **BL-004**: a command to summon the Crimson Master with a chosen quest ([backlog/BACKLOG.md](../backlog/BACKLOG.md)).
- **Not playtested by hand yet:**
  - Death Match with 2 real players.
  - Whether a technique kill counts in Death Match with PvP off. The code says yes, since the caster is the damage source.
  - Violet Guardian in survival.
  - Warrior Angel and Aqua Marine (speed, underwater view) in survival.
  - Illusion: clones fighting mobs, and with a second real player (PvP off, does it confuse who is real).
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
