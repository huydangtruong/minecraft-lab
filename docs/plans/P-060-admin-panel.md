# P-060 — /admin test panel

> **Tóm tắt cho Huy (VN, ≤ 3 dòng):** gõ /admin mở pop-up: trái là các tab nút/công tắc cho lệnh test, phải là ô liệt kê các lệnh sẽ chạy. Bấm Confirm thì pop-up tắt và các lệnh chạy lần lượt.

- **Status:** ready
- **Mod / decision:** `immortal-cultivation`, D155 · **Branch:** `feat/P-060-admin-panel` · **Depends on:** none

## Goal
One command, `/admin`, replaces typing the test commands. The panel only *builds command lines*; on Confirm the server runs them as if the player typed them. No new game logic.

## Scope
- In: `/admin` command, open-screen packet, `AdminScreen` (two panels), run-commands packet, pure `AdminDraft` class, tests.
- Out: changing any existing command (`/hearts`, `/testmode`, `/technique`, `/boss`, `/quest`, `/danger`, `/treasure`); new game features; non-OP use.

## Behaviour
- `/admin`: requires permission level 2 (same check as `/hearts set`). Non-OP gets a failure message. OP: server sends an S2C packet, client opens `AdminScreen`.
- **Left panel** — same look as the Cultivation screen (see `client/TechniquesTab.java`, `PassivesTab.java`, `StatsPanel.java` for the panel frame, tab style and colours; reuse their helpers, don't copy-paste big blocks). Tabs and controls (all target `@s`):
  - **Player**: hearts (number + Add → `hearts set @s N`), HeartXP (`hearts xp @s N`), Testmode full / reset, emeralds (`technique emeralds @s N`), Clear cooldowns.
  - **Techniques**: scrollable list of all techniques (from the technique registry/enum, not hard-coded); per row a level slider 0–20 → `technique set @s <id> <lv>`; a "Cast" button → `technique cast <id> <lv>`.
  - **Crimson Master**: Summon, Skip, Complete 1–4, Give up, Reset (`quest …`).
  - **God Slayer**: Summon (`boss summon @s`), level 1–5, Freeze stand / fly / fly_forward / off, Give up.
  - **Danger**: level slider 0–30 → `danger set N`; Info; elite: affix toggle chips + Summon → `danger elite <affixes>`.
  - **Treasure**: Status, Start, Here + group picker, Lineup 0–3, Give up.
  - **World**: on/off switches for every mod game rule (read ids from `ImmortalCultivation`, `danger/Danger` etc.; show current value — send the values in the open packet), number field for `danger_spawn_ceiling`; vanilla shortcuts: day / night, clear / rain, creative / survival, kill hostile mobs within 32 blocks.
  - Check exact argument syntax of each command in its class before writing the line; check the 26.3 `gamerule` / `time` / `weather` / `gamemode` / `kill` syntax with a quick dev-world run or `javap`, add findings to `02-findings.md`.
- **Right panel**: multi-line text area with word wrap (vanilla `MultiLineEditBox` or equivalent — verify name with `javap`), one command per line, no leading `/`. Editable by hand. Each left click appends a line; a switch/slider for the same key (same game rule, same technique level, danger level, hearts) **replaces** its earlier line instead of adding one. Below it: **Confirm** and **Clear**.
- **Confirm**: closes the screen, sends C2S packet with the lines. Server re-checks permission level 2, then runs each non-empty line in order with `server.getCommands().performPrefixedCommand(player.createCommandSourceStack(), line)` (verify names). Normal command feedback goes to chat. Limits: max 64 lines, 256 chars each (drop extra, tell the player).
- **Esc / X**: close without running; keep the draft text in a client static so the next `/admin` shows it again.
- UI text in English, like the rest of the mod UI. Keys under the `permahearts` namespace (D132).

## Files
| File | Where | Change |
|---|---|---|
| `admin/AdminCommand.java` | main | `/admin`, open packet |
| `admin/AdminNetworking.java` | main | S2C open (+ game rule values), C2S run lines, permission check, limits |
| `admin/AdminDraft.java` | main | pure Java: list of lines, append, replace-by-key, parse/serialize, limits |
| `client/AdminScreen.java` (+ small tab classes if needed) | client | the screen |
| registration | `ImmortalCultivation`, `ImmortalCultivationClient` | register command, payloads, screen opener |
| `assets/permahearts/lang/en_us.json` | resources | labels if keys are used |

## Tests
- JUnit `AdminDraftTest`: append, replace-by-key, clear, limits, strip leading `/`, blank lines skipped.
- Server game test: OP player's lines run in order (e.g. `hearts set @s 30` then `danger set 5` changes both); non-OP request runs nothing.
- Client game test (one, muted mod as usual): open `/admin`, click a few controls, screenshot shows lines on the right; Confirm closes the screen and the change applies.
- Scope: large (new screen + network) → `./gradlew :immortal-cultivation:build` + `runClientGameTest` once.

## Docs
- `docs/system/03-status.md`: "On `main`, not released" + In flight line.
- `docs/mods/` spec of the mod: short `/admin` section.

## Commit
Small coherent commits on `feat/P-060-admin-panel`, English messages, push the branch (never main).

## Done when
- [ ] `/admin` opens the panel for OP, refuses non-OP
- [ ] Every tab's controls write correct lines; switches replace, not duplicate
- [ ] Confirm closes and runs lines in order; Esc keeps the draft
- [ ] build + client game test green, screenshot looked at

## Risks / ask first
- If a vanilla multi-line edit box with wrap does not exist on 26.3, build a simple read-only wrapped list with per-line delete (×) instead and say so in the report.
