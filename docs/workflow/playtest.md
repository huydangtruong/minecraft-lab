# Chơi thử

## Một mình, không đụng save thật

```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
./gradlew :permahearts:runClient
```

Tạo world Survival mới (Creative bật cheat nếu cần lệnh). Kịch bản PermaHearts:

0. Tạo world mới: **Create New World → More → Game Rules → ImmortalCultivation → bật** (mặc định tắt; hoặc trong game `/gamerule permahearts:immortal_cultivation true`). Tắt: `/gamerule permahearts:immortal_cultivation false` → máu về 10 tim, thanh HeartXP ẩn, chết không mất tim, `/hearts` nhắc cách bật; bật lại → tim cũ trở về. (Màn Game Rules chưa từng được nhìn thấy trong game.)
0b. Linh dược: `/give @s permahearts:elixir_strength 4`, uống hết → `/hearts` hiện +2.0 sát thương; chết → vẫn còn. Tắt công tắc → chỉ số về gốc, `/hearts` vẫn nhớ cấp; uống khi tắt thì không tiêu hao. (Màu chai, hiệu ứng lấp lánh chưa từng được nhìn thấy trong game.)
0c. Lữ Khách (0.5.0): `/gamerule permahearts:immortal_cultivation true`, `/quest summon` → Lữ Khách hiện cạnh bạn; bấm chuột phải → màn hình nhiệm vụ (không phải bảng trao đổi). Bấm **Nhận nhiệm vụ** → 2–4 dòng. Dòng "Tìm N X": `/give` đồ đó rồi bỏ vào ô bên phải (đủ thì dư trả lại, dòng chuyển xanh "Phần thưởng: …"); dòng "Giết N Y": giết mob đó, số đếm tăng (có chữ nhỏ trên thanh hotbar). Lấy linh dược ở ô thưởng (shift-click cũng được). Người thứ hai mở cùng lúc phải thấy cùng tiến độ; ai lấy thưởng trước được, ô của người kia trống. `/quest complete 1` để thử nhanh; `/quest` xem tiến độ; xong + lấy hết thưởng → Lữ Khách chào và đi. `/quest skip` → tọa độ hiện trên chat (~1000 block), đi tới trong ~96 block thì NPC hiện. Kiểm: ô khớp ô vẽ, chữ không tràn, kho đồ dùng được. (Màn hình chưa từng được nhìn thấy trong game.)
0d. Sự kiện nhiệm vụ (0.6.0): `/quest summon` nhiều lần (hoặc `/quest skip`) tới khi nhiệm vụ có dòng mới, hoặc `/quest complete N` các dòng khác. Nút nằm trong bảng, cạnh dòng chữ, bên trái ô thưởng; kiểm chữ tiếng Việt không đè lên nút.
   - **Trăng máu** ("Sống sót qua đêm trăng máu" + nút **Bắt đầu**): bấm → trời thành đêm, có thanh đỏ "Trăng Máu" làm tối màn hình, nút đổi thành "Đang diễn ra…" (xám). Mỗi 10 giây 5 zombie quanh bạn, không quá 30 con; thử ngủ giường → bị từ chối. Sống tới sáng → dòng xanh. Chết (hoặc thoát game) → tin "Trăng máu đã hạ gục…", nút bấm lại được. Người chơi creative/spectator không bị vây.
   - **Thám hiểm** ("Đặt chân tới Làng…"): đứng trong một làng/đền... (`/locate structure #minecraft:village` rồi `/tp`) → dòng chuyển xanh trong ~2 giây. Cần đã Nhận nhiệm vụ.
   - **Hạ thủ lĩnh** ("Hạ Thủ Lĩnh Zombie" + nút **Triệu hồi**): đứng gần Lữ Khách, bấm → zombie/skeleton to gấp đôi, tên hiện trên đầu, 150 máu, thanh tím trong ~64 block, 4 đệ tử; nút đổi thành "Đang chiến đấu…". Bấm lần nữa khi trùm còn sống → bị từ chối. Hạ trùm → dòng xanh.
1. `/hearts` → 10 tim, HeartXP 0/500.
2. `/kill` → hồi sinh 9 tim, máu đầy, có tin báo.
3. `/hearts set @s 1` rồi `/kill` → vẫn 1 tim + tin "mức thấp nhất".
4. `/hearts xp @s 480`, `/summon zombie`, giết bằng cung → +1 tim, âm thanh level-up, thanh HeartXP về 0 (zombie +20 XP).
5. `/summon warden`, giết (creative/lệnh) → +1 tim. Giết bò → HeartXP không đổi. `/hearts set @s 50` + giết zombie → vẫn 50 tim, thanh hiện "HeartXP TỐI ĐA".
6. Nhìn góc dưới bên trái: chữ "HeartXP n/500" trên thanh đỏ; F1 hoặc Creative/Spectator thì ẩn. (HUD chưa từng được nhìn thấy trong game, chỉ mới biên dịch.)
7. Thoát world, vào lại → giữ nguyên (cả HeartXP).

## Với bạn (Essential)

1. Huy: `scripts/install.sh`, khởi động lại profile "26.3 Fabric Essential", kiểm `logs/latest.log` có `PermaHearts loaded`.
2. Bạn: `scripts/package.sh` → gửi zip, bạn làm theo `INSTALL-vi.txt`.
3. Huy mở world, mời bạn qua Essential; chết thử → mất 1 tim; `/hearts` của mỗi người độc lập.

## Chụp màn hình tự động

Không cần chơi: một bài test mở Minecraft thật trong world tạm, mở Lữ Khách bằng chuột phải thật và chụp màn hình (nhận nhiệm vụ, các dòng, dòng đã xong, tooltip phần thưởng).

```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
./gradlew :permahearts:runClientGameTest
```

Một cửa sổ Minecraft sẽ hiện ~30 giây rồi tự đóng. Ảnh nằm ở `mods/permahearts/build/run/clientGameTest/screenshots/`. `./gradlew :permahearts:build` không chạy bài này.

## 0.7.1: Give up, trùm trong hang, đếm ngược

1. `/quest summon`, mở Lữ Khách: cột phải có dòng "Leaves in 3 days 0 h (≈ 60 min)" dưới chữ giới thiệu, nút **Give up** chữ đỏ ở cuối cột trái. `/quest` cũng in thời gian còn lại.
2. Bấm Give up 1 lần: chữ đổi thành "Sure? Click again" (3 giây rồi trở lại). Bấm lần 2 trong 3 giây: Lữ Khách đi, tin nhắn "gave up", `/quest` báo lần sau còn 5–7 ngày.
3. Đào xuống hang, `/quest summon` trong hang, nhận nhiệm vụ Hạ thủ lĩnh, bấm Summon: trùm và 4 đệ tử hiện ngay trong hang cạnh bạn, không phải trên mặt đất. Trăng máu cũng sinh quái quanh bạn trong hang.
4. Zombie/skeleton quanh Lữ Khách không nhắm vào NPC. Trùm zombie/skeleton đứng ngoài nắng không bốc cháy.
5. Lỡ mất trùm (xóa bằng `/kill @e[tag=permahearts_boss]`): nút trở lại "Summon".
6. `/time add 72000` (hoặc đợi): tin "will leave soon" ~2 phút thực trước giờ, rồi Lữ Khách đi (không đi khi đang đánh trùm hoặc Trăng máu, đi ngay sau khi xong).
