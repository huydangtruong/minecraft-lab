# Quyết định

| # | Chủ đề | Quyết định | Ngày |
|---|---|---|---|
| D1 | Sàn tim (PermaHearts) | Tối thiểu **1 tim**. Chết khi đang 1 tim: không mất thêm, không bị loại, hồi sinh bình thường | 2026-11-11 |
| D2 | Trần tim | ~~Tối đa 20 tim~~ → thay bởi D6. Cũ: tối đa **20 tim**. Đạt 100 kill khi đã 20 tim: tiến độ về 0, báo "đã tối đa" | 2026-11-11 |
| D3 | Repo | GitHub **private** `huydangtruong/minecraft-lab` (tài khoản cá nhân, không phải huytruongrt) | 2026-11-11 |
| D4 | Cấu trúc | Một Gradle build chung, mỗi mod một thư mục `mods/<id>/`; docs, plan `P-NNN`, team Opus (manager) + Sonnet (dev) theo kiểu AllSeeing | 2026-11-11 |
| D5 | Mod đầu tiên | PermaHearts: chết −1 tim vĩnh viễn; giết 100 mob hostile +1 tim vĩnh viễn ("Đơn giản là thế") | 2026-11-11 |
| D6 | Trần tim (thay D2) | ~~Tối đa 30 tim~~ → thay bởi D7. Cũ: tối đa **30 tim**. Đạt 100 kill khi đã 30 tim: tiến độ về 0, báo "đã tối đa" | 2026-11-11 |
| D7 | Trần tim (thay D6) | Tối đa **50 tim** | 2026-11-12 |
| D8 | HeartXP (thay luật 100 kill) | Giết mob hostile được **HeartXP = máu tối đa của mob** (Zombie +20, Enderman +40, Ender Dragon +200, Wither +300, Warden +500). Đủ **500 HeartXP** = +1 tim, phần dư giữ lại. Ở trần thì không tích thêm. Lưu theo người chơi, không mất khi chết | 2026-11-12 |
| D9 | Thanh HeartXP | Thanh ở **góc dưới bên trái** màn hình, cách mép trái và mép dưới 8px, hiện tiến độ x/500. Cần cài mod ở client (cả host và bạn) | 2026-11-12 |
| D10 | Bật/tắt theo world | ~~Tên `permahearts:enabled`~~ → đổi tên ở D11. Game rule, **mặc định tắt**. Bật lúc tạo world (More → Game Rules) hoặc trong game bằng `/gamerule permahearts:enabled true/false`. Tắt: không mất tim, không HeartXP, máu về 10 tim, ẩn thanh; dữ liệu tim giữ nguyên để bật lại | 2026-11-12 |
| D11 | Công tắc chung | Mod Lữ Khách + linh dược nằm **chung mod PermaHearts** (jar `permahearts`, giữ id để không mất dữ liệu). Một công tắc cho tất cả, tên **ImmortalCultivation**: game rule `permahearts:immortal_cultivation`, mặc định tắt (thay `permahearts:enabled` của D10; world cũ phải bật lại bằng lệnh mới) | 2026-11-12 |
| D12 | Lữ Khách xuất hiện | Sau **3–5 ngày Minecraft** (ngẫu nhiên) một NPC **Lữ Khách** xuất hiện cách một người chơi ~**1000 block**; báo tọa độ trên chat, NPC hiện ra khi có người tới gần. Mỗi lần chỉ 1 NPC. Đứng yên, bất tử, ngoại hình Wandering Trader. Xong hết nhiệm vụ và lấy hết thưởng thì đi mất, đếm lại 3–5 ngày | 2026-11-12 |
| D13 | Nhiệm vụ | Bấm NPC: màn hình giới thiệu + nút Nhận → danh sách **2–4 dòng** ngẫu nhiên. Loại: thu thập (ô bỏ đồ), giết (đếm), Trăng máu (nút bắt đầu, 5 zombie mỗi 10 giây, tối đa 30 con sống, người bấm sống tới sáng là xong, chết thì làm lại), Thám hiểm (đứng trong một công trình), Hạ thủ lĩnh (nút triệu hồi trùm). Tiến độ **dùng chung** cho mọi người chơi | 2026-11-12 |
| D14 | Phần thưởng | Mỗi dòng xong thành **1 ô chứa 1 linh dược**, dùng chung: **ai lấy trước được**, người kia không thấy nữa | 2026-11-12 |
| D15 | Linh dược | Uống: +1 cấp chỉ số **vĩnh viễn, không trần, chết không mất**. Sức mạnh +0.5 sát thương, Cuồng Phong +10% tốc đánh, Thần Tốc +5% tốc chạy, Kim Cang +1 giáp, Trường Thủ +0.5 tầm với, Khai Sơn +10% tốc đào. Công tắc tắt: chỉ số không có tác dụng (vẫn lưu) | 2026-11-12 |
