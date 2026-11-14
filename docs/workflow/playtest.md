# Chơi thử

## Một mình, không đụng save thật

```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
./gradlew :permahearts:runClient
```

Tạo world Survival mới (Creative bật cheat nếu cần lệnh). Kịch bản PermaHearts:

0. Tạo world mới: **Create New World → More → Game Rules → ImmortalCultivation → bật** (mặc định tắt; hoặc trong game `/gamerule permahearts:immortal_cultivation true`). Tắt: `/gamerule permahearts:immortal_cultivation false` → máu về 10 tim, thanh HeartXP ẩn, chết không mất tim, `/hearts` nhắc cách bật; bật lại → tim cũ trở về. (Màn Game Rules chưa từng được nhìn thấy trong game.)
0b. Linh dược: `/give @s permahearts:elixir_strength 4`, uống hết → `/hearts` hiện +2.0 sát thương; chết → vẫn còn. Tắt công tắc → chỉ số về gốc, `/hearts` vẫn nhớ cấp; uống khi tắt thì không tiêu hao. (Màu chai, hiệu ứng lấp lánh chưa từng được nhìn thấy trong game.)
0c. Lữ Khách (0.5.0): `/gamerule permahearts:immortal_cultivation true`, `/quest summon` → Lữ Khách hiện cạnh bạn; bấm chuột phải → màn hình nhiệm vụ (không phải bảng trao đổi). Bấm **Nhận nhiệm vụ** → 2–4 dòng. Dòng "Tìm N X": `/give` đồ đó rồi bỏ vào ô bên phải (đủ thì dư trả lại, dòng chuyển xanh "Phần thưởng: …"); dòng "Giết N Y": giết mob đó, số đếm tăng (có chữ nhỏ trên thanh hotbar). Lấy linh dược ở ô thưởng (shift-click cũng được). Người thứ hai mở cùng lúc phải thấy cùng tiến độ; ai lấy thưởng trước được, ô của người kia trống. `/quest complete 1` để thử nhanh; `/quest` xem tiến độ; xong + lấy hết thưởng → Lữ Khách chào và đi. `/quest skip` → tọa độ hiện trên chat (~500 block), đi tới trong ~96 block thì NPC hiện. Kiểm: ô khớp ô vẽ, chữ không tràn, kho đồ dùng được. (Màn hình chưa từng được nhìn thấy trong game.)
0d. Sự kiện nhiệm vụ (0.6.0): `/quest summon` nhiều lần (hoặc `/quest skip`) tới khi nhiệm vụ có dòng mới, hoặc `/quest complete N` các dòng khác. Nút nằm trong bảng, cạnh dòng chữ, bên trái ô thưởng; kiểm chữ tiếng Việt không đè lên nút.
   - **Trăng máu** ("Sống sót qua đêm trăng máu" + nút **Bắt đầu**): bấm → trời thành đêm, có thanh đỏ "Trăng Máu" làm tối màn hình, nút đổi thành "Đang diễn ra…" (xám). Mỗi 10 giây 3 zombie quanh bạn, không quá 30 con; đi ra xa ~48 block khỏi Lữ Khách → bị kéo lại, có bụi đỏ ở mép (D33); thử ngủ giường → bị từ chối. Sống tới sáng → dòng xanh. Chết (hoặc thoát game) → tin "Trăng máu đã hạ gục…", nút bấm lại được. Người chơi creative/spectator không bị vây.
   - **Thám hiểm** ("Đặt chân tới Làng…"): đứng trong một làng/đền... (`/locate structure #minecraft:village` rồi `/tp`) → dòng chuyển xanh trong ~2 giây. Cần đã Nhận nhiệm vụ.
   - **Hạ thủ lĩnh** ("Hạ Thủ Lĩnh Zombie" + nút **Triệu hồi**): đứng gần Lữ Khách, bấm → zombie/skeleton to gấp đôi, tên hiện trên đầu, 150 máu, thanh tím trong ~64 block, 4 đệ tử; nút đổi thành "Đang chiến đấu…". Bấm lần nữa khi trùm còn sống → bị từ chối. Hạ trùm → dòng xanh.
   - Trùm **Ghast** (D35): hiện lơ lửng ngoài trời gần Lữ Khách (kể cả khi Lữ Khách ở trong hang), 4 Phantom bay quanh, không cháy nắng. Chạy ra xa: Ghast bám theo nhưng không ra quá ~24 block quanh Lữ Khách; luôn lơ lửng tối đa ~16 block trên đầu bạn. Bắn khoảng 2,5 giây/phát (vanilla 3 giây). Cầu lửa phá block (theo gamerule mobGriefing).
