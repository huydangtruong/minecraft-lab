# P-044 — Clone farming fixes, installer shows the changelog and exits

> **Tóm tắt cho Huy (VN):** phân thân hết đứng đợi, gặt 2 mức cuối (cho chín rồi gặt), tầm cao ±6 (D123); script cài xong hiện changelog phiên bản rồi tự thoát (D124).

- **Status:** ready · **Branch:** `feat/P-044-farm-installer` · **Decisions:** D123, D124

## 1. Clone stands idle (bug)
Huy: with a hoe in slot 1 (level 10–14, radius 16), clones harvest a little, then **stand still until near the end of their life**, then go to the chest. A second field less than 10 blocks away is ignored. Code: `technique/illusion/WorkGoal.java` (idle → delivery after `IDLE_BEFORE_DELIVERY` ticks), `FarmTask.java` (search: `WorkTask.cells(g, sl, 3, 3)`, ripe = max age). Reproduce in a server game test first (two farmland fields ~10 blocks apart inside the radius, mixed ripe/unripe, a chest; one clone) and find why it stands still — likely a target it cannot reach that keeps `task.tick` returning true, or a path that fails without skipping. Fix: a target not reached within a few seconds is skipped; no work → deliver at once (already intended), then keep searching. Raise the farm search height from ±3 to **±6** around the cast point.

## 2. Harvest the last two stages (D123)
`FarmTask.ripe`: a `CropBlock` at `maxAge` or `maxAge − 1` counts. At `maxAge − 1` the clone sets the crop to `maxAge` (`getStateForAge`) right before breaking it, so the drops are the ripe drops. Replant as today. JUnit or game test for wheat at 6 and beetroot at 2.

## 3. Installer: changelog then exit (D124)
`minecraft-lab.command` (macOS) and `minecraft-lab.ps1` (Windows), and `scripts/test-installer.sh`:
- After a **successful** install of a version, print a "What's new in <title> <version>:" header and the GitHub release `body` of that release (it is in the releases JSON the script already downloads; with `MCLAB_RELEASES` tests it comes from the local file). Empty body → "(no notes)".
- Then exit with code 0 (no "Back to the version list / Exit" menu). On failure keep today's behaviour.
- Windows: the console window closes on exit when started by double-click, so print "Press Enter to close" and wait for one Enter, then exit. macOS: exit at once (Terminal keeps the text).
- Keep JSON parsing as the scripts do today (no new tools). Update `scripts/test-installer.sh` to check the changelog line and the exit.

## Verify
`./gradlew :permahearts:build` green (JDK 25; known flaky game tests may fail once: rerun). `scripts/test-installer.sh` passes. If `pwsh` is not installed, don't install it: read the ps1 change carefully and say it was not run.

## Commit
Branch `feat/P-044-dev` from `feat/P-044-farm-installer`, commit each part, ff-merge back. Do not push. English commits with the Co-Authored-By line. No `__pycache__`.
