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
4. Bấm "Reset +N XP": chiêu về Lv 0, nhận lại một nửa số điểm XP đã trả. Trang chi tiết có một dòng đầu: nút "Back" bên trái, rồi biểu tượng và tên chiêu, dòng "Lv N / 20" nằm dưới tên (không còn hàng ô chiêu ở trang này). Bấm "Back" để về danh sách. Danh sách có hàng cao hơn, cuộn bằng con lăn chuột (thanh cuộn mảnh bên phải); hàng 4 ô chiêu luôn dính ở trên cùng.
5. Bấm TRÁI một ô đã mở: chế độ sửa (viền vàng), ô ngọc lục bảo biến thành nút ✕ (chữ bên cạnh ẩn). Bấm ✕ hoặc bấm lại đúng ô đó để thoát. Trong chế độ sửa, mỗi chiêu từ Lv 1 trở lên (trừ chiêu đang ở ô đó) có nút + ở cuối hàng: bấm + để đặt chiêu vào ô, rồi thoát chế độ sửa. Chiêu đang ở ô khác thì dời đi và ô cũ thành trống (ví dụ ô 1 Fire, ô 2 Ice, sửa ô 1, bấm + ở Ice: ô 1 Ice, ô 2 trống). Bấm PHẢI vào ô để gỡ chiêu.
6. Chiêu trong ô đang hồi (thử bằng phần C) thì ô xám có số giây; bấm trái hay phải vào ô đó đều không làm gì (di chuột vào ô thấy số giây còn lại), và Reset chiêu cũng bị từ chối. `/technique cooldowns @s clear` xóa hồi chiêu.
7. Bấm vào bảng khi đang cầm đồ: đồ không bị rơi ra ngoài.
8. Tắt công tắc thế giới: nâng cấp/gắn/nạp ngọc đều bị từ chối (dòng báo trên hotbar), không mất gì.

## Chiêu thức: hiệu ứng phía server (P-011 phần C)

Chưa có phím hay thanh chiêu (phần B), nên thử bằng lệnh op `/technique cast <chiêu> <level 1-20> [charge 0-1]`, chạy bởi chính bạn. Chiêu: `fire`, `ice`, `healing`, `phase`, `repulse`, `sacrifice`. Cần công tắc ImmortalCultivation đang BẬT (tắt thì lệnh báo lỗi, chiêu không làm gì). Dùng `/summon husk ~5 ~ ~ {NoAI:1b}` làm bia (Husk không cháy nắng).

1. **Lửa**: nhìn thẳng vào Husk cách ~6 block, `/technique cast fire 1`. Cầu lửa bay tới, nổ ra vòng lửa bán kính ~2 trong 4 giây; Husk bị 4 sát thương rồi cháy. Không có block lửa nào xuất hiện, cỏ/gỗ gần đó không cháy. `fire 6` trở lên cháy mạnh hơn (2 máu/giây), vẫn lửa thường. `fire 10 0.5` ném gần hơn (nửa tầm).
2. **Băng**: `/technique cast ice 10`. Vùng tuyết bán kính ~4,8 trong 6 giây; Husk trong vùng có hiệu ứng **Frostbite** (biểu tượng bông tuyết, đi chậm). Ra khỏi vùng, hiệu ứng còn 3 giây. Bạn đứng trong vùng của chính mình: không bị gì.
3. **Hồi máu**: gây vài sát thương cho bản thân và cho một con bò/Husk gần đó, đứng gần nhau, `/technique cast healing 1`. Vòng tròn sáng bán kính 4 ở chân bạn trong 10 giây; mỗi giây hồi 2 máu (tổng 10 tim) cho MỌI sinh vật trong vòng, kể cả Husk. Ra khỏi vòng thì thôi hồi.
4. **Xuyên tường**: đứng sát một bức tường đá, `/technique cast phase 1`. Trong 10 giây đi xuyên được mọi block ngang chân trở lên (tường, đồi); sàn dưới chân vẫn đỡ bạn. Đi qua chỗ hố/vực thì rơi xuống, sàn tự hạ theo: tới tầng thấp hơn vẫn xuyên tường được và đứng được trên block bên dưới (sàn chỉ hạ, không bao giờ nâng). Không bị ngạt và không bị tối khi đầu nằm trong block: nhìn xuyên block như chế độ spectator, thấy hang động phía trước. Hết giờ mà đang kẹt trong block: bạn được đẩy lên chỗ trống đầu tiên, không chết ngạt. Hạt tím quanh người.
5. **Hất tung**: vây 2-3 Husk (bỏ `NoAI`) trong 3 block, `/technique cast repulse 5`. Chúng bị hất ra xa theo hướng từ bạn, mất vài máu; bạn không mất máu. Vòng hạt trắng loe ra.
6. **Hiến tế**: `/technique cast sacrifice 1`: máu còn 20% (Lv 10: 50%, Lv 20: 30%), không bao giờ dưới nửa tim cuối. 5 giây (Lv 10: 8 giây) không nhận sát thương nào (để Husk đánh thử); hạt đỏ quanh người. `/kill` và rơi xuống hố hư không vẫn giết được. Hết giờ thì nhận sát thương lại.
7. **PvP tắt vẫn trúng**: 2 người, `/gamerule pvp false`. Người A `/technique cast repulse 5` sát người B: B vẫn mất máu và bị hất, dù đánh tay thì không mất (chiêu cố ý đi vòng luật PvP, D44). Hệ quả: người chết vì chiêu thì không ai được tính công và thông báo chết không nêu tên người ra chiêu.
8. Tắt công tắc (`/gamerule permahearts:immortal_cultivation false`): `/technique cast ...` báo lỗi, không có gì xảy ra.

