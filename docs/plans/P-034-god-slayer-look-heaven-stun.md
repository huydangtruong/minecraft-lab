# P-034 — God Slayer new look + Heaven Blast stun

> **Tóm tắt cho Huy (VN, ≤ 3 dòng):** God Slayer cao 5 block, dáng đứng tấn: hai tay mở, hai katana to mũi chĩa lên, hai chân dang, sừng lớn, tóc dài to (D98).
> Heaven Blast làm choáng quái bị hất tung 1 giây ở Lv 1, +0,1 giây mỗi level; người chơi không bị choáng (BL-008, D99).

- **Status:** ready
- **Mod / decision:** `permahearts`, D98, D99 · **Branch:** `feat/P-034-look-stun` · **Depends on:** P-032 (merged)

## Part A — God Slayer look (D98)

Today: `GodSlayerRenderer` uses a humanoid model with `Attributes.SCALE` = `BASE_BODY_SCALE (1.5) * GodSlayerRules.scale(level)`, so it grows with the level (to ~8 blocks at Lv 10). Replace with:

- **Height 5 blocks at every level** (no growth by level; strength is numbers only). Hitbox about 1.6 wide × 4.8 tall (`EntityType.Builder.sized`), eye height to match. Remove the level-based SCALE; keep `SCALE` at 1 or the base value your model needs.
- **A custom model** (`GodSlayerModel`, `LayerDefinition` built in code, its own `ModelLayerLocation`), not the humanoid model:
  - **Wide stance:** legs apart, knees slightly bent.
  - **Arms slightly open** to the sides (not hanging straight down).
  - **Two big katanas held upright, tips up**, one in each hand, each about as long as the body is tall from waist to head (≈2.5 blocks). Part of the model (cubes), not held items. Remove the katana held-item rendering if the model draws them.
  - **Big horns**: two large curved horns (several cubes stepping outward then up).
  - **Long, big hair**: thick white mane around the head and falling down the back to the waist.
  - Bulky shoulder plates, a belt/rope at the waist, a fierce face with fangs.
- **Colors stay ours** (original, not copied from any picture or game): dark red skin, white hair, black and dark-red samurai armor, gold trim, yellow glowing eyes (keep the emissive eyes layer), steel blades with a faint glowing edge.
- **Animation:** legs swing when walking; on the slash, the attacking arm raises and swings down with its katana; idle sway of the hair is optional.
- Update `tools/make_god_slayer_texture.py` for the new UV layout and regenerate the texture. Do not commit `tools/__pycache__` changes.
- Movement with the bigger hitbox: it already breaks blocks and teleports; check that the server game tests still pass (teleport, water, Nether). If a 4.8 tall hitbox makes pathing much worse, say so in the report with what you saw.
- Client test: keep `90-god-slayer-front` and add a side/three-quarter view `91-god-slayer-side`. Look at both yourself.

## Part B — Heaven Blast stun (D99, BL-008)

- `TechniqueRules.repulseStunSeconds(level) = 1.0 + 0.1 * (level - 1)` → 1.0 at Lv 1, 2.9 at Lv 20. New stat line "Stun" `x.x s` in the `REPULSE` stats (`TechniqueRules` line ~539).
- Every **non-player** living entity pushed by Heaven Blast is stunned for that time: it does not move (no walking, no AI navigation; it may still fall from the knock-up), does not attack, does not shoot. Players are never stunned.
- God Slayer: stun lasts 10% of the time (anti-cheese, like the 10% push in P-032).
- Visual: a small ring of particles (e.g. `ParticleTypes.CRIT` or stars) circling above the head while stunned.
- Implementation hint: a server-side map entity → stun end tick, ticked; while stunned, zero the AI movement (`getNavigation().stop()`, horizontal motion only from physics) and block attacks (a mixin or goal guard; pick the smallest that works on Mob). Clear on death/unload.
- Regenerate `docs/mods/permahearts/techniques-levels.md` from `TechniqueRules` (as its header says) and add the stun row.

## Tests

- JUnit: `repulseStunSeconds` at Lv 1, 10, 20; stat line present.
- Server game tests: a zombie hit by Heaven Blast does not move toward a player and does not hurt them during the stun (Lv 1: check at ~0.5 s), moves again after; a player in the blast is not stunned; God Slayer stun is ~10%.
- God Slayer tests from P-032 still green with the new size.

## Verify

```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
./gradlew :permahearts:build
```

## Docs

- `spec.md` God Slayer section: 5 blocks tall, the stance; Heaven Blast stun line.
- `playtest.md`: a line for the stun and for the new look.
- `02-findings.md`: model layer / LayerDefinition names checked with javap.

## Commit

Branch `feat/P-034-dev` from `main`, small commits, merge into `feat/P-034-look-stun`. Do not push (the manager pushes). English messages ending with the Co-Authored-By line.

## Done when

- [ ] Boss is 5 blocks tall at every level with the new stance, horns, hair and big upright katanas; two screenshots.
- [ ] Heaven Blast stuns mobs (not players) 1.0 s + 0.1 s per level; God Slayer 10%.
- [ ] `./gradlew :permahearts:build` green.
