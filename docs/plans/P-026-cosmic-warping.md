# P-026 — Quantum Leap rename + Cosmic Warping (portal network)

> Tóm tắt (VN): đổi Thiên Lý Bộ thành Quantum Leap (màu xanh ngọc). Thêm chiêu thứ 14 Cosmic Warping: chuột phải tạo cổng tím (mỗi người một màu bên trong), chuột trái mở menu chọn cổng hoặc người chơi để dịch chuyển ngay. 2 → 20 cổng, hồi 160 → 65 giây.
> Decisions: D74, D75. Branch: dev on `feat/P-026-dev`, merges into `feat/P-026-cosmic-warping`.

- **Status:** merged
- **Dev:** one Sonnet subagent. Another dev works on P-027 (reward boxes, `wanderer/`) at the same time. Do not touch quest or reward code.

## Part 1 — Quantum Leap (D74)

- `Technique.TELEPORT`: the display name becomes **"Quantum Leap"**. Keep the id `teleport`, so saves keep their levels.
- Turquoise (about `#30D5C8`) replaces the current colour everywhere the technique shows:
  - the icon in `tools/make_technique_sprites.py`; regenerate the sprite and the `focus_` item copy;
  - the particles and the thrown projectile's trail;
  - any tint in the techniques tab.
- Update `en_us.json` and any docs that name "Thousand Mile Step" (keep the history in `01-decisions.md` as it is).

## Part 2 — Cosmic Warping (D75)

### Numbers (pure Java in `TechniqueRules`, JUnit)

- `Technique.WARP("warp", "Cosmic Warping", 160, false)`. The cooldown varies with the level, so add `warpCooldownSeconds(level) = 160 - 5 * (level - 1)` (65 at Lv 20).
  - Check how cooldowns are read today. If they come only from the enum, add a per-level hook used for WARP.
- `warpMaxPortals(level)`: 2 at Lv 1 up to 20 at Lv 20, a straight line rounded down (`2 + (level - 1) * 18 / 19`). Lv 20 must give exactly 20.
- Stat lines: Portals (max), Cooldown.

### Controls

The technique is used the way the others are, when it is in the selected slot or held as the focus item.

- **Right click → place a portal.**
  - Shoot a short purple light beam where the player looks, within about 6 blocks. The portal appears on the ground there, on the first solid top face with 2 air blocks above.
  - **No cooldown** for placing.
  - At the max: no portal. Show a **red** overlay message: "Portal limit reached (N). Remove one in the warp menu."
- **Left click → open the warp menu.** It opens from anywhere; the player does not need to stand at a portal.
  - Pick a row to teleport at once.
  - The cooldown starts only after a teleport. While it runs, the menu still opens to view and delete rows, but the rows show the time left and teleporting is refused.
  - Check how left click is caught today: Life Steal Blade and other held techniques may already do it, so reuse their path. Find the client → server packet path the mod already uses.

### The portal

- **Server side:** an **entity** or a **block with a block entity**, your choice. It must:
  - stay **forever** (saved with the world);
  - be **unbreakable**: no player, explosion, piston, fluid or mob removes it, including creative players by hand;
  - be removable only from the menu (X). An owner who loses the technique (level 0, switch off) keeps the portals.
- **Size and look:**
  - About 1 block wide and ~1.5 tall, standing upright on the ground, slowly swirling.
  - **Purple outline**, and an **inner colour unique per owner**. Derive it from the owner UUID (hash → hue), and keep it away from purple so the outline stays clear.
  - A soft glow and a few purple particles; a quiet ambient hum is optional.
  - Original art only: a shader-free renderer, quads with a scrolling texture or particles.
- **Others:** other players see the portals but cannot use them. Nothing happens when they walk into one.
- **Ownership data:** the portal list per player, persistent: id, dimension, block pos, name, and creation time for ordering.
  - Store it in a world-level `SavedData`, so the list survives the owner being offline and the portals can be removed even when their chunk is not loaded.
  - Removing a portal whose chunk is unloaded removes it from the list and despawns it the next time that chunk loads.

### Auto names

At creation, name the portal after what is near it:
1. **The nearest structure** whose bounding box is within ~64 blocks (village, desert pyramid, jungle temple, ocean monument, mineshaft, stronghold, ancient city, trial chambers, nether fortress, bastion, end city…). Use `ServerLevel.structureManager()` / `getStructureWithPieceAt`, checked with javap. Make the name readable: "Village", "Desert Pyramid", "Ancient City".
2. **Otherwise the biome**, readable: "Savanna", "Cherry Grove", "Deep Dark".
3. If the name already exists for this owner, add " 2", " 3".

### The warp menu (client screen)

- Opened by a server packet with the owner's portal list and the online players (other than the owner) with their dimension and position.
- **Two sections:** "Portals" first (oldest first), then "Players".
- **Each row:**
  - two lines: the **name on top**, the **coordinates below** (`x, y, z` plus a short dimension tag when not the Overworld);
  - portal rows have an **X button at the right end** that deletes that portal, with no confirm;
  - player rows have no X.
- A header with "Portals N / max" and the cooldown left.
- It scrolls when there are many rows, and keeps the look of the mod's existing screens.
- Clicking a row sends a teleport request. The server checks the portal still exists, or the player is still online, and the cooldown. Then it teleports, across dimensions if needed:
  - to the portal: the free spot in front of it;
  - to a player: next to them.
- Play the enderman teleport sound and particles at both ends. Close the menu.

### Wiring

Everything a new technique needs, as ANGEL and ILLUSION did:
- the exhaustive switches (`TechniqueCasts`, `FocusParticles`, `TechniquesTab.description`…);
- the icon: an original 16×16 purple ring with a coloured centre, in `make_technique_sprites.py`, plus the `focus_warp` item;
- `en_us.json`;
- the renderer registration.

Description: "Right click opens a portal where you look. Left click warps you to any of your portals, or to another player."

## Tests

- **JUnit:** `warpMaxPortals` (2, 20, monotonic), `warpCooldownSeconds` (160, 65), the name suffix rule, and the colour-from-UUID (stable, not purple).
- **Game tests:**
  - **Placing:** Lv 1 places 2 portals; the 3rd is refused and the list stays at 2.
  - **Unbreakable:** a portal survives a TNT explosion and a player's `hurt`/attack.
  - **Deleting:** removing a portal through the server handler (the same code the X uses) removes the entity and the list entry.
  - **Teleporting:** to a portal moves the player next to it and starts the cooldown; a second warp during the cooldown is refused. To a second mock player moves next to them.
  - **Names:** a portal placed on a plain test floor gets a biome name, not empty.
  - **Persistence:** the portal list round-trips through its codec.
  - **Quantum Leap:** the display name is "Quantum Leap" and the id is still `teleport`.
- **Client test:** one screenshot of the warp menu with 2 portals and 1 player, and one of a portal in the world. Look at both yourself:
  - rows have the name on top and the coordinates below;
  - X at the right;
  - purple outline with a coloured centre.
  - Client tests stay muted: keep `MuteTestClientMixin`.

## Done when

- `./gradlew :permahearts:build` is green, and the client test is green with the screenshots checked.
- `docs/workflow/playtest.md` (Vietnamese) has steps for both parts.
- `02-findings.md` has the new API names.
- Merged into `feat/P-026-cosmic-warping`, not pushed.
- Report in ≤10 lines.
