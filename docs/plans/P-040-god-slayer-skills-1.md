# P-040 — God Slayer: first 4 skills and the skill director (BL-023, D111)

> **Tóm tắt cho Huy (VN):** Lv 2 Hỏa cầu, Lv 3 Ném đao, Lv 4 Phun lửa, Lv 5 Tia mắt dưới nước; boss ra chiêu theo túi xáo ngẫu nhiên, xen cận chiến, không dồn chiêu.

- **Status:** ready · **Branch:** `feat/P-040-boss-skills` · **Decision:** D111 (replaces D97's unlock order for Lv 2–5)
- Files: `godslayer/GodSlayer.java`, new `godslayer/GodSlayerSkills.java` (pure rules, JUnit) and a goal/director class, `GodSlayerRules.java`, the boss model/renderer only if needed for the thrown katana, `BossCommand` (or wherever `/boss` lives).
- **Time box:** Huy wants a release at 20:50. Work in this order and **commit after each skill works**: director + Fireball → Katana throw → Flame breath → Eye beam. Whatever is not done by **20:43** stays for the next version; stop, make sure the build is green and merge what works.

## Unlock (pure)
`GodSlayerSkills.unlocked(level)`: Lv 1 none (slash only), Lv 2 FIREBALL, Lv 3 + KATANA_THROW, Lv 4 + FLAME_BREATH, Lv 5 + EYE_BEAM. All damage × `GodSlayerRules.scale(level)`.

## Director (pure core + thin goal)
- At spawn: shuffle the unlocked skills into a **bag**.
- Loop: **melee pressure** for a random 4–8 s (chase and slash as today) → pick the next skill from the bag that **fits now** (range/water rules below); a skill that does not fit stays in the bag for later; bag empty → reshuffle, but never the same skill twice in a row. If nothing fits, keep fighting in melee and try again in 1 s.
- Only one skill at a time; while a skill runs the boss does not slash or chase (it stands, turns to the target). Each skill has a short **tell** (sound + particles) before it hits.
- Teleport chain: after a Katana throw, if FLAME_BREATH is unlocked and the target is more than 6 blocks away, 50% chance to teleport behind the target and do Flame breath next (counts as that skill's turn).

## Skills (Lv 1 base numbers, × scale)
| Skill | Fits when | What happens |
|---|---|---|
| Fireball | target 6–32 blocks, line of sight | stands 0.75 s (tell: flame particles at hands, blaze sound), shoots **3 fireballs 0.25 s apart** at the target; each 8 damage, small fire, **no block damage** |
| Katana throw | target 6–24 blocks, line of sight | throws one katana (a visible spinning blade entity, or an item display) at the target, 20 damage on hit, then it flies back; 1.5 s |
| Flame breath | target within 6 blocks | cone 6 blocks, 60° for 2.5 s; 3 damage every 0.5 s and sets on fire; flame particles; no block fire |
| Eye beam | boss in water and target within 24 blocks (in water or not) | charges 1 s (tell: guardian-beam sound), then a beam from the eyes 2 s, 4 damage every 0.5 s; use the guardian beam look if easy, otherwise a particle line |

All skill damage counts as the boss (not player-caused). Skills never break blocks.

## Commands for testing
`/boss level <n>` already exists. Add `/boss skill <fireball|katana|breath|beam>` (OP) to force the next skill. Server game tests: unlock list by level; the director never picks the same skill twice in a row and never a non-fitting one (pure JUnit with a fixed RNG).

## Verify
`./gradlew :permahearts:build` green (JDK 25); known flaky game tests (thunder pig, golem guardian) may fail once: rerun. No client screenshots needed this time.

## Commit
Branch `feat/P-040-dev` from `feat/P-040-boss-skills`, commit after each skill, ff-merge back. Do not push. English commits with the Co-Authored-By line. No `__pycache__`.
