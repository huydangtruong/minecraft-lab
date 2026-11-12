# P-007 — Client game test: screenshots of the quest UI

> **Tóm tắt cho Huy:** thêm một bài test mở Minecraft thật trong world tạm và tự chụp màn hình quest (nhận nhiệm vụ, các dòng, dòng đã xong, tooltip phần thưởng; cả tiếng Anh và tiếng Việt). Chạy riêng, không chạy khi `build`.

- **Status:** merged-pending
- **Mod / decision:** `permahearts` · **Branch:** `feat/P-007-client-screenshots` · **Depends on:** P-006

## Goal

Let a dev (or Huy) see the Wanderer screen without playing: a Fabric client game test opens a throwaway creative world, spawns the Wanderer, right-clicks it with the real use key, and takes screenshots. It asserts nothing; it fails only on exceptions.

## Files

| File | Change |
|---|---|
| `build.gradle` | `enableClientGameTests = true` (adds task `runClientGameTest`; `build` does not depend on it) |
| `mods/permahearts/src/gametest/resources/fabric.mod.json` | `fabric-client-gametest` entrypoint |
| `mods/permahearts/src/gametest/java/io/github/huydangtruong/permahearts/QuestScreenClientTest.java` | the test |

## How to run

```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
./gradlew :permahearts:runClientGameTest
```

A Minecraft window opens (~30 s). PNGs land in `mods/permahearts/build/run/clientGameTest/screenshots/` (gitignored via `build/`): `00-world-view`, `01-quest-offer`, `02-quest-lines`, `03-line-completed`, `04-reward-tooltip`. English only (D16).

## Done when

- [x] `runClientGameTest` green, PNGs show the real screens
- [x] `./gradlew :permahearts:build` stays green and opens no window
