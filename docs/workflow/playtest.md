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
2. Bấm: bảng trái mở, bên trái bảng có 2 tab. Tab Nether Star = Stats; tab sách = Techniques ("No techniques yet"). Bấm qua lại đổi nội dung.
3. Cầm đồ trên chuột rồi bấm vào tab: đồ không bị rơi.

## Vitality hồi máu tự nhiên (D41)

1. Survival, uống 4 bình Vitality, bảng Stats ghi "Food heal +80%" và "Food regen +80%".
2. Ăn no (thanh đói đầy), tự làm mất vài tim: máu hồi nhanh hơn rõ so với chưa uống (khoảng 1,8 lần).

## Chiêu thức: hiệu ứng phía server (P-011 phần C)

Chưa có phím hay thanh chiêu (phần B), nên thử bằng lệnh op `/technique cast <chiêu> <level 1-20> [charge 0-1]`, chạy bởi chính bạn. Chiêu: `fire`, `ice`, `healing`, `phase`, `repulse`, `sacrifice`. Cần công tắc ImmortalCultivation đang BẬT (tắt thì lệnh báo lỗi, chiêu không làm gì). Dùng `/summon husk ~5 ~ ~ {NoAI:1b}` làm bia (Husk không cháy nắng).

1. **Lửa**: nhìn thẳng vào Husk cách ~6 block, `/technique cast fire 1`. Cầu lửa bay tới, nổ ra vòng lửa bán kính ~2 trong 4 giây; Husk bị 4 sát thương rồi cháy. Không có block lửa nào xuất hiện, cỏ/gỗ gần đó không cháy. `fire 6` trở lên cháy mạnh hơn (2 máu/giây), vẫn lửa thường. `fire 10 0.5` ném gần hơn (nửa tầm).
2. **Băng**: `/technique cast ice 10`. Vùng tuyết bán kính ~4,8 trong 6 giây; Husk trong vùng có hiệu ứng **Frostbite** (biểu tượng bông tuyết, đi chậm). Ra khỏi vùng, hiệu ứng còn 3 giây. Bạn đứng trong vùng của chính mình: không bị gì.
3. **Hồi máu**: gây vài sát thương cho bản thân và cho một con bò/Husk gần đó, đứng gần nhau, `/technique cast healing 1`. Vòng tròn sáng bán kính 4 ở chân bạn trong 10 giây; mỗi giây hồi 2 máu (tổng 10 tim) cho MỌI sinh vật trong vòng, kể cả Husk. Ra khỏi vòng thì thôi hồi.
4. **Xuyên tường**: đứng sát một bức tường đá, `/technique cast phase 1`. Trong 10 giây đi xuyên được mọi block ngang chân trở lên (tường, đồi); sàn dưới chân vẫn đỡ bạn, không rơi. Không bị ngạt trong tường (hình có thể tối đen khi đầu nằm trong block, như vanilla). Hết giờ mà đang kẹt trong block: bạn được đẩy lên chỗ trống đầu tiên, không chết ngạt. Hạt tím quanh người.
5. **Hất tung**: vây 2-3 Husk (bỏ `NoAI`) trong 3 block, `/technique cast repulse 5`. Chúng bị hất ra xa theo hướng từ bạn, mất vài máu; bạn không mất máu. Vòng hạt trắng loe ra.
6. **Hiến tế**: `/technique cast sacrifice 1`: máu còn 20% (Lv 10: 50%, Lv 20: 30%), không bao giờ dưới nửa tim cuối. 5 giây (Lv 10: 8 giây) không nhận sát thương nào (để Husk đánh thử); hạt đỏ quanh người. `/kill` và rơi xuống hố hư không vẫn giết được. Hết giờ thì nhận sát thương lại.
7. **PvP tắt vẫn trúng**: 2 người, `/gamerule pvp false`. Người A `/technique cast repulse 5` sát người B: B vẫn mất máu và bị hất, dù đánh tay thì không mất (chiêu cố ý đi vòng luật PvP, D44). Hệ quả: người chết vì chiêu thì không ai được tính công và thông báo chết không nêu tên người ra chiêu.
8. Tắt công tắc (`/gamerule permahearts:immortal_cultivation false`): `/technique cast ...` báo lỗi, không có gì xảy ra.

Kiểm bằng máy: `./gradlew :permahearts:build` (game test cho từng chiêu) và `./gradlew :permahearts:runClientGameTest` (ảnh `20-fire-field` đến `26-phase-inside-wall-third-person` trong `mods/permahearts/build/run/clientGameTest/screenshots/`).
