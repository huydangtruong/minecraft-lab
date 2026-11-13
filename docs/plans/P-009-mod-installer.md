# P-009 — Mod installer from GitHub Releases + rename to Immortal Cultivation

> **Tóm tắt cho Huy (VN, ≤ 3 dòng):** đổi tên hiển thị PermaHearts → **Immortal Cultivation** (giữ id để không mất save). Mỗi bản mod thành một GitHub Release. File `install-mods` ở gốc repo (Mac + Windows): chọn mod → chọn 1 trong 6 bản gần nhất (có chữ Installed) → script tự tải, kiểm tra, tìm đúng thư mục mods và cài.

- **Status:** ready
- **Mod / decision:** `permahearts`, D22, D23, D24 · **Branch:** `feat/P-009-mod-installer` · **Depends on:** none

## Goal

1. The mod shows as **Immortal Cultivation** everywhere a player sees a mod name (Mod Menu / mod list, jar file name, release title), version **0.8.0**. Internal id stays `permahearts` (D23).
2. `scripts/release.sh` publishes a mod version as a GitHub Release on `huydangtruong/minecraft-lab` (public).
3. Root installers for players: `install-mods.command` (macOS), `install-mods.ps1` + `install-mods.bat` (Windows). Interactive, numbered menus, Enter to confirm.

## Scope

- In: rename (display name, description, jar name), `release.json` asset, `scripts/release.sh`, the three root installer files, fixture test for the macOS installer, `LICENSE` (MIT, the mod already declares MIT), docs.
- Out (do not do): changing `mod_id`, Java package, `mods/permahearts/` folder, namespace `permahearts:` (attachments, gamerule, items, textures, lang keys), the `/hearts` command. Creating a real release or pushing a tag (manager does it after review). Code signing, exe/dmg. Bumping pinned versions. Deleting `scripts/package.sh` (keep as fallback).

## Rename (D23)

| File | Change |
|---|---|
| `mods/permahearts/gradle.properties` | `mod_name=Immortal Cultivation`, `mod_version=0.8.0`, new `mod_file=immortal-cultivation`, `mod_description` starts with the cultivation idea (hearts, HeartXP, elixirs, Crimson Master quests; one line, English) |
| `build.gradle` | `base.archivesName = project.findProperty('mod_file') ?: project.mod_id` → jar `immortal-cultivation-0.8.0.jar` |
| `scripts/common.sh` | `mod_jar` uses `mod_file` when set. New `remove_mod_jars <dir> <mod_id>`: deletes only jars whose `fabric.mod.json` `"id"` equals `<mod_id>` (read with `unzip -p`), so old `permahearts-*.jar` is removed too and other mods never are |
| `scripts/install.sh`, `scripts/package.sh` | use `remove_mod_jars` instead of `rm "$MODS_DIR/${id}-"*.jar`; package note mentions the new jar name |
| player-visible text | grep `lang/en_us.json`, `Messages.java`, client screens for "PermaHearts"; none expected. If found, change to "Immortal Cultivation" |

Old saves keep working because nothing under `permahearts:` changes. State this in the 0.8.0 release notes.

## Release format (D24)

- Tag `<mod_id>-v<version>`, e.g. `permahearts-v0.8.0`. Release title `<mod_name> <version>`, e.g. `Immortal Cultivation 0.8.0`. Pre-release flag for minor/test builds.
- Assets: the mod jar + `release.json`:
  ```json
  {"mod_id":"permahearts","mod_name":"Immortal Cultivation","version":"0.8.0",
   "minecraft":"26.3","fabric_loader":"0.20.1","fabric_api":"0.166.2+26.3",
   "jar":"immortal-cultivation-0.8.0.jar","sha256":"<hex>"}
  ```
  Values come from `gradle.properties` (root + mod). Fabric API is NOT attached; installers fetch it from maven.fabricmc.net and check its `.sha1`.

### `scripts/release.sh <mod-id> [--pre] [--notes "<text>"]`

1. Refuse unless: on `main`, working tree clean, `HEAD` equals `origin/main`, tag does not exist (`gh release view` / `git ls-remote --tags`).
2. `./gradlew :<id>:build` (tests must pass).
3. Write `build/release/<id>/release.json` (sha256 via `shasum -a 256`).
4. `gh release create <tag> --repo huydangtruong/minecraft-lab --target "$(git rev-parse HEAD)" --title "<name> <ver>" --notes "<notes or default>" [--prerelease] <jar> release.json`, run with `GH_TOKEN="$(gh auth token -u huydangtruong)"` so the release is made by Huy's personal account (gh's active account is the work one `huytruongrt`; never run `gh auth switch`).
5. Print the release URL.

