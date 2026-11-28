# Status — where work stands

> Tóm tắt cho Huy: file này ghi việc đang làm dở và bước tiếp theo, để một phiên Claude mới đọc là làm tiếp được.
> Manager cập nhật file này cuối mỗi đợt việc (merge, phát hành, quyết định mới).
> Snapshot: 2026-11-28, after release 0.49.0 (P-085, P-086).

A fresh agent reads, in order: this file → [AGENTS.md](../../AGENTS.md) → [workflow/team.md](../workflow/team.md) → the plan of the work in flight. Do not read the whole repo.

## Releases

| What | Where |
|---|---|
| Last release | `immortal-cultivation-v0.49.0` (GitHub Releases on `huydangtruong/minecraft-lab`), `mod_version=0.49.0`. All old tags `permahearts-v*` were renamed to `immortal-cultivation-v*` on 2026-11-17 (D132); the installers still accept both prefixes |
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
| 0.28.0 contains | P-045 Z X C V skill keys, one scroll ring (D125); /testmode full gives carrots, emeralds, 200 levels (D126); skill bar in the bottom-right corner with dark cells and small black key labels (D127, D128, D130; P-046/P-051); Spirit Walk rename, sight 16 → 64, time-left line (P-048, P-049, D129); P-050 shared-note chat line and Crimson Master farewell levels, completed quests × 8 (D131). Server + client game tests green. Not playtested by hand. Unused trial branch: `feat/P-047-skill-bar-under-hotbar` |
| 0.29.0 contains | P-052 rename to immortal-cultivation, data namespace `permahearts` kept (D132); skill bar 4 px from the right and bottom edges (D133). First jar with id `immortal-cultivation`: the installers remove the old id-`permahearts` jar. Installed on Huy's profile 2026-11-17; a copy of MegaHeart loaded and saved with identical mod data. Server game tests green; flaky `farmTakesSeedsFromTheChestAndPlantsEmptyFarmland` failed 1 of 4 runs (clone dead at tick 1202) |
| 0.30.0 contains | P-053 Soul Grasp, 16th technique: tame mobs with a gold tether, list screen with hearts/armor/damage icons, Follow/Stay/Guard/Call/Release, villagers and passive mobs fight, owner-given gear (D134–D136). P-054 Danger level 0–30 after each Crimson Master visit, more and stronger mobs, day spawns, armor from 21, elites from 20 with 5 affixes (D137–D139, D141). P-055 1 vanilla XP per ripe crop, Illusion harvest/breed XP to the owner (D140). Server game tests green (186). Not playtested by hand |
| 0.31.0 contains | P-056 Seism (17th technique: Impact boulder, Wave from Lv 3) and Crystal Prison (18th: crystal cage, disarm) (D142–D146); fixes: clones/guardians/angels ignore the owner's grasped mobs, grasped mobs do not burn in the sun. Server game tests green (201). Not playtested by hand |
| 0.31.1 contains | Fix: unclaimed reward boxes go into a chest on the ground when the Crimson Master leaves (D147). Server game tests green (202) |
| 0.32.0 contains | P-057 Passives tab (D148): 5 passives x 10 levels, 5 XP levels per upgrade. Server game tests green (208), client test `PassiveClientTest` green |
| 0.33.0 contains | Passives: on/off switch left of "+" instead of clicking the gem; XP gain bonus also on crop harvest XP and clone breeding XP (D148); 6th passive Crop growth, +20%/level on crops the player or their clones plant (P-058, D149); gems keep their colour when switched off (D151); Deadly Treasure event with Mega guards, Crimson base items trimmed (P-059, D150); locked chest empty until opened, restored if destroyed, Mega Endermen do not teleport (D152); reward box cards 60% elixir (D153). Server game tests green (216). Not playtested by hand |
| 0.33.1 contains | Seismic Impact boulder falls 0.5 s instead of 0.2 s (D154). Server game tests green (216) |
| 0.34.0 contains | P-060 `/admin` test panel: tabs on the left, command lines on the right, Confirm runs them (D155). P-061 Solar Lance, 19th technique: hold-to-fire laser 256 blocks, damage doubles every 3 s, 12 s max, 45 s cooldown (D157, D159); Life Stealer replaces Life Steal Blade (id `blade`, levels kept): 16-block drain with cap, Bleeding and -80% speed for 2 s, 24 s cooldown (D158, D159); Soul Grasp sky pillar instead of the hand tether (D156). SeismClientTest fixed for D154. Server + client game tests green. Not playtested by hand |
| 0.34.1 contains | Solar Lance fires from the front of the chest. Test scope checklist per area in team.md, `runGameTest -Pgt` filter. |
| 0.35.0 contains | P-062: /admin Player tab Level field and No cooldown switch (D160, `/technique nocooldown`); `TestModeGameTest` registered and green. P-063: Solar Lance beam starts at screen bottom-centre by FOV and breaks blocks, one per 6 ticks, never unbreakable ones (D161). Area tests green (admin, test_mode, technique_game, lance). Not playtested by hand |
| 0.35.1 contains | Heaven Blast stun 2 s + 0.25 s/level (D162). Life Stealer: blood flies 1.5 s (D162), drains 30% at Lv 1, +2%/level (D164). Solar Lance drawn over the HUD in first person, grows to 2.5x width by 12 s, harsher sound, shorter Damage text (D163). Soul Grasp followers keep their own spot 2.5-3.5 blocks from the owner, fixed odd-phase mobs never following (D165). Deadly Treasure chest has 2 enchanted golden apples (D166). Area tests green. Not playtested by hand |
| 0.36.0 contains | Grasped witch throws healing at the owner and allies, harmful potions at enemies (D167); Soul Grasp "All" row: Follow, Stay, Guard, Call, Release all (D168). Spirit Walk 10 s + 1 s/level, sight 24-96 blocks; followers (grasped mobs, guardians, clones, angels, pets) never teleport into walls while the owner phases (D169), and only onto safe ground, never into the air or water (D170). Diamond reward box: pick 2 of 5 (D171). Deadly Treasure takes every other Crimson Master visit, chest adds 32 golden carrots, always 8 random stacks (D172); lasts 2 days; God Slayer on every 4th special event (D173). Area tests green. Not playtested by hand |
| 0.37.0 contains | P-064 Crystal Mayhem, 20th technique (D174): left click Shard Projection (light-based damage, 20 shards, 10 s amethyst clusters), hold right click Light Strike Reflection (chain of lit buddings that burn 1 + 0.5/level and slow, 20 s, then 30 s cooldown). Server + client game tests green. Not playtested by hand |
| 0.38.0 contains | P-065 Item guard, 7th passive (D176): items do not break, they become broken with a durability debt of 100% (Lv1) down to 10% (Lv10), unusable until the anvil or Mending pays it; crafting-grid repair of a broken item refused, grindstone vanilla. Solar Lance cooldown 36 s (D175). Fix: flaky sun-burn grasp test. Server game tests green; client suite green once, new screenshots not looked at. Not playtested by hand |
| 0.39.0 contains | P-066 Life Stealer (D180): 4 visual bats fly in, lift the target 2 blocks (ceiling-safe, bosses not lifted), drain in 10 pulses over 2 s, fly out, target falls; drains 36% at Lv 1, +2%/level (D181). Passives tab scrolls with the original row size (D177). Technique upgrades cost a flat 5 levels (D178). Item guard debt starts at 0 and grows with wear up to the level cap (D179). Crystal Mayhem hits other players only with PvP on (D182). Area tests green; client suite not run; bats and scroll not looked at in game |
| 0.40.0 contains | Solar Lance and Fireball burn reset the real vanilla damage cooldown (D183). God Slayer takes its own 5th special-event slot after every 2nd Deadly Treasure (D184). A grasped raider leaves its raid and never joins another (D185). /admin: Give full netherite button and Elixirs tab to lower or raise each bonus (D186). Deadly Treasure purple top bar with coordinates, guards left, time left, and a warning 2 minutes before it vanishes (D187). Area server tests green. |
| 0.41.0 contains | D188 hold-R technique wheel (Attack/Defense/Movement/Allies, click assigns or swaps slots, red text when the slot cools). D189 Crystal Mayhem gives a fresh 20 shards with clusters after the cooldown. D190 natural illagers never join raids; raid mobs glow. D191 grasped patrol vindicators follow and fight at night. D192 vexes of a grasped evoker are on the owner's side. Area tests green; not tried in a real game. |
| 0.42.0 contains | D193 wheel opens from the cross on the lowest ready slot, slot row on top, only the edited slot's technique darkened, scroll between slots. D194 a grasped evoker's vexes leave other players (outside Death Match) and villagers alone. D195 grasped mobs never stop the owner sleeping. D196 the wheel stays open after a pick until R is let go. D197 a working illusion stores what it carries before it vanishes. D198 Call and Call all reach grasped mobs in other dimensions and unloaded chunks. Area tests green; not tried in a real game. |
| 0.43.0 contains | D199 Quantum Leap charge bar shows the block distance. D200 villagers and the wandering trader do not fear grasped mobs. D201 Solar Lance slows and pushes back a little. D202/D205 Soul Grasp icon = front-facing sheep with swirl eyes. D203/D204 health row = one heart + rounded bar as wide as the armour bar. D206 danger level 0..50 (old saves x5/3 minus 5, D209). D207 XP follows a boosted mob's health. D208 illusions take leftovers to the next chest with room. D210 reward boxes: gold 2 of 4, diamond 3 of 6, emerald 4 of 8. D211-D213 bigger diamond/emerald/pearl cards, emerald books Mending, Sharpness V, Protection IV, Unbreaking III, Looting III, Efficiency V. D214 Deadly Treasure: 64 coal, 32 XP bottles, 32 pearls, Blast Protection IV. Area tests green; not tried in a real game. |
| 0.44.0 contains | D215 Soul Grasp icon without the two black top-corner dots. D216 health row keeps the colour of full health (poison/wither/frozen still recolour). D217 Notes commented out; a trash slot in its place destroys the carried stack (plan P-074). D218 `/quit`: 10 s countdown, /quit again cancels, then save, kick friends and close the host's game (plan P-075; the real close never run, only compile + javap checked). Area tests green; not tried in a real game. |
| 0.45.0 contains | P-076 refactor, behaviour identical (R1-R10, R13; R11 UI helpers and R12 Notes switch deferred, wait for Huy). P-077 fixes B1-B15 (D219-D233): shift-click quest reward gives the real box, trash slot right click, only left click on Upgrade/Reset/+/Release, /boss freeze really pauses, client/server statics reset, stale crop owners dropped, Grasp scroll clamp, Quantum Leap dismount only on success, Grasp Call and Warp on safe ground, area techniques skip friends (heal circle skips hostiles), Warp yaw, LifeStealer NoGravity cleanup, /treasure start cleans the old event, Grasp list/Release at level 0. Five stale client tests fixed. Full build green (290 JUnit, 292 game tests), client suite 27/27; not tried in a real game. |
| 0.46.0 contains | D243 Solar Lance 12 s (Lv 1) to 24 s (Lv 20). D244 gear panel for grasped mobs (right click). D245 a vanishing Illusion stores its load in the nearest chest. D246 Undying Jutsu forbids weapon and hand hits. P-078 Purgatory (D234-D240, D247): 4th Cultivation tab, solo and team floors in a 64³ bedrock box in `permahearts:purgatory`, 10 min survival, 30+n levels and n runes, watching, `/purgatory`; bosses and events wait. Runes and Rune Forge (D241, D242, D247): runes add enchantment levels past conflicts, inscribe 5 levels per rune level, merge free. Full build green (304 game tests), client suite green; team runs and the real game not tried by hand |
| 0.46.1 contains | D248 Rune Forge recipe in the recipe book (unlocks with a diamond or an iron ingot). D249 Undying Jutsu countdown on the skill bar. D250 Purgatory Give up at the bottom centre of the inventory screen (E). Fix: the edge tabs work again from the Purgatory tab. Client suite green |
| 0.46.2 contains | D251 Purgatory floors last 6 min, 5 min wait, each winner gets 3 x players + floor runes. D252 a totem in either hand saves a Purgatory fighter and the run goes on. D253 mob health x2.5 and damage x2 on floor 10 (linear). Purgatory game tests 8/8 |
| 0.47.0 contains | P-081 Purgatory wait (D254); P-082 Purgatory dungeon: generated rooms in a bedrock-skinned dungeon, sealed/pillar/treasure rooms, boss in the last room, 10 min clock, 30 s after the win (D255-D267, D269); runes hidden in the chests, boss drops XP on the ground, no rune bag (D271); team waves x1.8 for 2 players +0.8 per extra (D273); +1 stairs / +3 boss room mobs (D274). P-083 Time Rewind, 20th technique: 16-block time stop, wheel rewinds mobs, projectiles, blocks and the caster; caster held until they move or click (D268, D270, D272). Full build 315/315, client tests green. Team Purgatory not played with 2 real players |
| 0.48.0 contains | P-084: summon/grasped/projectile kills count in the Death Match (D275); rename portals in the Cosmic Warping menu (D276); Throw Power rune (D277); new rune look with glyph and level number (D278); Healing Domain 64 hearts at Lv 20 + 3% max health/s (D279); Deadly Treasure ending raises danger (D280); Mega guards get danger strength (D281); witch potions scale with the witch's danger strength (D282). Server game tests green (320), flaky God Slayer `hunt_flow` on reruns. Not playtested by hand |
| 0.49.0 contains | P-085 Purgatory: boss room locked until every sealing room is done (D284), doors wait for a teammate behind (D283), 6 big 2x2 halls with 4 waves and 2 chests, 2 halls from floor 10 (D285). P-086 God Slayer: x1.3 per level (D286), 6 fireballs, 3-katana fan with blood, narrower longer flame +50%, eye beam on land +100% (D287), Lv 6 Blast: orange flash 1.5 s, radius 8, 50 damage, breaks blocks, 3 s red blindness (D288). Server game tests 325 green. Not playtested by hand (team doors need 2 real players) |
| On `main`, not released | Nothing |

