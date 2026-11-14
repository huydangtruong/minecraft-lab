# Backlog

| ID | Việc | Ghi chú |
|---|---|---|
| BL-001 | Thử PermaHearts với bạn qua Essential (host + khách) | Cần P-001 xong; xem [playtest](../workflow/playtest.md) |
| ~~BL-002~~ | ~~HUD client~~ → thanh HeartXP, [P-002](../plans/P-002-heartxp.md) | D9 |
| BL-003 | CI GitHub Actions chạy `./gradlew build` (JDK 25) | Mẫu: fabric-example-mod `.github/workflows/build.yml` |
| BL-004 | Lệnh triệu hồi Crimson Master với một nhiệm vụ cụ thể (vd. `/quest summon boss ghast`, `/quest summon blood_moon creeper`, `/quest summon kill zombie 16`) | Huy 2026-11-14; để test/chơi thử nhanh từng loại nhiệm vụ |
| ~~BL-005~~ → P-014 | Chiêu **Bay** (Flight), thêm vào danh sách chiêu P-011: bay như Creative trong một thời gian. Lv 1: bay **20 giây**, tốc độ như Creative · Lv 10: 40 giây, +50% (đề xuất) · Lv 20: **60 giây**, **+100%** tốc độ bay so với Creative. Hồi chiêu **120 giây**. Đề xuất: hết giờ thì rơi chậm vài giây (không chết vì rơi ngay); dùng `abilities.mayfly/flying` + `setFlyingSpeed`, không đụng người đang ở Creative/Spectator | Huy 2026-11-14; làm sau P-011 (cần thanh chiêu của phần B). Số liệu thêm vào `TechniqueRules` + bảng `techniques-levels.md` khi làm |
| ~~BL-006~~ → P-014 | Chiêu **Huyết Kiếm** (Life Steal Blade): vật trên tay là một thanh kiếm nhuốm máu. Đang chọn chiêu này, **chuột trái đánh trúng quái** thì **hút máu** bằng: sát thương cơ bản + sát thương của chiêu + sát thương cộng thêm vĩnh viễn từ linh dược. Sát thương của chiêu: Lv 1 bằng **kiếm sắt**, mỗi level **+0,5**. Cần chốt khi làm: có hồi chiêu không (đề xuất: không, là chiêu "cầm trên tay" như vũ khí), tốc độ đánh như kiếm, có hút máu từ người chơi khác không | Huy 2026-11-14; làm sau P-012 |