Dev: write and dry-run it with `--dry-run` (prints the gh command, creates nothing). Do not run it for real.

## Installer behaviour (both OS, same flow)

Constants at the top: `REPO=huydangtruong/minecraft-lab`. Overrides for tests: `MCLAB_RELEASES` (path to a local releases JSON instead of the API), `MC_DIR` (Minecraft root), `MCLAB_TTY` (macOS: file to read answers from instead of `/dev/tty`).

1. **Fetch** `https://api.github.com/repos/$REPO/releases?per_page=100` (no token; 60 req/h is plenty). Network error → clear message, exit 1.
2. **Find Minecraft profiles**: Minecraft root = macOS `~/Library/Application Support/minecraft`, Windows `%APPDATA%\.minecraft`. Read `launcher_profiles.json`; keep profiles whose `lastVersionId` matches `fabric-loader-*-<minecraft>` (minecraft from the chosen release's `release.json`; for the first menu use `26.3`). Profile folder = `gameDir` if set, else the root; mods folder = `<folder>/mods` (create if missing). Several → numbered menu with profile names. None → say Fabric Loader for Minecraft 26.3 is not installed, print `https://fabricmc.net/use/installer/` (or the Essential installer), and offer to type a mods folder path by hand (empty = exit).
3. **Installed mods**: for every `*.jar` in that mods folder read `fabric.mod.json` → `id`, `version`.
4. **Menu 1 — mods**: one line per distinct mod id found in tags (newest first), title from the release name minus the trailing version, plus the installed version if any:
   ```
   Minecraft Lab mods  (profile: 26.3 Fabric Essential)
     1) Immortal Cultivation   installed 0.7.1
     0) Exit
   Choose a mod and press Enter:
   ```
   (Installed `permahearts` jars count as this mod even if they were named `permahearts-0.7.1.jar`.)
5. **Menu 2 — versions**: the 6 newest releases of that mod by publish date, labels `(pre-release)` and `(Installed)`; `0) Back`.
   ```
   Immortal Cultivation
     1) 0.8.0
     2) 0.7.2  (pre-release)
     3) 0.7.1  (Installed)
     0) Back
   ```
   Invalid input → ask again. Choosing the installed version reinstalls it.
6. **Install**:
   - Warn if Minecraft is running (macOS `pgrep -f net.fabricmc.loader.impl.launch.knot.KnotClient`; Windows a `java`/`javaw` process whose command line contains `knot.KnotClient`) and ask to close it first (Enter to continue after closing).
   - Download `release.json` and the jar to a temp folder; verify sha256; mismatch → delete, error, nothing changed in mods.
   - Fabric API: if no jar in mods has id `fabric-api`, download `fabric-api-<ver>.jar` from `https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/<ver>/` and check `.sha1`.
   - Remove jars whose `fabric.mod.json` id equals the mod id, then copy the new jar. Never touch any other jar, `saves`, `config`.
   - Print `Installed Immortal Cultivation 0.8.0 into <mods folder>. Start Minecraft with the "<profile>" profile.` then return to Menu 1.
7. Text in English (players include the friend). No colors required; keep it readable in a plain console.

### macOS — `install-mods.command`

- `#!/bin/bash`, must run on the stock **bash 3.2** (no `mapfile`, no associative arrays, no `${x,,}`) and stock tools only: `curl`, `unzip`, `shasum`, `plutil` (`plutil -extract <keypath> raw -o - -` parses JSON, macOS 12+). No `jq`, no `python3` (it may trigger the Xcode tools prompt).
- All prompts go through one `ask` function reading from `${MCLAB_TTY:-/dev/tty}`, so `bash <(curl -fsSL …)` works.
- Executable bit committed (`git update-index --chmod=+x`).

### Windows — `install-mods.ps1` + `install-mods.bat`

- Must run on **Windows PowerShell 5.1** (built into Windows 10/11): `Invoke-RestMethod`, `ConvertFrom-Json`, `Get-FileHash`, `System.IO.Compression.ZipFile` (`Add-Type -AssemblyName System.IO.Compression.FileSystem`) to read `fabric.mod.json` from jars. Force TLS 1.2: `[Net.ServicePointManager]::SecurityProtocol = 'Tls12'`. Avoid PowerShell 7-only syntax (`??`, `?:`, `-Parallel`, `&&`).
- `install-mods.bat`: self-contained, so the friend only keeps this one file. It runs `powershell -NoProfile -ExecutionPolicy Bypass -Command "irm https://raw.githubusercontent.com/huydangtruong/minecraft-lab/main/install-mods.ps1 | iex"` and then `pause`.
- `.gitattributes`: `*.bat` and `*.ps1` use `eol=crlf`; `*.command` uses `eol=lf`.

## Files

| File | Where | Change |
|---|---|---|
| `install-mods.command` | root | new |
| `install-mods.ps1`, `install-mods.bat` | root | new |
| `LICENSE` | root | new, MIT, "Copyright (c) 2026 Huy" |
| `scripts/release.sh` | scripts | new |
| `scripts/test-installer.sh` | scripts | new, fixture test (below) |
| `scripts/common.sh`, `install.sh`, `package.sh` | scripts | `mod_file`, `remove_mod_jars` |
| `build.gradle`, `mods/permahearts/gradle.properties` | | rename |
| `.gitattributes` | root | line endings |
| `README.md` (root) | | rewrite in Vietnamese: what the mods are, **"Cài mod cho người chơi"** section with the Mac and Windows steps, link to Releases, dev commands |
| `docs/workflow/release.md` | | how Huy releases: merge → `scripts/release.sh permahearts [--pre]` |
| `docs/mods/README.md`, `docs/mods/permahearts/spec.md` | | name Immortal Cultivation (id `permahearts`) |
| `docs/system/02-findings.md` | | plutil JSON parsing, PS 5.1 notes, anything learned |

## Tests

- Existing: `./gradlew :permahearts:build` stays green (34 game tests + JUnit). Built jar is `mods/permahearts/build/libs/immortal-cultivation-0.8.0.jar` and its `fabric.mod.json` has `"id": "permahearts"`, `"name": "Immortal Cultivation"`.
- `scripts/test-installer.sh` (runs on this Mac, no network, no real Minecraft folder): builds a fixture in a temp dir —
  - fake `MC_DIR` with `launcher_profiles.json` (one Fabric 26.3 profile with `gameDir`, one vanilla profile), its `mods/` containing a fake `permahearts-0.7.1.jar` (zip with `fabric.mod.json` id `permahearts`), a fake `Essential-x.jar` (other id) and a fake `fabric-api-0.166.2+26.3.jar` (id `fabric-api`);
  - fake releases JSON (`MCLAB_RELEASES`) with 7 versions of `permahearts` incl. one pre-release; asset URLs are `file://` paths to fake jars + `release.json` (curl reads `file://`);
  - answers file (`MCLAB_TTY`): `1`, `1`, (Enter), `0`.
  Assert: menu shows `installed 0.7.1`, versions list shows exactly 6 lines with `(pre-release)` and `(Installed)` on the right ones; after install the mods folder has the new jar, no `permahearts-0.7.1.jar`, Essential and Fabric API untouched. Second case: a release.json with a wrong sha256 → exit non-zero and mods folder unchanged. Third case: no Fabric profile → the Fabric message.
- PowerShell: if `pwsh` exists, run `install-mods.ps1` against the same fixture (`$env:MCLAB_RELEASES`, `$env:MC_DIR`, answers piped). If not (current state), say so in the report; the manager arranges a check.

## Verify

```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
./gradlew :permahearts:build
unzip -p mods/permahearts/build/libs/immortal-cultivation-0.8.0.jar fabric.mod.json
scripts/test-installer.sh
scripts/release.sh permahearts --dry-run
bash -n install-mods.command
```

## Docs

Decisions D22–D24 are already recorded by the manager. Update the files in the table. Root README must not mention local absolute paths of Huy's machine.

## Commit

Small commits on `feat/P-009-mod-installer`: rename · release script · macOS installer + test · Windows installer · docs. Push the branch, never main. Report ≤ 10 lines.

## Done when

- [ ] Jar is `immortal-cultivation-0.8.0.jar`, id `permahearts`, name Immortal Cultivation; build green.
- [ ] `install.sh` replaces an old `permahearts-*.jar` and leaves other jars alone.
- [ ] `scripts/test-installer.sh` passes all three cases.
- [ ] `release.sh --dry-run` prints a correct `gh release create`.
- [ ] Windows files written for PS 5.1; tested with pwsh or flagged as untested.
- [ ] README has player install steps for Mac and Windows.

## Risks / ask first

- macOS marks a downloaded `.command` as quarantined (Gatekeeper warning on double-click). README offers the Terminal one-liner `bash <(curl -fsSL https://raw.githubusercontent.com/huydangtruong/minecraft-lab/main/install-mods.command)` as the main path, and "right-click → Open" for the file.
- Windows SmartScreen may warn on the downloaded `.bat` ("More info → Run anyway"); say so in README.
- Do not install `pwsh`, `jq` or anything else (downloads need Huy's go-ahead).
- If `plutil` cannot read GitHub's releases JSON (size, unicode in notes), fall back to `osascript -l JavaScript` (JXA `JSON.parse`), also stock. Record which one was used in 02-findings.