Kiểm bằng máy: `./gradlew :permahearts:build` (game test cho từng chiêu) và `./gradlew :permahearts:runClientGameTest` (ảnh `20-fire-field` đến `26-phase-inside-wall-third-person` trong `mods/permahearts/build/run/clientGameTest/screenshots/`).

## Chiêu thức: thanh chiêu, phím R, dùng chiêu (P-011 phần B)

Cần công tắc thế giới BẬT, chế độ Survival (Creative/Spectator không hiện thanh chiêu và không dùng được chiêu, giống thanh HeartXP). Chuẩn bị: `/technique set @s fire 10`, `/technique set @s ice 5`, `/technique set @s healing 3`, `/technique emeralds @s 832` (mở 3 ô), rồi gắn chiêu vào ô ở tab Techniques (chưa gắn thì thanh chỉ có ô ✕).

**Lưu ý phím R**: Essential cũng dùng R cho bảng emote. Hãy đổi một trong hai: Options > Controls > Key Binds > mục "Immortal Cultivation" > "Technique mode (toggle)", hoặc đổi phím emote trong Essential. (Bản cũ đặt nhầm mặc định là mũi tên lên; nếu máy bạn còn lưu phím cũ thì gán lại R.)

1. **Thanh chiêu bên phải (chỉ để xem)**: góc dưới phải, cách mép 8 px: nhãn phẳng ghi tên phím (ví dụ `R`), rồi mỗi chiêu đã gắn một ô (hình chiêu + số giây hồi khi đang hồi). Không có khung chọn ở đây.
2. **Bấm R = bật/tắt chế độ chiêu**: các chiêu đã gắn chèn vào **đầu thanh đồ** (ô nền tối, viền vàng nhạt), đồ của bạn dịch sang phải (N ô cuối thanh đồ tạm ẩn). Ô đang chọn giữ nguyên số khi bật/tắt (ô 1 = kiếm → bấm R → ô 1 = chiêu 1 → bấm R → ô 1 = kiếm). Cuộn chuột / bấm phím 1-9 chọn trên thanh đã dịch: ô chiêu thì khung chọn **màu vàng**, ô đồ thì khung trắng như thường (đồ ở ô thứ N+1 là món số 1 trong túi). Đồ trong túi không bị di chuyển. Chiêu đang chọn hiện tên và cấp phía trên thanh bên phải; tay chính hiện vật tiêu điểm + hạt như trước. Chế độ tự tắt khi chết, tắt công tắc, hoặc không còn ô nào gắn chiêu.
3. (Bảng tròn khi giữ R đã bỏ.)
4. **Chuột trái khi đã chọn chiêu**: ném ngay với sức đầy (Lửa/Băng) hoặc dùng luôn (Hồi máu, Xuyên tường, Hất tung, Hiến Tế). Đánh/đào/đặt block bằng đồ cầm tay bị chặn khi đang chọn chiêu; muốn đánh bình thường thì cuộn / bấm 1-9 sang ô đồ, hoặc bấm R tắt chế độ chiêu.
5. **Chuột phải giữ** với Lửa/Băng: màn hình **zoom dần như kéo cung**, thanh nạp hiện **giữa màn hình, ngay trên hàng máu/giáp**, đầy sau 1 giây (chuyển vàng). **Thả** để ném: nạp nửa thì bay gần hơn. Cầu lửa/băng nổ ở chỗ nó dừng dù rơi xuống đất, nước hay hết thời gian bay (lỗi cầu lửa nạp đầy rơi không nổ đã sửa). Với các chiêu dùng ngay thì bấm chuột phải là dùng.
6. **Hồi chiêu**: ngay sau khi dùng, ô chiêu bị phủ xám, thu nhỏ từ trên xuống, ghi số giây còn lại (Lửa 16 s, Băng 24 s, Hồi máu 60 s, Xuyên tường 90 s, Hất tung 20 s, Hiến Tế 45 s). Dùng khi đang hồi: dòng chữ "On cooldown: N s" hiện giữa/dưới màn hình, không ném, không tốn gì. Chiêu đang hồi vẫn chọn được.
7. **Tự bỏ chọn**: chọn một ô rồi vào tab Techniques gỡ chiêu khỏi ô đó, hoặc chết, hoặc tắt công tắc thế giới: thanh tự về ✕ và vật tiêu điểm biến mất.
8. **Chơi chung** (Essential): người kia chọn chiêu thì bạn thấy họ cầm vật tiêu điểm và hạt ở tay; họ đổi/bỏ chọn thì mất.

