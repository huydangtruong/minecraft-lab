# P-074 Trash slot (D217)

Status: done (branch feat/P-073-sheep-dots)

## Goal
1. Turn the Notes feature (P-042, D117, D122) off by commenting out every wiring spot (files stay).
2. Put a trash slot where the Notes button was (next to the Cultivation button in the inventory screen).

## Design
- `trash/TrashRules.shouldDestroy(carriedEmpty, spectator)`: pure rule.
- `trash/TrashNet`: C2S payload `permahearts:trash` (no data). Server handler `apply(ServerPlayer)` reads `containerMenu.getCarried()`, clears it, broadcasts, sends a lava-extinguish sound to that player only. Client never says what the stack is.
- `StatsPanel.TrashSlot`: 18x18 widget, vanilla `container/slot` sprite + `permahearts:trash/trash` icon (16x16, made by `tools/make_trash_sprite.py`), hover highlight, tooltip lang key `permahearts.trash.tooltip`. Left or right click sends the payload when the cursor holds a stack.
- Notes: `NotesNet.register()`, `NotesTab.register()`, the Notes button/box/key/click/scroll/draw wiring are commented with `// D217: Notes disabled`. Old saved Notes data stays untouched in the world (never loaded, so no crash).
- Tests: `TrashRulesTest` (JUnit), `TrashGameTest` (server), `TrashClientTest` (screenshots). Notes game/client tests removed from `fabric.mod.json` lists; `NoteRulesTest` still passes (pure) and stays.

## Checks
JUnit `*TrashRulesTest`, `*NoteRulesTest`; `runGameTest -Pgt='*:trash*'`; client test limited to `TrashClientTest`; final `:build`.
