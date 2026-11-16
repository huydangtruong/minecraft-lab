# Status — where work stands

> Tóm tắt cho Huy: file này ghi việc đang làm dở và bước tiếp theo, để một phiên Claude mới đọc là làm tiếp được.
> Manager cập nhật file này cuối mỗi đợt việc (merge, phát hành, quyết định mới).
> Snapshot: 2026-11-17, after release 0.27.0.

A fresh agent reads, in order: this file → [AGENTS.md](../../AGENTS.md) → [workflow/team.md](../workflow/team.md) → the plan of the work in flight. Do not read the whole repo.

## Releases

| What | Where |
|---|---|
| Last release | `permahearts-v0.27.0` (GitHub Releases on `huydangtruong/minecraft-lab`), `mod_version=0.27.0` |
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
| 0.21.0 contains | P-035: God Slayer chases instead of teleporting early (D100), look fixes with wings and 45° lean (D101), orange Crimson Master notice, red 60 s countdown with heartbeat and red edge (D102), Swiftness +5% (D103), `/boss freeze stand\|fly\|fly_forward\|off` (D104). Not playtested by hand |
| 0.22.0 contains | P-036: God Slayer look round 3 (D105): head attached, 95% wide, shorter above the eyes (13 rows, about 7%, integer boxes); longer, wider, flatter horns; katana edge forward with the tip slant; one white trapezoid hair piece; two-bone ^ ^ wings with 11 feathers per side from the approved mockup. Not playtested by hand |
| 0.23.0 contains | P-037: God Slayer look round 4 (D106): thinner, narrower, lower head; slimmer horns; fixed mane in two pieces (upper on the head, lower on the chest, 10° back, past the belt); head turn clamped to 15° yaw / 10° pitch; arms more forward, longer upper arm. Countdown (D107): no heartbeat, drum every second for 60 s, thin pale red edge without square bands |
| 0.24.0 contains | P-038: simple 50/50 reward roll (D108), Heart Elixir from gold boxes up, God Slayer kill gives HeartXP, Enlightenment +24. P-039: Illusion gives the whole feed stack, work radius +4 per 5 levels (D109); boss level reward 50 + 50 × damagers split by damage (D110). P-040: God Slayer skill director + Fireball Lv 2, Katana throw Lv 3, Flame breath Lv 4, Eye beam Lv 5 (D111). 30 random lines each for win / loss / scorn (D113). Not playtested by hand |
| 0.24.1 contains | Technique list grouped: attack, control/defense, movement, allies (D114) |
| 0.25.0 contains | P-041 effect icons with time left (Undying Jutsu, Spirit Phase, Cloud Travel, Aqua Marine) and purple Death Match sky (D115, D116); P-042 Notes panel (D117); Shadow Walk renamed Spirit Phase (D118). Not playtested by hand |
| 0.26.0 contains | P-043: boss Ghast survives reflected fireballs (D119), Notes fixes (no extra Cultivating tabs; wrap, author color, X button, 32 chars, side tabs — D122), clones fetch seeds from chests (D120), Quantum Leap cooldown 40 s −1 s/level (D121), summons attack the Death Match opponent. Server + client game tests green. Flaky: `farmTakesSeedsFromTheChestAndPlantsEmptyFarmland` (rerun) |
| 0.27.0 contains | P-044: clones skip unreachable crops after 3 s (whole field 30 s), farm height ±6, harvest the last two stages with ripe drops (D123); installer prints the release notes after a good install and exits, Windows waits for Enter (D124; ps1 not run, no pwsh). |
| On `main`, not released | Nothing |

Release steps: [workflow/release.md](../workflow/release.md). Release **only when Huy says so**.

## In flight

- **0.24.0 / 0.24.1 released.** Waiting for Huy to play it.
- **0.25.0 released** (P-041, P-042, D118). Waiting for Huy to play it.
- **Next:** BL-024/D112 (God Slayer arrives 2–5 days after the 4th Crimson Master leaves; Master pauses until the fight ends). P-040 leftovers: Katana → teleport behind → Flame breath chain, `/boss skill` command, a real blade model and guardian-style beam. Then the rest of D97's skills (minions, blind, arena, self-destruct).
- **BL-014** (player technique fire burns the God Slayer) is next in line with P-033.

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