Kiểm bằng máy: `./gradlew :permahearts:build` (game test cast/cooldown/chọn) và `./gradlew :permahearts:runClientGameTest` (ảnh `30-skill-bar-cross` đến `46-focus-third-person` trong `mods/permahearts/build/run/clientGameTest/screenshots/`).

## Chiêu Bay và Huyết Kiếm (P-014, D52)

Chuẩn bị (Survival, công tắc BẬT): `/technique set @s flight 10`, `/technique set @s blade 5`, `/technique emeralds @s 832`, gắn Bay và Huyết Kiếm vào 2 ô ở tab Techniques, bấm R bật chế độ chiêu.

1. **Bay**: chọn ô Bay, bấm chuột phải hoặc trái. Bạn bay lên ngay (không cần double-jump), bay như Creative: Space lên, Shift xuống, bấm đúp Space để tắt/bật bay. Lv 1 bay đúng tốc độ Creative trong 20 giây; Lv 10 nhanh hơn 50% trong 40 giây; Lv 20 nhanh gấp đôi trong 60 giây. Đang bay có hạt mây trắng dưới chân; bên phải màn hình, phía trên tên chiêu, hiện "Flight N s" đếm ngược (đỏ khi còn dưới 5 giây).
2. **Hết giờ**: hết thời gian là mất khả năng bay, tốc độ bay về bình thường, được 5 giây rơi chậm (không chết vì rơi). Ô Bay hồi 120 giây (bắt đầu từ lúc dùng).
3. **Bay + chết / thoát vào lại**: chết khi đang bay: hồi sinh bình thường, không còn bay. Thoát game rồi vào lại: nếu còn thời gian thì vẫn bay tiếp, nếu hết thì tự tắt. Người đang Creative/Spectator không bị chiêu này đổi gì (dùng chiêu cũng không được).
4. **Huyết Kiếm**: chọn ô Huyết Kiếm: tay chính cầm thanh kiếm nhuốm máu, có giọt máu nhỏ ở tay. Nhắm vào Husk/bò (trong tầm đánh) rồi bấm chuột trái: sát thương = sát thương cơ bản của bạn + 5 (Lv 1; +0.5 mỗi cấp) + phần linh dược Sức mạnh. Kiếm/rìu cầm trong túi (ví dụ kiếm kim cương) KHÔNG cộng thêm. Giáp của đối thủ giảm sát thương như bình thường. Có hất lùi như đánh thường, hạt đỏ bay từ mục tiêu về phía bạn.
5. **Hút máu**: bạn hồi đúng bằng số máu mục tiêu thực sự bị mất (giáp giảm thì hồi cũng ít đi; đánh con còn ít máu thì chỉ hồi bằng phần máu còn lại). Thử bằng cách để máu mình thấp rồi đánh.
6. **Hồi chiêu 12 giây**: mỗi cú đánh trúng khiến ô Huyết Kiếm hồi 12 giây (hiện ở thanh chiêu bên phải như chiêu khác). Đang hồi mà bấm chuột trái: hiện "On cooldown: N s", không đánh. Vung hụt (không nhắm trúng gì) không tốn hồi chiêu. Chuột phải với kiếm: không làm gì. Đào/đập block bằng kiếm: không làm gì.
7. Đánh người chơi khác bằng Huyết Kiếm khi PvP tắt: vẫn trúng (đi qua `TechniqueDamage`, D44).

Kiểm bằng máy: `./gradlew :permahearts:build` (game test `FlightBladeGameTest`: bay, hết giờ, chết, Creative, sát thương, hút máu, linh dược, tầm đánh) và `./gradlew :permahearts:runClientGameTest` (ảnh `50-blade-in-hand`, `51-blade-hit`, `52-blade-cooldown`, `53-flight-hud-timer`, `54-flight-third-person`, và `46-focus-flight`, `47-focus-blade`).
