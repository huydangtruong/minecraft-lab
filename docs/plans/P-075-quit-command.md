# P-075 /quit command (D218)

Status: done (branch feat/P-073-sheep-dots)

## Goal
Huy hosts a world through Essential (integrated server) and leaves the game running. When the friend is done, anyone types `/quit`: 10 s countdown, world saved, everybody disconnected, the host's Minecraft closes completely. Vanilla `/stop` does not exist on an integrated server.

## Design
- `quit/QuitCountdown` (pure Java): `start`, `cancel`, `toggle`, `tick()` returning `IDLE/RUNNING/SECOND/FIRE`, `ticksLeft`, `secondsLeft`. 200 ticks; fires on tick 200 exactly once.
- `quit/Quit` (glue): `/quit` (permission LEVEL_ALL) toggles the countdown; `END_SERVER_TICK` ticks it, chat line at start/cancel (names who), action bar every second. Dedicated server: failure message, nothing starts. Hooks `Quit.hosted` and `Quit.finalStep` are replaceable so game tests never stop the server.
- Final step `Quit.saveAndClose`: `server.saveEverything(false, true, true)`, kick everybody except the host (`isSingleplayerOwner(nameAndId)`) with "The host's world was saved and closed", then send `QuitNet.CloseGame` (S2C, no data) to the host. No host online: `server.halt(false)`.
- Client `QuitClient`: on the payload, `Minecraft.disconnectFromWorld(...)` (the pause screen's "Save and Quit to Title" call; shows "Saving world", halts the integrated server and waits for its thread), then `Minecraft.stop()`.

## Tests
- Checklist: areas ticked = shared plumbing is untouched; only the new /quit area plus one line each in `ImmortalCultivation`, `ImmortalCultivationClient`, test `fabric.mod.json`. Change type = new logic in a new area.
- `./gradlew :immortal-cultivation:test --tests '*QuitCountdownTest'`, `runGameTest -Pgt='*:quit*'`, then one `:build` at the end.
- No client test: it would really close the test client.
