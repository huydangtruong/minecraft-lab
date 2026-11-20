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
- **Manager (Opus) mỏng**: viết brief ngắn, giao **một** dev Sonnet làm trọn (số liệu, code, test, chạy test **một lần** theo mức ở mục "Test theo phạm vi thay đổi", merge vào nhánh tính năng); manager chỉ đọc báo cáo rồi merge vào `main`. Không chạy lại client test khi dev đã báo xanh; không tự mò javap.
- Chỉnh số nhỏ (đổi một con số): không cần plan riêng, một dòng quyết định là đủ.

## Test theo phạm vi thay đổi (Huy, 2026-11-17, checklist 2026-11-21)

Chỉ test đúng phần mà thay đổi chạm tới. Làm theo checklist này **trước khi chạy bất kỳ test nào**, và chép phần đã đánh dấu vào báo cáo.

### Bước 1: thay đổi chạm tới đâu?

Chạy `git diff --name-only main...HEAD` (hoặc `git status`). Đánh dấu `[x]` các mảng có file bị sửa trong bảng dưới.

| ☐ | Mảng | JUnit (`:test --tests`) | Game test server (`runGameTest -Pgt=`) | Client test (cả bộ, chỉ khi đổi màn hình mới) |
|---|---|---|---|---|
| ☐ | Fireball, Frozen Breath, Heaven Blast, Healing Domain, Undying Jutsu, Spirit Walk | `*TechniqueRulesTest` | `'*:technique_effects*'` | TechniqueClientTest |
| ☐ | Thunder Strike | `*ThunderRulesTest` | `'*:thunder*'` | ThunderClientTest |
| ☐ | Seism | `*SeismRulesTest` | `'*:seism*'` | SeismClientTest |
| ☐ | Solar Lance, Life Stealer | `*LanceStealerRulesTest` | `'*:lance*'` | LanceClientTest |
| ☐ | Crystal Prison | `*CrystalRulesTest` | `'*:crystal*'` | CrystalClientTest |
| ☐ | Soul Grasp | `*GraspRulesTest` | `'*:grasp*'` | GraspClientTest |
| ☐ | Cosmic Warping | `*WarpRulesTest` | `'*:warp*'` | WarpClientTest |
| ☐ | Quantum Leap | `*TechniqueRulesTest` | `'*:thousand_mile_step*'` | TeleportClientTest |
| ☐ | Cloud Travel | `*TechniqueRulesTest` | `'*:flight_blade*'` | FlightBladeClientTest |
| ☐ | Aqua Marine | `*TechniqueRulesTest` | `'*:aqua*'` | AquaClientTest |
| ☐ | Violet Guardian | `*TechniqueRulesTest` | `'*:golem*'` | GolemClientTest |
| ☐ | Warrior Angel | `*TechniqueRulesTest` | `'*:angel*'` | AngelClientTest |
| ☐ | Illusion | `*TechniqueRulesTest` | `'*:illusion*'` | IllusionClientTest |
| ☐ | Thanh chiêu, danh sách chiêu, hồi chiêu chung | `*SkillBarLogicTest`, `*TechniqueDataTest` | `'*:technique_game*'` | SkillBarClientTest, TechniqueClientTest |
| ☐ | Tim, HeartXP, linh dược | `*Heart*`, `*ElixirStatTest`, `*StatMathTest` | `'*:immortal_cultivation*'` | LabelsClientTest |
| ☐ | Nội tại (Passives) | `*PassiveRulesTest` | `'*:passive*'` | PassiveClientTest |
| ☐ | Crimson Master, nhiệm vụ, rương quà | `*QuestLogicTest`, `*RewardBoxTest`, `*StayTimeTest`, `*BloodMoonArenaTest` | `'*:wanderer*'`, `'*:reward_box*'` | QuestScreenClientTest, RewardBoxClientTest |
| ☐ | God Slayer | `*GodSlayer*` | `'*:god_slayer*'` | GodSlayerClientTest |
| ☐ | Danger level, quái tinh anh | `*DangerRulesTest` | `'*:danger*'` | DangerClientTest |
| ☐ | Deadly Treasure | `*TreasureRulesTest` | `'*:treasure*'` | TreasureClientTest |
| ☐ | Notes | `*NoteRulesTest` | `'*:notes*'` | NotesClientTest |
| ☐ | Death Match | `*DeathMatchTest` | – | – |
| ☐ | XP thu hoạch | – | `'*:harvest_xp*'` | – |
| ☐ | /testmode, /admin | `*TestModeRulesTest`, `*AdminDraftTest` | `'*:admin*'` (TestModeGameTest chưa được khai báo trong `fabric.mod.json` nên chưa bao giờ chạy) | AdminClientTest |
| ☐ | **Phần chung**: enum `Technique`, `TechniqueNet`, `TechniqueData`, lưu dữ liệu, đổi tên, `build.gradle`, bộ cài | cả bộ | cả bộ | cả bộ |

### Bước 2: thay đổi thuộc loại nào?

- [ ] **Chỉ hình ảnh** (màu, vị trí tia, vị trí HUD, icon): chỉ biên dịch `./gradlew :immortal-cultivation:compileClientJava`. **Không** chạy client test để chụp ảnh; Huy xem trong game.
- [ ] **Đổi số hoặc chữ của một mảng** (hồi chiêu, sát thương, thời gian): chỉ chạy JUnit của mảng đó, ví dụ `./gradlew :immortal-cultivation:test --tests '*ThunderRulesTest'`. Nếu số đó có trong game test của mảng, chạy thêm game test của mảng đó.
- [ ] **Logic mới hoặc sửa logic trong một mảng**: chạy JUnit và game test của các mảng đã đánh dấu, ví dụ `./gradlew :immortal-cultivation:runGameTest -Pgt='*:lance*'` (khoảng 10 giây). Có màn hình mới thì chạy client test **một lần** ở cuối; bộ này không lọc được nên chạy cả bộ.
- [ ] **Đụng phần chung** (dòng cuối của bảng): chạy đủ `./gradlew :immortal-cultivation:build` và `runClientGameTest`, thêm `scripts/test-installer.sh` nếu đụng bộ cài.

### Bước 3: chạy và báo cáo

- Chỉ chạy đúng các lệnh đã chọn ở bước 2. Muốn lọc nhiều mảng cùng lúc thì chạy `runGameTest` vài lần, mỗi lần một `-Pgt`.
- Test hỏng ở mảng không đánh dấu: ghi lại, không sửa, không chạy lại.
- Trong báo cáo, ghi các mảng đã đánh dấu, loại thay đổi và lệnh đã chạy.
- Phát hành: `scripts/release.sh` tự chạy `build` (JUnit + toàn bộ game test server), nên cả bộ vẫn được kiểm tra một lần trước mỗi bản phát hành.

## Minecraft lúc test phải tắt tiếng (Huy, 2026-11-15)

Huy có thể đang chơi hoặc đã tắt màn hình đi ngủ khi agent test. Mọi Minecraft do agent mở đều **không có tiếng**:
- Client game test: mixin `MuteTestClientMixin` trong mod test (`src/gametest`) chặn mọi âm thanh. Đừng xoá nó. Client test mới thì vẫn dùng mod test này.
- `runClient`: `build.gradle` đặt `soundCategory_master:0.0` trong `run/options.txt` trước khi mở game.

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
