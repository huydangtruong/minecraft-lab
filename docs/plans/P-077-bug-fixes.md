# P-077 — Bug fixes from the P-076 audit

> **Tóm tắt cho Huy (VN, ≤ 3 dòng):** sửa 15 lỗi mà audit P-076 tìm ra (rương quà shift-click mất đồ, ô thùng rác không nhận chuột phải, chuột giữa/chuột bên bấm nhầm nút, /boss freeze không dừng hẳn, chiêu diện rộng đánh trúng đồng minh, ...). Mỗi lỗi một commit, có test khi làm được. Quyết định D219 trở đi do manager chốt thay Huy, Huy có thể đổi lại.

- **Status:** in-dev
- **Mod / decision:** `immortal-cultivation`, D219+ · **Branch:** `feat/P-077-bug-fixes` (cut from the `feat/P-076-audit-refactor` tip) · **Depends on:** P-076

## Goal

Fix B1–B15 of [P-076](P-076-audit-refactor.md) as scoped by the manager on Huy's behalf (2026-11-23). One commit per bug.

## Fixes

| Bug | Fix | Test |
|---|---|---|
| B1 | Shift-click on a done quest reward gives the real (rolled) box, the same as a normal click | game test (wanderer) |
| B2 | Trash slot accepts left (1) and right (3) | compile + existing trash test |
| B3 | Only the left click triggers Upgrade, Reset, Passive "+" and Soul Grasp Release | compile (client) |
| B4 | `/boss freeze` adds the full 5-tick interval, so the hunt timer truly pauses | god_slayer game test if feasible |
| B5 | One client `DISCONNECT` hook resets ClientGrasp, GodSlayerWarningHud, ClientSkills charge/grace and the tab scroll/detail | compile + client suite |
| B6 | `SERVER_STOPPED` resets the BloodMoon, QuestBoss and DangerMobs statics | full build |
| B7 | CropOwners drops an owner when the block at that pos is no longer a crop (checked on use/grow) | passive game test |
| B8 | GraspScreen clamps scroll after the list changes | compile + client suite |
| B9 | Quantum Leap dismounts only after the teleport succeeds | thousand_mile_step game test |
| B10 | Soul Grasp call uses `SafeTeleport.spotOrNull`, like the follow-teleport | grasp game test |
| B11 | AoE techniques (Seism, Repulse, Thunder, burn/freeze fields) skip the caster's allies (clones, guardian, angel, grasped mobs) and other players when PvP is off; the heal circle heals only the caster, allies and players, never hostile mobs. Uses the existing friend filter; the 8 variants are not merged | area game tests |
| B12 | Warp applies the yaw facing; `freeSpot` rejects lava | warp game test |
| B13 | LifeStealer marks lifted mobs with a tag; on load, a tagged mob loses NoGravity and the tag | lance game test |
| B14 | `/treasure start` during an active event cleans up the old chest and guards first | treasure game test |
| B15 | Soul Grasp LIST and RELEASE work at technique level 0 | grasp game test |

## Tests

- Test scope per bug (team.md), then full `./gradlew :immortal-cultivation:build` and one `runClientGameTest` at the end.

## Docs

- `01-decisions.md`: one Vietnamese row per behaviour change, from D219.
- `03-status.md`.

## Done when

- [ ] One commit per bug, build green, client suite run once, branch pushed. No merge, no release.