Release steps: [workflow/release.md](../workflow/release.md). Release **only when Huy says so**.

## In flight

- **P-087 in flight** on `feat/P-087-old-rune-look` (from main after 0.49.0): old runes show the glyph and number look everywhere (D289). Client suite green, installed; not released. Also: the D283 team game test now picks a floor with a locked room next to an open one (random floors could have none). Known flaky: `purgatory ... a_seal_pillar_room_opens_when_the_pillar_breaks` ("one pillar: 0") fails now and then when run with the other tests, always passes alone; cause not found yet.
- 0.49.0 released 2026-11-28 (P-085, P-086, D283-D288). Branch `feat/P-086-god-slayer-power` (contains P-085) merged into main.
- 0.48.0 released 2026-11-28 (P-084, D275-D282). Branch `feat/P-084-match-summon-kill-warp-rename` merged into main.
- 0.47.0 released 2026-11-28: Purgatory dungeon and Time Rewind playtested only partly by Huy (solo); team Purgatory needs 2 real players.
- Waiting for Huy: R11 (shared UI helpers, may move pixels) and R12 (Notes behind a feature switch) from [P-076](../plans/P-076-audit-refactor.md). Found, not fixed: `Warp.go` still calls `stopRiding()` before `teleportTo` (same pattern as B9).
- **All plans up to P-086 are released** (last: 0.49.0). Released but not playtested by hand: everything from 0.13.0 on unless the release row says otherwise.
- **Next ideas:** BL-024/D112 (God Slayer arrives 2–5 days after the 4th Crimson Master leaves; Master pauses until the fight ends). P-040 leftovers: Katana → teleport behind → Flame breath chain, `/boss skill` command, a real blade model and guardian-style beam. Then the rest of D97's skills (minions, blind, arena, self-destruct). BL-014 (player technique fire burns the God Slayer).
- **Windows installer bug** (reported by Huy's friend): picking a profile other than "26.3 Fabric Essential" says SUCCESS, but that profile's mods folder still has the old jar. Reading `minecraft-lab.ps1` (`Find-Profiles` / `Select-Profile`) found nothing yet. Waiting for the friend's `Mods folder:` line and that profile's Game directory.

## Open, not scheduled

- **Flaky game tests left:** `GodSlayerGameTest.huntFlow` ("the boss followed into the Nether", failed once in a full run on 2026-11-27, passes alone 2/2); `IllusionWorkGameTest.farmTakesSeedsFromTheChestAndPlantsEmptyFarmland` (the clone sometimes dies; failed 2 of 4 full runs during P-076). Golem guardian and Thunder pig were deflaked in P-076 R3. Grasp "not where it was seen" and "unloaded chunk comes when called" still fail now and then in full runs (pass alone, also on the P-076 base). A spawned zombie sometimes has 20.9 max health; P-077 pins it in the Lance and Seism helpers, `hostile_kill_gives_xp` may still see 21 XP.
- **Client suite** had been red on main since D178/D180/D188 (stale expectations); fixed in P-076, now green.

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
  - Runs the full `./gradlew :immortal-cultivation:build` once (client screenshot tests only if UI changed).
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
