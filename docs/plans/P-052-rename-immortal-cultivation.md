# P-052 — Rename permahearts → immortal-cultivation (keep save data)

> **Tóm tắt cho Huy (VN):** đổi tên `permahearts` thành `immortal-cultivation` ở mọi chỗ bên ngoài: thư mục mod, project Gradle, package Java, id mod, tag phát hành, script, tài liệu. Chỉ giữ chữ `permahearts` ở tên dữ liệu bên trong (người chơi không thấy) để world cũ còn nguyên tim, linh dược, chiêu, nhiệm vụ, ghi chú, cổng Warp, phím đã đổi (D132).

- **Status:** ready · **Branch:** `feat/P-052-rename-immortal-cultivation` (from `main`) · **Decisions:** D132

## Two names from now on
- **Mod id** (Fabric `fabric.mod.json` id, Gradle `mod_id`, release tag prefix, installer, scripts): **`immortal-cultivation`**.
- **Data namespace** (stays **`permahearts`**, never changes): every `Identifier` namespace (items, effects, attachments, payloads, saved data, game rules `permahearts:immortal_cultivation`, sounds, sprites), the resource folders `assets/permahearts/` and `data/permahearts/`, lang keys `permahearts.*` / `item.permahearts.*` / `effect.permahearts.*`, key-binding names and category (`key.permahearts.*`, stored in options.txt), NBT/codec field names. Changing any of these loses player data: do not.

## Steps
1. `git mv mods/permahearts mods/immortal-cultivation`. `gradle.properties`: `mod_id=immortal-cultivation` (Fabric allows `-`; keep `mod_file=immortal-cultivation`). Check `build.gradle` (loom `mods { "${mod_id}" }`, gametest `modId "${mod_id}-test"`, resource expansion) still works with a hyphen.
2. Java: package `io.github.huydangtruong.permahearts` → `io.github.huydangtruong.immortalcultivation` in all source sets (main, client, gametest, test). Classes `PermaHearts` → `ImmortalCultivation`, `PermaHeartsClient` → `ImmortalCultivationClient`, `PermaHeartsGameTest` → `ImmortalCultivationGameTest` (and any other `PermaHearts*` class). Update `fabric.mod.json` entrypoints, gametest entrypoints, mixin configs: `permahearts.mixins.json` → `immortal-cultivation.mixins.json`, `permahearts.client.mixins.json` → `immortal-cultivation.client.mixins.json` (their `package` field too), any `accessWidener` name.
3. Constants: today `PermaHearts.MOD_ID = "permahearts"` is used ~50 times, mostly as the Identifier namespace. Introduce `ImmortalCultivation.NAMESPACE = "permahearts"` and use it for every Identifier / resource / data use (including the literal `"permahearts"` strings like `GodSlayerSavedData`, `WarpSavedData`, `NotesData`, `Illusion.SPRINT_ID`: leaving those literals is fine). Keep `MOD_ID = "immortal-cultivation"` only for `FabricLoader.getModContainer(...)` lookups (version shown in the game-rule group "Immortal Cultivation <version>", etc.). Grep every `MOD_ID` use and decide which it is. Add a comment on NAMESPACE: never change it (saves).
4. Scripts:
   - `scripts/common.sh`, `install.sh`, `package.sh`, `release.sh`, `test-installer.sh`: work with the new folder and id; release tag becomes `immortal-cultivation-v<ver>`.
   - **Old jars:** installed jars of 0.28.0 and older have fabric id `permahearts`. Every place that removes / detects an installed mod by jar id must treat `permahearts` as `immortal-cultivation` (alias), or the old jar stays next to the new one and the game crashes on duplicate content. That is `scripts/common.sh remove_mod_jars` (install.sh), `minecraft-lab.command` (`scan_installed`, `installed_version`, `remove_mod_jars`) and `minecraft-lab.ps1` (`Update-Installed`, `Get-InstalledVersion`, `Remove-ModJars`). One small alias table (`permahearts → immortal-cultivation`) per script.
   - **Old tags:** the installers group releases by the tag prefix (`<mod>-v<ver>`). Map the prefix through the same alias so `permahearts-v0.27.0` and `immortal-cultivation-v0.29.0` show as one mod with one version list (the manager will also rename the old tags on GitHub, so both must work).
   - `scripts/test-installer.sh`: keep the old-jar case (`permahearts-0.7.1.jar`, id `permahearts`) and add a case: an installed id-`permahearts` jar is detected as Installed and removed when a new `immortal-cultivation` jar is installed; releases mixing both tag prefixes list as one mod. It must pass (`bash scripts/test-installer.sh`). The .ps1 cannot be run here (no pwsh): mirror the bash logic carefully.
5. Docs (live docs only; old plans `docs/plans/P-0xx` stay as history): `AGENTS.md`, `README.md`, `docs/README.md`, `docs/system/*.md` (paths, `./gradlew :immortal-cultivation:...`, javap notes, ids), `docs/workflow/*.md`, `docs/mods/README.md`, `git mv docs/mods/permahearts docs/mods/immortal-cultivation` and fix links, `docs/plans/README.md`/`TEMPLATE.md` if they name the folder. Grep the repo for `permahearts` at the end: every hit left must be the data namespace, history, or the alias.
6. Nothing in game changes for the player: same items, same lang text, same keys.

## Tests
- `./gradlew :immortal-cultivation:build` and `./gradlew :immortal-cultivation:runClientGameTest` green (JDK 25; known flaky game tests may fail once: rerun).
- A save-compat check: a game test (or JUnit on the constants) asserting key Identifiers still use namespace `permahearts` (e.g. the hearts attachment id, the game rule id, a focus item id).
- `bash scripts/test-installer.sh` green. `scripts/release.sh immortal-cultivation --dry-run` prints tag `immortal-cultivation-v0.28.0` (do not run without `--dry-run`; it will complain the tag/branch state, that's fine, just show the tag line).

## Commit
Branch `feat/P-052-dev` from `feat/P-052-rename-immortal-cultivation`, commit (several commits fine), report. Do not push, do not touch `main`, do not run install.sh / package.sh / release.sh without `--dry-run`. English commits ending with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
