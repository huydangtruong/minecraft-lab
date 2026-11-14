# Status — where work stands

> Tóm tắt cho Huy: file này ghi việc đang làm dở và bước tiếp theo, để một phiên Claude mới đọc là làm tiếp được.
> Manager cập nhật file này cuối mỗi đợt việc (merge, phát hành, quyết định mới).
> Snapshot: 2026-11-14.

A fresh agent reads, in order: this file → [AGENTS.md](../../AGENTS.md) → [workflow/team.md](../workflow/team.md) → the plan of the work in flight. Do not read the whole repo.

## Releases

| What | Where |
|---|---|
| Last release | `permahearts-v0.10.1` (GitHub Releases on `huydangtruong/minecraft-lab`), `mod_version=0.10.1` |
| On `main`, not released | P-019 Thousand Mile Step (D58, D59); `/technique cast` reports a refused cast |
| On `feat/P-020-blade-golems`, not on `main` | P-020 brief, the "Tiết kiệm credit" section in team.md, this file |

Release steps: [workflow/release.md](../workflow/release.md). Release **only when Huy says so**. Next version: `0.11.0` (new technique), unless Huy picks another.

## In flight: P-020 — Life Steal Blade upgrade + Violet Guardian

Plan: [plans/P-020-blade-golems.md](../plans/P-020-blade-golems.md) (D60).

- A Sonnet dev was started on 2026-11-14 in worktree `.claude/worktrees/agent-a4190417818542910`, branch `feat/P-020-dev`. It merges into `feat/P-020-blade-golems` when the full build is green.
- To find where it stopped:
  ```sh
  git log --oneline feat/P-020-blade-golems -5
  git log --oneline feat/P-020-blade-golems..feat/P-020-dev
  ```
- **If `feat/P-020-blade-golems` already has the merge:** merge it into `main` (`git merge --no-ff`), run one quick `./gradlew :permahearts:build` only if there were conflicts, push `main`, mark P-020 `merged` in [plans/README.md](../plans/README.md), then ask Huy whether to release.
- **If the dev stopped half-way** (commits on `feat/P-020-dev` but no merge): start one new Sonnet dev in that worktree with the plan and "continue from the existing commits on feat/P-020-dev; run the full build once; merge into feat/P-020-blade-golems".
- **If `feat/P-020-dev` has no new commits:** start a new Sonnet dev with the plan from scratch.
- Tell Huy these were manager proposals, not his numbers, so he can change them: golem lasts **60 s**, cooldown **90 s**.

## Open, not scheduled

- **BL-004**: a command to summon the Wanderer with a chosen quest ([backlog/BACKLOG.md](../backlog/BACKLOG.md)).
- **P-010 Death Match**: old branch `feat/P-010-death-match`, never merged, far behind `main`. Ask Huy before reviving it.
- **Huy's game profile** "26.3 Fabric Essential" has an older build. Run `scripts/install.sh` only when Huy asks in that message.
- **Housekeeping**: about 14 old agent worktrees under `.claude/worktrees/` from merged plans. Clean them up only with Huy's OK. Never touch the P-020 one while it is running.

## How the team works (short)

Full text: [workflow/team.md](../workflow/team.md).

- **Manager (Opus):**
  - Writes a plan, or a decision line for small tweaks.
  - Starts **one** Sonnet dev per batch.
  - Reads the dev's ≤10-line report.
  - Merges into `main` and pushes.
  - Does not re-run client tests the dev already ran green.
- **Dev (Sonnet):**
  - Works in a worktree on `feat/P-NNN-dev` and writes the code and tests.
  - Runs the full `./gradlew :permahearts:build` once (client screenshot tests only if UI changed).
  - Merges into `feat/P-NNN-<slug>`.
- **Git and GitHub:**
  - Commits are made as `huydangtruong` (local git config of this repo).
  - Push and release with `GH_TOKEN="$(gh auth token -u huydangtruong)"`.
  - Never change the global git or gh config, and never push `main` from a dev.
- **Tokens:** auto-compact is set to 240k in `.claude/settings.local.json`. Suggest that Huy opens a fresh session after each release.

## Keeping this file current

At the end of every batch, the manager edits only the sections that changed:
- **Releases:** what is on `main` but not released.
- **In flight:** each plan with its branch, worktree, and how to resume it.
- **Open:** anything Huy deferred.

Add new decisions to [01-decisions.md](01-decisions.md), not here.
