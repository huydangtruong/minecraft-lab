# Team session (kiểu AllSeeing, rút gọn)

| Session | Model | Làm gì | Không làm |
|---|---|---|---|
| **manager** | Opus | Nhận yêu cầu của Huy, chốt quyết định (`01-decisions.md`), viết plan `P-NNN` có sẵn file/tên hàm đã kiểm, review diff + kết quả test, merge | Tự code tính năng lớn |
| **dev** | Sonnet | Làm **một plan** trong worktree riêng, nhánh `feat/P-NNN-<slug>`, báo kết quả theo mẫu | Làm ngoài plan, push `main`, đổi phiên bản ghim, tải thứ gì ngoài repo |

## Luồng

1. Huy nêu ý → manager hỏi chỗ chưa rõ, ghi quyết định, viết `docs/mods/<id>/spec.md` và plan theo [TEMPLATE](../plans/TEMPLATE.md) (status `ready`).
2. Huy ra lệnh → giao plan cho dev (gửi đường dẫn, không chép nội dung).
3. Dev: đọc plan + AGENTS.md, chỉ mở file plan nêu, chạy đúng lệnh kiểm trong plan, commit, push nhánh, báo.
4. Manager review, merge vào `main`, cập nhật `docs/mods/README.md`. Huy chạy `scripts/install.sh` và chơi thử ([playtest.md](playtest.md)).

## Tiết kiệm credit (Huy, 2026-11-14)

- **Một phiên chat cho một đợt việc**: phát hành xong thì mở phiên mới. Phiên càng dài, mỗi bước càng tốn (mô hình đọc lại toàn bộ lịch sử ở mỗi lần gọi công cụ).
- **Gom yêu cầu**: nhiều chỉnh sửa nhỏ gửi chung một lần → một brief, một dev, một lần test.
- **Manager (Opus) mỏng**: viết brief ngắn, giao **một** dev Sonnet làm trọn (số liệu, code, test, chạy toàn bộ test **một lần**, merge vào nhánh tính năng); manager chỉ đọc báo cáo rồi merge vào `main`. Không chạy lại client test khi dev đã báo xanh; không tự mò javap.
- Chỉnh số nhỏ (đổi một con số): không cần plan riêng, một dòng quyết định là đủ.

## Tiết kiệm token (cho dev)

- Đọc plan trước; không dò code ngoài plan. Tra API bằng `javap`/`gh api` như AGENTS.md, ghi cái mới vào `02-findings.md`.
- Sửa không được sau **2 lần thử**: dừng, gửi manager lệnh + vài dòng lỗi, không mò.
- Plan sai/thiếu: hỏi manager, không tự mở rộng phạm vi.
- Báo cáo ≤ 10 dòng.

## Mẫu báo cáo

```
P-NNN <slug> — done | blocked
Branch: feat/P-NNN-<slug> @ <commit> (pushed)
Changed: <file, 1 line each>
Tests: ./gradlew :<mod>:build ✔ (JUnit N, gametest M)
Playtest: <steps done in runClient | not done>
Off-plan: <none | what and why>
Questions: <none | …>
```

## Câu mở session dev (Sonnet, có worktree)

```
You are dev-N, a developer session (Sonnet) of the Minecraft Lab team. Read AGENTS.md and docs/workflow/team.md once, reply with one short Vietnamese line that you are ready, then wait for a plan (docs/plans/P-NNN-*.md). When you get one: git checkout -b feat/P-NNN-<slug>, follow the plan, push the branch (never main), report in the team.md format.
```

## Bàn giao giữa các phiên

Cuối mỗi đợt việc, manager cập nhật [system/03-status.md](../system/03-status.md) (đã phát hành gì, việc đang dở và cách làm tiếp, việc chờ Huy). Phiên mới đọc file đó trước tiên.