1. `/hearts` → 10 tim, HeartXP 0/500.
2. `/kill` → hồi sinh 9 tim, máu đầy, có tin báo.
3. `/hearts set @s 1` rồi `/kill` → vẫn 1 tim + tin "mức thấp nhất".
4. `/hearts xp @s 480`, `/summon zombie`, giết bằng cung → +1 tim, âm thanh level-up, thanh HeartXP về 0 (zombie +20 XP).
5. `/summon warden`, giết (creative/lệnh) → +1 tim. Giết bò → HeartXP không đổi. `/hearts set @s 300` + giết zombie → vẫn 300 tim, thanh hiện "HeartXP TỐI ĐA".
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
2. Bấm Give up 1 lần: chữ đổi thành "Sure? Click again" (3 giây rồi trở lại). Bấm lần 2 trong 3 giây: Lữ Khách đi, tin nhắn "gave up", `/quest` báo lần sau còn 4 ngày.
3. Đào xuống hang, `/quest summon` trong hang, nhận nhiệm vụ Hạ thủ lĩnh, bấm Summon: trùm và 4 đệ tử hiện ngay trong hang cạnh bạn, không phải trên mặt đất. Trăng máu cũng sinh quái quanh bạn trong hang.
4. Zombie/skeleton quanh Lữ Khách không nhắm vào NPC. Trùm zombie/skeleton đứng ngoài nắng không bốc cháy.
5. Lỡ mất trùm (xóa bằng `/kill @e[tag=permahearts_boss]`): nút trở lại "Summon".
6. `/time add 72000` (hoặc đợi): tin "will leave soon" ~2 phút thực trước giờ, rồi Lữ Khách đi (không đi khi đang đánh trùm hoặc Trăng máu, đi ngay sau khi xong).

## Thanh tim nhiều lớp (D36)

1. Survival, `/hearts set @s 21`: vẫn 2 hàng, tim đầu tiên hàng dưới màu cam. Bị đánh mất nửa tim → tim cam còn nửa; mất thêm → hết cam, còn 20 tim đỏ.
2. `/hearts set @s 57`: hàng dưới 8 tim hồng đầu rồi cam, hàng trên cam. `/hearts set @s 100`: 20 tim xanh dương.
3. Mặc giáp: thanh giáp nằm ngay trên 2 hàng tim. Ăn táo vàng: tim vàng ở hàng trên. Uống thuốc độc: mọi tim màu độc.
4. D37: `/hearts set @s 150` → tim nhạt (hồng phấn trên nền đào). `/hearts set @s 300` → toàn huyết nham; bị đánh dần: lửa linh hồn, lửa xanh lá, thiên hà, hoa anh đào lần lượt lộ ra (D38).

## Bảng tu luyện (D39)

1. Mở túi đồ (Survival): cạnh nút sách công thức có nút phiến đá Cánh hồn bo góc (D40); rê chuột → nền xanh như nút bên cạnh, tooltip "Cultivation".
2. Bấm: bảng trái mở, bên trái bảng có 2 tab. Tab Nether Star = Stats; tab sách = Techniques (xem mục Chiêu thức bên dưới). Bấm qua lại đổi nội dung.
3. Cầm đồ trên chuột rồi bấm vào tab: đồ không bị rơi.

## Vitality hồi máu tự nhiên (D41)

1. Survival, uống 4 bình Vitality, bảng Stats ghi "Food heal +80%" và "Food regen +80%".
2. Ăn no (thanh đói đầy), tự làm mất vài tim: máu hồi nhanh hơn rõ so với chưa uống (khoảng 1,8 lần).

## Chiêu thức: tab Techniques (P-011 phần A)

Cần bật công tắc thế giới. Lệnh trợ giúp (op): `/technique set <người> <chiêu> <cấp>`, `/technique emeralds <người> <số>`, `/technique cooldowns <người> clear`. Tên chiêu: fire, ice, healing, phase, repulse, sacrifice.

1. Survival, mở túi đồ, bấm nút Cultivation, chọn tab sách (Techniques): hàng 4 ô khóa (hình ổ khóa), ô ngọc lục bảo bên phải ghi "0/64", dưới là danh sách 6 chiêu, tất cả "Lv 0" màu xám.
2. Nhặt 30 ngọc lục bảo, bấm vào chúng trong túi để cầm lên chuột, bấm vào ô ngọc lục bảo của bảng: ô ghi "30/64", ngọc trên chuột hết. Nạp thêm tới 64: ô khóa đầu mở. Chỉ lấy đủ số cần (tối đa 1856 ngọc tổng cộng), phần dư ở lại trên chuột.
3. `/xp add @s 40 levels`, bấm hàng "Flame Burst": trang chi tiết (tên, "Lv 0 / 20", dòng mô tả, thông số cấp 1). Bấm "Learn (6 levels)": thanh cấp XP giảm 6, chiêu lên Lv 1; thông số hiện dạng "16 → 18 blocks" (hiện tại → cấp sau). Bấm tiếp: cấp 2 tốn 7 cấp XP; nút xám khi không đủ XP.
4. Bấm "Reset +N XP": chiêu về Lv 0, nhận lại một nửa số điểm XP đã trả. Bấm nút "Back" ở góc trên phải để về danh sách.
5. Bấm một ô đã mở: viền vàng. Khi đó mỗi chiêu từ Lv 1 trở lên có nút ✓ ở cuối hàng: bấm ✓ để gắn chiêu vào ô. Gắn cùng chiêu vào ô khác thì nó dời đi (không nhân đôi). Chuột phải vào ô để gỡ chiêu.
6. Chiêu đang hồi (phần A chưa có lệnh dùng chiêu, thử bằng phần C) thì ô xám có số giây, không thay/gỡ/reset được. `/technique cooldowns @s clear` xóa hồi chiêu.
7. Bấm vào bảng khi đang cầm đồ: đồ không bị rơi ra ngoài.
8. Tắt công tắc thế giới: nâng cấp/gắn/nạp ngọc đều bị từ chối (dòng báo trên hotbar), không mất gì.

