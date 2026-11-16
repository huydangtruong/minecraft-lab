# P-042 — Notes panel (shared and private)

> **Tóm tắt cho Huy (VN):** nút Ghi chú thứ 3 cạnh Cultivating; panel trái 2 tab Chung/Riêng; tin ≤ 24 ký tự; mỗi người ≤ 16 tin chung, ≤ 24 tin riêng; tin mới trên cùng; nút Xóa hiện dấu X trên tin của mình (D117).

- **Status:** ready · **Branch:** `feat/P-042-notes` · **Decision:** D117

## Rules (pure Java, JUnit) — `notes/NoteRules`
- `MAX_CHARS = 24` (trim; empty rejected; longer rejected — the text box also caps at 24).
- `MAX_SHARED_PER_PLAYER = 16`, `MAX_PRIVATE_PER_PLAYER = 24`. Over the cap: reject with a message "Delete an old note first".
- Note = (id, author UUID, author name, text, game time / sequence). List order newest first.
- Delete: only your own notes (shared: check author; private: they are all yours).

## Server
- Store per world in a `SavedData` (shared list + private lists by UUID). Payloads: client → server `add(tab, text)`, `delete(tab, id)`, `request`; server → client the shared list (to everyone when it changes) and the player's private list (to that player). Follow how existing payloads are registered (`technique/TechniqueNet.java`, `godslayer/GodSlayerNet.java`).

## Client
- Find the profile panel with the "Cultivating" button (grep `client/` for it; the stats/techniques panel opened from the inventory). Add a **third button** next to it: "Notes". Clicking opens a **left-side panel** in the same style as the existing ones, with two tabs at the top: **Shared** / **Private**, and a **Delete** button at the top.
- List: each row "Name: text", newest at the top, scroll with the mouse wheel (like the techniques list). Delete mode: an "X" on each row the player may delete; click deletes (server checks).
- Bottom: an `EditBox` (max 24 chars) + a **Send** button (Enter also sends). Show the count, e.g. "12/16".
- English UI text (lang `en_us.json`).

## Verify
`./gradlew :permahearts:build` green (JDK 25); known flaky game tests may fail once: rerun. JUnit for NoteRules. A server game test for add/limit/delete-own-only. If time allows one client screenshot test (muted game).

## Commit
Branch `feat/P-042-dev` from `feat/P-042-notes`, commit each part, ff-merge back. Do not push. English commits with the Co-Authored-By line. No `__pycache__`.
