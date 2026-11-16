# Status — where work stands

> Tóm tắt cho Huy: file này ghi việc đang làm dở và bước tiếp theo, để một phiên Claude mới đọc là làm tiếp được.
> Manager cập nhật file này cuối mỗi đợt việc (merge, phát hành, quyết định mới).
> Snapshot: 2026-11-15, after release 0.20.0.

A fresh agent reads, in order: this file → [AGENTS.md](../../AGENTS.md) → [workflow/team.md](../workflow/team.md) → the plan of the work in flight. Do not read the whole repo.

## Releases

| What | Where |
|---|---|
| Last release | `permahearts-v0.20.0` (GitHub Releases on `huydangtruong/minecraft-lab`), `mod_version=0.20.0` |
| 0.12.0 contains | Warrior Angel (P-021, D62, D63), Aqua Marine (P-022, P-023, D64, D69), chieftain XP x10 and damage x2 (D65, D66), stronger Strength/Swiftness/Mining elixirs (D67), gold quest 64 (D68) |
| 0.13.0 contains | Illusion technique (P-024, D71); D72 numbers (Warrior Angel, Cloud Travel, Undying Jutsu, Heaven Blast, Fireball, Frozen Breath); chieftain health 200 and the leader-bonus fix; Strength +2 (D70); "Death Match (PvP)" line |
| 0.14.0 contains | Illusion work modes and hostile mobs targeting the clones (P-025, D73); Quantum Leap rename (turquoise) and Cosmic Warping portals (P-026, D74, D75); quest reward boxes (P-027, D76); technique slots cost 64, 128, 256, 512 emeralds (D77); clones plant bare farmland and pick up wool near the sheep. Not playtested by hand |
| 0.15.0 contains | Cooldown balance and Healing Light renamed Healing Domain, heals 12 → 48 hearts (D80); Cosmic Warping places a portal after a 1 s hold (D79); fix: reward box cards could not be clicked (any box), now left or right click; yellow "Immortal Cultivation <version>" game rule group and yellow key category (D78) |
| 0.15.1 contains | Cosmic Warping cooldown 90 s (Lv 1) to 40 s (Lv 20) (D81) |
| 0.16.0 contains | Thunder Strike, 15th technique (P-029, D86); Illusion chopping fix with leaf breaking (D82), farming without tilling (D84), job from hotbar slot 1 (D85); new technique picking, level bars and "i" button (P-028, D83). Not playtested by hand |
| 0.17.0 contains | Crimson Master every 5 days, new quest weights and mobs (D87, D88); emerald reward box, pick 2, Bottle of Enlightenment (D89); box tier random at claim and hidden (D90) (P-030). Not playtested by hand |
| 0.18.0 contains | No "+" buttons, "i" at the right (D91); /testmode full and reset (D92); detail page without level bar, short Thunder Strike text, fixed Thunder icon (D93) (P-031). Fireball afterburn, burn 2 to 5 (D94); level next to the name, Thunder Strike cooldown 26 s (D95). Not playtested by hand |
| 0.19.0 contains | God Slayer boss Lv 1: passives, hunt, Crimson Master trigger, reward, `/boss summon`, `/boss giveup`, `/boss level` (P-032, D96, D97). Not playtested by hand |
| 0.20.0 contains | God Slayer 5 blocks tall, new stance model (D98); Heaven Blast stuns mobs 1 s + 0.1 s per level (D99) (P-034). Not playtested by hand |
| On `main`, not released | Nothing |

Release steps: [workflow/release.md](../workflow/release.md). Release **only when Huy says so**.

## In flight

- **Next: P-033** God Slayer skills Lv 2 to 10 through `GodSlayerSkills` (D97). No plan yet.
- **P-035 done on branch `feat/P-035-playtest-fixes`, not pushed, not released:** BL-009..BL-012 (D100..D103) and `/boss freeze` (D104). Waiting for Huy to playtest it.

- **Windows installer bug** (reported by Huy's friend): picking a profile other than "26.3 Fabric Essential" says SUCCESS, but that profile's mods folder still has the old jar. Reading `minecraft-lab.ps1` (`Find-Profiles` / `Select-Profile`) found nothing yet. Waiting for the friend's `Mods folder:` line and that profile's Game directory.

## Open, not scheduled

- **Flaky game tests left:** `golem_game_test_guardian_attacks_amonster_near_the_owner` and `thunder_game_test_a_pig_in_the_blast_becomes_azombified_piglin` failed once each in 10 runs. Not investigated. (Chop and afterburn flakes fixed.)

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
