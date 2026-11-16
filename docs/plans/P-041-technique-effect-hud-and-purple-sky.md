# P-041 — Technique effect icons with time left, purple Death Match sky

> **Tóm tắt cho Huy (VN):** Undying Jutsu, Aqua Marine, Cloud Travel… hiện ô hiệu ứng góc phải trên kèm thời gian còn lại (D115); trời Death Match màu tím, sáng hơn Trăng máu một chút (D116).

- **Status:** ready · **Branch:** `feat/P-041-hud-sky` · **Decisions:** D115, D116

## 1. Effect icons (D115)
- Today some timed techniques show in the vanilla top-right effect area and some do not; Undying Jutsu shows no time. Find how each timed technique stores its duration (`technique/TechniqueCasts.java`, `TechniqueEffects`, attachments) and how the current ones that do show are done.
- Goal: every technique with a duration (at least Undying Jutsu, Aqua Marine, Cloud Travel; also Shadow Walk/others if they are timed) shows an icon in the top-right effect area **with the time left**, like a potion. Simplest route: a custom `MobEffect` per technique (registered, with icon texture `textures/mob_effect/<id>.png` — reuse the technique's icon) applied on cast with the real duration and removed when the technique ends; keep it beneficial, no particles, no gameplay attributes (the technique logic stays where it is). Or draw the time on the client if a vanilla effect already exists. Check the API names with javap (26.3, unobfuscated).
- Milk must not break the technique: if milk clears the icon only, fine; the technique logic must not depend on the effect.

## 2. Purple Death Match sky (D116)
- Find how the Blood Moon tints the sky/fog on the client (grep `client/` for BloodMoon). Death Match does the same while a Death Match is active (sync the flag the same way), but **purple** and **a bit brighter** than Blood Moon (still dark). Blood Moon unchanged.

## Verify
`./gradlew :permahearts:build` green (JDK 25); known flaky game tests (thunder pig, golem guardian) may fail once: rerun. If a client screenshot test exists for Blood Moon, add one for the purple sky and keep the game muted (`options.txt` sound volumes 0).

## Commit
Branch `feat/P-041-dev` from `feat/P-041-hud-sky`, commit each part, ff-merge back. Do not push. English commits with the Co-Authored-By line. No `__pycache__`.
