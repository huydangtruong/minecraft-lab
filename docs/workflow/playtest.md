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
   - **Hạ thủ lĩnh** ("Hạ Thủ Lĩnh Zombie" + nút **Triệu hồi**): đứng gần Lữ Khách, bấm → zombie/skeleton to gấp đôi, tên hiện trên đầu, 200 máu (D72), thanh tím trong ~64 block, 4 đệ tử; nút đổi thành "Đang chiến đấu…". Bấm lần nữa khi trùm còn sống → bị từ chối. Hạ trùm → dòng xanh.
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
5. Bấm TRÁI một ô đã mở: chế độ sửa (viền vàng), ô ngọc lục bảo biến thành nút ✕ (chữ bên cạnh ẩn). Bấm ✕ hoặc bấm lại đúng ô đó để thoát. Trong chế độ sửa, **không có nút +** (D91): bấm vào dòng chiêu (từ Lv 1 trở lên) là đặt chiêu vào ô rồi thoát chế độ sửa; nút "i" nằm sát mép phải của dòng. Chiêu đang ở ô khác thì dời đi và ô cũ thành trống (ví dụ ô 1 Fire, ô 2 Ice, sửa ô 1, bấm dòng Ice: ô 1 Ice, ô 2 trống). Bấm PHẢI vào ô để gỡ chiêu.
6. Chiêu trong ô đang hồi (thử bằng phần C) thì ô xám có số giây; bấm trái hay phải vào ô đó đều không làm gì (di chuột vào ô thấy số giây còn lại), và Reset chiêu cũng bị từ chối. `/technique cooldowns @s clear` xóa hồi chiêu.
7. Bấm vào bảng khi đang cầm đồ: đồ không bị rơi ra ngoài.
8. Tắt công tắc thế giới: nâng cấp/gắn/nạp ngọc đều bị từ chối (dòng báo trên hotbar), không mất gì.

## Chiêu thức: hiệu ứng phía server (P-011 phần C)

Chưa có phím hay thanh chiêu (phần B), nên thử bằng lệnh op `/technique cast <chiêu> <level 1-20> [charge 0-1]`, chạy bởi chính bạn. Chiêu: `fire`, `ice`, `healing`, `phase`, `repulse`, `sacrifice`. Cần công tắc ImmortalCultivation đang BẬT (tắt thì lệnh báo lỗi, chiêu không làm gì). Dùng `/summon husk ~5 ~ ~ {NoAI:1b}` làm bia (Husk không cháy nắng).

1. **Lửa**: nhìn thẳng vào Husk cách ~6 block, `/technique cast fire 1`. Cầu lửa bay tới, nổ ra vòng lửa bán kính ~2 trong 4 giây; Husk bị 4 sát thương rồi cháy. Không có block lửa nào xuất hiện, cỏ/gỗ gần đó không cháy. `fire` cấp cao cháy mạnh hơn (+0,2 máu/giây mỗi level, D72), vẫn lửa thường. `fire 10 0.5` ném gần hơn (nửa tầm).
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
## P-013 Tầm nhìn Xuyên tường (D51)

Survival, công tắc BẬT, `/technique set @s phase 1`. Đứng cạnh một vách đá lớn (có hang phía trong), dùng Xuyên tường rồi đi vào trong đá.
1. **Cấp 1**: trong đá, màn hình tối xám như ở trong đá; hang cách 6 block vẫn thấy, thứ cách 15+ block chìm vào sương mù đen (tầm nhìn 8 block, sương bắt đầu mờ từ khoảng 5 block).
2. `/technique set @s phase 20` rồi dùng lại: tầm nhìn xa tới 48 block (sương mờ từ khoảng 29 block), thấy hang và quặng ở xa hơn nhiều.
3. Ra khỏi đá (đứng ngoài trời hoặc trong hang) khi còn đang xuyên tường: sương mù trở lại bình thường như vanilla. Hết thời gian Xuyên tường cũng vậy.

Kiểm bằng máy: `./gradlew :permahearts:runClientGameTest` (ảnh `27-phase-xray-cave` cấp 1 và `28-phase-xray-level-20`; test kiểm khoảng cách kết thúc sương 8 và 48).

## Lửa đốt gỗ (D55)

1. Ném Lửa vào một cây hoặc tường gỗ: chỗ trúng và gỗ quanh bãi lửa bắt lửa, lửa có thể lan như lửa thường.
2. Ném vào mob trên nền đá: mob cháy, nền đá không có lửa.

## Quantum Leap, trước là Thiên Lý Bộ / Thousand Mile Step (D58, D74, P-019)

Survival, công tắc BẬT, `/technique set @s teleport 1` (hoặc học trong tab chiêu), gán vào ô, bấm R chọn ô đó (pháp khí tím trên tay).
1. **Gồng**: giữ chuột phải, thanh lực đầy sau **3 giây** (lâu gấp 3 Fireball), màn hình zoom dần như cung, hạt tím quanh tay. Thả ra: dịch chuyển theo hướng nhìn. Chuột trái = lực tối đa ngay.
2. **Cấp 1 hết lực** nhìn ngang ra đồng bằng: đáp xuống mặt đất cách khoảng 150 block. Chạm nhẹ (lực thấp) vẫn đi được ít nhất 20% (30 block). `/technique set @s teleport 20`: tối đa ~2500 block.
3. Nhìn vào vách núi/tường gần: đáp ngay trước vách (hoặc trên mặt sàn nếu nhìn xuống đất), không chui vào khối.
4. Không rơi sát thương sau khi đáp (kể cả đáp trên cao xuống đất). Có tiếng Enderman, hạt portal ở hai đầu và vệt hạt dọc đường.
5. Nhìn ra đại dương xa: đáp trên mặt nước. Nhìn về phía hồ dung nham lớn / vực không đáy (Nether, End): thông báo "No safe place to land", không dịch chuyển, **không** vào hồi chiêu.
6. Ở Nether chiêu không đưa lên nóc bedrock (tìm khoảng trống gần độ cao hiện tại).

Kiểm bằng máy: `./gradlew :permahearts:build` (game test `ThousandMileStepGameTest`: 150 block, sàn 20%, tường, dung nham bị từ chối) và `runClientGameTest` (ảnh `60-teleport-focus-in-hand`, `61-teleport-charging`, `62-teleport-after`).

## P-020: Huyết Kiếm nâng cấp và Violet Guardian

1. **Huyết Kiếm**: sát thương cấp 1 là +8 (trước +5), cấp 20 là +17.5. Chém trúng: mục tiêu **chảy máu** 4 giây (mất 1 máu mỗi giây, hạt đỏ, có biểu tượng giọt máu) và bị **đánh văng**: cấp 1 khoảng 2 block, cấp 20 khoảng 6 block (quái xác sống cũng chảy máu).
2. **Violet Guardian** (chiêu thứ 10, `/technique set @s golem 20`): bấm một lần, triệu hồi Iron Golem **màu tím** cạnh bạn. Số lượng: 1 (cấp 1-9), 2 (10-14), 3 (15-19), 4 (cấp 20). Máu và sát thương bằng 0.5x golem thường ở cấp 1, 2x ở cấp 20.
3. Golem đi theo bạn như chó, tự đánh quái quanh bạn và quái đánh bạn; **không** đánh người chơi, bạn đánh chúng thì không gây sát thương. Xa hơn khoảng 16 block thì dịch chuyển về cạnh bạn.
4. Tồn tại **90 giây** rồi biến mất kèm hạt khói; bạn chết/thoát game hoặc dùng lại chiêu thì biến mất ngay (dùng lại thì thay bằng bộ mới). Hồi chiêu **180 giây**.
5. Kiểm bằng máy: `./gradlew :permahearts:build` (`GolemGameTest`) và `runClientGameTest` (ảnh `70-blade-hit-knockback`, `71-guardians-first-person`, `72-guardians-third-person`, `73-guardians-fight`).

## P-021: Warrior Angel và tính kill cho chủ (D62, D63)

(Phần hiện % tốc đánh đã bỏ: bảng chỉ số giữ nguyên +0,16.)
1. **Warrior Angel** (chiêu thứ 11, `/technique set @s angel 20`): bấm một lần, triệu hồi thiên thần dáng Vex **màu vàng trắng** quanh bạn. Số lượng: 2 (cấp 1-4), 3 (5-9), 4 (10-14), 5 (15-19), 6 (cấp 20). Trang chi tiết có Angels, Health, Damage, Lasts, Cooldown.
2. Thiên thần bay xuyên tường, đi theo bạn (trong khoảng 6 block), tự lao vào đánh quái quanh bạn hoặc quái đánh bạn / bạn đánh. **Không** đánh người chơi và bạn; bạn đánh thì không gây sát thương. Khi lao vào có tư thế tấn công (mắt sáng).
3. Máu: bằng Vex ở cấp 1, gấp đôi ở cấp 20; sát thương: nửa Vex ở cấp 1, gấp đôi ở cấp 20. Tồn tại **60 giây** rồi biến mất kèm hạt vàng; bạn chết/thoát game hoặc dùng lại chiêu thì biến mất (dùng lại thì thay bộ mới). Hồi chiêu **120 giây**. Không rơi đồ, không cho XP.
4. **Tính kill**: để Violet Guardian hoặc Warrior Angel giết một con quái địch: tiến độ HeartXP / 100 quái tăng và nhiệm vụ giết của Crimson Master (nếu đang làm) cũng tăng, giống sói đã thuần.
5. Kiểm bằng máy: `./gradlew :permahearts:build` (`AngelGameTest`) và `runClientGameTest` (ảnh `80-angels-third-person`).

## P-022: Aqua Marine (D64)

1. **Aqua Marine** (chiêu thứ 12, `/technique set @s aqua 20`): bấm một lần, không gồng. Trang chi tiết có Lasts (30 s cấp 1 → 70 s cấp 20), Swim speed và Sprint swim (10 → 20 và 24 blocks/s, D69), Underwater mining (×1 → ×2 Aqua Affinity), Cooldown 120 s.
2. **Thở**: nhảy xuống nước sâu rồi dùng chiêu, bong bóng đầy lại và **không tụt** cho tới hết giờ; hết giờ thì bong bóng tụt lại như thường. Góc phải dưới hiện "Aqua N s" đếm ngược.
3. **Bơi**: chạy bơi (Ctrl) dưới nước. Cấp 1 nhanh bằng Dolphin's Grace (so bằng cách uống thuốc cá heo ở creative), cấp 20 bơi **1,2 ô/tick khi chạy bơi (Ctrl)** và **1,0 ô/tick khi không chạy** (nhanh hơn hẳn cá heo, 0,5).
3b. **Tầm nhìn (P-023, D69)**: dưới nước lúc chiêu chạy, nhìn **rõ ngay** (không mờ dần 30 s) và **sáng** như Night Vision, kể cả ban đêm; lên bờ hoặc hết giờ thì về bình thường (trên bờ không sáng).
4. **Đào**: đào đá dưới nước, cấp 1 như có Aqua Affinity (đội mũ), cấp 20 nhanh gấp đôi. Hết giờ thì chậm lại như thường. Chưa chạm đất (bơi lơ lửng) vẫn bị phạt như vanilla.
5. Hạt bong bóng nhẹ quanh người; cầm linh khí thì có bong bóng và nước bắn ở tay. Chết hoặc thoát game thì chiêu tắt ngay.
6. Kiểm bằng máy: `./gradlew :permahearts:build` (`AquaGameTest`: đo tốc độ bơi cấp 1 = cá heo, cấp 20 = 1,2 sprint / 1,0 thường, đo cả giữa nước và sát đáy) và `runClientGameTest` (ảnh `90-aqua-underwater-first-person`, `91-aqua-underwater-third-person`, `92-aqua-night-view-without-buff`, `93-aqua-night-view-with-buff`).


## P-024: Illusion (D71)

1. **Illusion** (chiêu thứ 13, `/technique set @s illusion 20`): bấm một lần. Cấp 1 có 1 bản sao, cấp 10 có 2, cấp 20 có 3. Mỗi bản sao có 20% máu và sát thương của bạn ở cấp 1, tới 40% ở cấp 20 (giáp cũng theo tỉ lệ đó).
2. **Hình dạng**: bản sao trông như bạn: cùng skin, cùng giáp, cầm cùng vũ khí/khiên, có cả tên trên đầu. Khi bấm chiêu bạn bị dịch chuyển ngẫu nhiên vào một trong các vị trí (xáo trộn), kèm hạt khói và tiếng chuông.
3. **Hành vi**: đi theo bạn, thỉnh thoảng nhảy, ngồi xuống, nhìn quanh; xa hơn 24 block thì dịch chuyển về cạnh bạn. Bản sao chạy nhanh hơn đi bộ một chút, có lúc lùi lại hoặc bắn cung nếu cầm cung.
4. **Mục tiêu**: bản sao đánh quái địch quanh bạn, **kể cả creeper**. Khi tắt PvP: ai đánh bạn, hoặc người bạn đánh, trở thành mục tiêu và bản sao gây sát thương cho họ (dù PvP tắt). Bạn và bản sao khác không bị bản sao đánh; bạn đánh bản sao thì không gây sát thương.
5. **Kill**: quái chết do bản sao giết thì tính cho bạn (HeartXP, nhiệm vụ giết). Người chơi bị bản sao giết **không** tính là kill của Death Match.
6. Hết giờ, bạn chết/thoát game hoặc dùng lại chiêu thì bản sao biến mất kèm khói. Không rơi đồ, không cho XP.
7. Kiểm bằng máy: `./gradlew :permahearts:build` (`IllusionGameTest`, `TechniqueRulesTest.illusionNumbers`) và `runClientGameTest` (ảnh `81-illusion-third-person`).

## P-025: Illusion làm việc (D73)

Để đồ nghề ở **ô hotbar đầu tiên** (D85, không phải đồ đang cầm) rồi bấm Illusion: bản sao khoá việc theo món đó. Ô đầu trống thì các bản sao chỉ đánh nhau. Đồ của bạn không bị lấy (trừ phần thức ăn chia cho việc breed) (đổi đồ sau không đổi việc). Việc nằm trong vòng tròn quanh chỗ bấm (bán kính 8 block ở cấp 1, 16 ở cấp 20). Mỗi bản sao mang tối đa 9 ô đồ, đem về rương/thùng gần chỗ bấm khi đầy, khi hết việc, hoặc 3 giây trước khi hết giờ. Không có rương thì thả đồ xuống chân bạn. Việc không cho XP và không hao đồ nghề. Cầm tay không hoặc kiếm thì vẫn chỉ đánh nhau.

1. **Cuốc (farm)**: đặt vài ô lúa chín gần nước, rương bên cạnh, bấm. Bản sao gặt, trồng lại ngay, trồng hạt lên ô đất cày sẵn còn trống. Nó KHÔNG BAO GIỜ cuốc đất mới (D84). Hết việc thì mang hết hạt và nông sản đến rương; không có rương thì thả xuống đất (vài giây sau khi hết việc, không đợi hết giờ).
2. **Cần câu (fish)**: đứng gần ao nước từ 2x2 ô trở lên, bấm. Bản sao ra bờ, vung cần, chờ 5-30 giây (Lure rút ngắn, Luck of the Sea tăng đồ hiếm), cá vào rương.
3. **Rìu (chop)**: gần cây tự nhiên. Chặt từ gốc lên, trồng lại cây con ở gốc; công trình gỗ của bạn (khúc gỗ chạm lá bạn đặt) thì không đụng. Cây cao 6-7 khúc phải đốn hết trong khoảng 12 giây ở cấp 1. Lá tự nhiên chắn đường tới khúc gỗ thì bản sao đập lá đó (rơi cây con, que, táo), không phá cả tán; lá bạn đặt thì giữ nguyên. Không có rương thì gỗ rơi xuống đất ở chân bạn.
4. **Kéo (shear)**: gần cừu còn lông. Bản sao cắt, len vào rương.
5. **Xô (milk)**: để xô rỗng trong rương, có bò gần đó. Xô rỗng thành xô sữa trong rương. Không có xô trong rương thì không làm gì (không tạo xô).
6. **Lúa mì / cà rốt / hạt (breed)**: cầm đồ ăn, chia đều giữa bạn và các bản sao. Bản sao đút bò/cừu/lợn trưởng thành, hai con cùng loài một lúc. Thức ăn trong rương dùng trước. Con non ra đời không cho XP.
7. **Đánh nhau**: quái đánh bạn thì bản sao đánh trả rồi quay lại làm việc. Quái thù địch (zombie, skeleton...) giờ chọn ngẫu nhiên giữa bạn và các bản sao (sửa lỗi "quái không để ý bản sao").
8. Bản sao biến mất (hết giờ, chết) thì thả đồ đang mang.
9. Kiểm bằng máy: `./gradlew :permahearts:build` (`IllusionWorkGameTest`, `TechniqueRulesTest`).

## P-026: Quantum Leap (đổi tên) và Cosmic Warping (D74, D75)

**Quantum Leap** (chiêu cũ Thousand Mile Step, id vẫn là `teleport`): tên mới "Quantum Leap" trong tab chiêu, trang chi tiết và tên pháp khí; icon, hạt, vệt bay và màu tab đổi sang **xanh ngọc (turquoise)**. Cách chơi y như cũ (gồng 3 giây, đi tới ~150 block cấp 1).

**Cosmic Warping** (chiêu thứ 13, `/technique set @s warp 20` hoặc học trong tab chiêu, gán vào ô, bấm R chọn ô đó):
1. **Giữ chuột phải đủ 1 giây** (có thanh nạp) rồi thả, nhìn xuống mặt trên của một khối trong tầm ~6 block (có 2 khối trống phía trên): mọc một **cổng** (hình bầu dục ~1 rộng × 1,5 cao, viền tím, bên trong xoáy chậm màu riêng của bạn, có hạt). Đặt cổng **không** tốn hồi chiêu. Cấp 1 tối đa **2 cổng**, cấp 20 tối đa **20**; đặt thêm: dòng chữ **đỏ** "Portal limit reached (N). Remove one in the warp menu.".
1b. **Bấm chuột phải nhanh** (chưa đủ 1 giây): không mọc cổng, hiện dòng "Hold right click for 1 s to open a portal" (D79).
2. Cổng **không phá được**: không bằng tay hay công cụ (kể cả creative), không bằng TNT/Creeper, piston, nước/dung nham, quái. Mất chiêu / reset chiêu vẫn không mất cổng. Người khác thấy cổng của bạn nhưng không dùng được.
3. **Chuột trái** (ở bất cứ đâu, kể cả khác chiều không gian): mở **menu warp**: mục **Portals** (cũ nhất lên trước, mỗi dòng 2 hàng: tên, rồi "x, y, z" + chiều không gian nếu không phải Overworld, có nút **X** bên phải) rồi mục **Players** (người chơi khác đang online). Tiêu đề hiện "Portals N / max" và thời gian hồi còn lại. Lăn chuột để cuộn.
4. Bấm một dòng: dịch chuyển tới cổng (hoặc tới người đó), có tiếng Enderman + hạt portal ở hai đầu, menu đóng. **Hồi chiêu chỉ bắt đầu sau khi dịch chuyển**: 90 giây cấp 1, giảm dần còn 40 giây cấp 20 (D81). Đang hồi chiêu vẫn mở được menu, nhưng bấm dòng thì bị từ chối (chữ đỏ báo giây còn lại). Thanh chiêu hiện đếm ngược đúng.
5. Bấm **X**: cổng biến mất ngay và khỏi danh sách (chỉ chủ cổng xoá được). Cổng ở khu vực chưa tải: danh sách mất ngay, thân cổng biến mất khi tải lại khu đó.
6. **Tên cổng tự đặt**: công trình gần nhất trong ~64 block (Village, Ruined Portal...), không có thì tên biome (ví dụ "Cherry Grove"); trùng thì thêm " 2", " 3".
7. Màu bên trong cổng lấy từ UUID người đặt (hai người chơi khác nhau thường khác màu, không bao giờ tím).
8. Thoát game vào lại: cổng và danh sách vẫn còn.

Kiểm bằng máy: `./gradlew :permahearts:build` (JUnit `WarpRulesTest`, game test `WarpGameTest`: đặt 2 cổng + từ chối cổng thứ 3, không phá được, xoá, dịch chuyển + hồi chiêu, tới người chơi khác, tên) và `runClientGameTest` (`WarpClientTest`: ảnh `90-warp-menu`, `91-warp-portal`).

## P-027, P-030: Hộp thưởng (D76, D89, D90)

Mỗi dòng nhiệm vụ trả một **hộp thưởng**, loại hộp **bốc ngẫu nhiên lúc nhận** (không hiện trước). Thường: bạc 40% / vàng 30% / kim cương 20% / ngọc lục bảo 10%; hạ thủ lĩnh và Blood Moon: 20 / 30 / 30 / 20. Hộp bạc 3 thẻ, vàng 4, kim cương 5 (chọn 1); **hộp ngọc lục bảo 6 thẻ, chọn 2**.

1. Lấy hộp bằng lệnh: `/give @s permahearts:gold_reward_box` (cũng có `silver_reward_box`, `diamond_reward_box`, `emerald_reward_box`, và trong tab Creative "Food & Drinks").
2. Bấm chuột phải khi cầm hộp: mở màn hình các thẻ. Mỗi thẻ là một bình linh dược hoặc một món đồ (sắt, vàng, kim cương, ngọc lục bảo, redstone, lapis, ngọc ender, thuốc súng). Thẻ đồ có 20% là thẻ đặc biệt (hộp vàng và kim cương): 4 táo vàng phép hoặc 32 ngọc lục bảo; hộp kim cương còn có 1 thỏi netherite. Hộp bạc không có thẻ đặc biệt.
3. Bấm một thẻ: thẻ có viền vàng. Nút **Claim** chỉ sáng khi chọn đủ số thẻ (1; hộp ngọc lục bảo 2). Di chuột vào thẻ để xem tên món.
4. Bấm **Claim**: nhận món, hộp biến mất, có tiếng lên cấp. Bấm **Close** (hoặc Esc): hộp còn nguyên, và mở lại vẫn ra đúng các thẻ cũ (thẻ được ra lúc mở lần đầu).
5. Hoàn thành một dòng nhiệm vụ rồi nói chuyện với Wanderer: ô thưởng chỉ hiện hộp xám "?" ("Random Reward Box"). Lấy ra khỏi ô thì nhận hộp thật (loại ngẫu nhiên). Nhiệm vụ lưu từ bản cũ cũng vậy.
6. **Hộp ngọc lục bảo** (xanh lá, 6 thẻ): dòng chữ trên cùng ghi "Pick 2 rewards", chọn 1 thẻ thì ghi "1 left". Thẻ đồ có 25% là đặc biệt: Totem of Undying, Bottle of Enlightenment, sách Mending, Sharpness V, Protection V (thẻ sách ghi tên phép, vd "Sharpness V"). Chọn 2 thẻ khác nhau, Claim: nhận cả hai. Đóng khi mới chọn 1 thẻ: không lấy gì, hộp còn nguyên.
7. **Bottle of Enlightenment**: `/give @s permahearts:enlightenment_bottle`, cầm uống (giữ chuột phải ~1,6 giây): +16 level, tiêu 1 bình.
8. Lữ Khách giờ tới sau **5 ngày** (lần đầu và sau mỗi lần rời đi). Nhiệm vụ mới có thêm thịt lợn (48), giết Blaze / Wither Skeleton / Phantom (8 con), phù thủy còn 6 con; không có 2 dòng cùng một con.
9. Kiểm bằng máy: `./gradlew :permahearts:build` (`RewardBoxTest`, `RewardBoxGameTest`, `QuestLogicTest`) và `runClientGameTest` (ảnh `90-reward-box-*`, `92-quest-box-rewards`, `93-reward-box-diamond-*`, `101-emerald-box`, `102-emerald-box-picked`, `103-emerald-box-two-picked`).

## P-028: bảng chiêu thức mới (D83)

Mở Cultivation, tab Techniques.
1. Mỗi dòng chiêu có nút **i** (mở trang chi tiết, đây là cách duy nhất) và thanh cấp độ xanh lá dưới tên (rê chuột vào thanh: "Lv N / 20"). Không còn chữ "Lv N".
2. **Chọn chiêu trước**: bấm vào dòng một chiêu đã học: dòng viền vàng, các ô có thể đặt cũng viền vàng, ô emerald thành nút Cancel đỏ nhạt. Bấm ô viền vàng để đặt. Bấm lại dòng đó hoặc Cancel để bỏ chọn. Dòng chiêu chưa học: không làm gì (tooltip "Learn it first: click i").
3. **Chọn ô trước**: bấm một ô, rồi bấm vào tên chiêu (không cần dấu cộng) là đặt luôn. Dấu cộng vẫn dùng được. Chuột phải vào ô vẫn xoá ô đó.
4. Trang chi tiết: chữ "Lv N / 20" màu xanh lá có viền tối, thanh xanh lá bên dưới.

## P-029: Thunder Strike (D86)

1. Cho mình chiêu: nâng bằng tab Techniques, đặt vào một ô, bấm phím chiêu để vào chế độ chiêu.
2. Nhắm tâm vào một con mob hoặc bức tường cách 10 ô, bấm chuột trái (hoặc phải): vòng tia lửa vàng trắng hiện quanh điểm đó 1 giây, rồi sét đánh, nổ cỡ creeper, mob bị thương, khối vỡ. Không có lửa.
3. Đứng sát điểm nhắm: mình không bị thương. Có Violet Guardian thì nó cũng không bị thương.
4. Nhắm lên trời hoặc ra xa: vẫn đánh, vào mặt đất dưới điểm xa 128 ô.
5. Đứng gần một con heo khi sét đánh: heo thành zombified piglin. Dân làng thành witch, creeper thành creeper tích điện.
6. `/gamerule mobGriefing false`: sét vẫn gây sát thương nhưng không phá khối.
7. Hồi chiêu 24 giây tính từ lúc bấm.
8. Kiểm bằng máy: `ThunderRulesTest`, `ThunderGameTest`, `ThunderClientTest` (ảnh `99-thunder-warning`, `100-thunder-strike`).

## /testmode (P-031, D92)

1. Là OP, bật luật chơi rồi gõ `/testmode full`: mũ, áo, quần, giày netherite mặc sẵn, kiếm/cúp/rìu/xẻng/cuốc netherite trong túi, mỗi món có 2 phù phép tối đa (xem tooltip); 50 tim (100 máu); `/hearts` thấy mỗi linh dược +10. Tin "Test mode on. /testmode reset to go back.".
2. Gõ `/testmode full` lần nữa: không ghi đè bản đã lưu.
3. Nhặt thêm vài món, rồi `/testmode reset`: tim, linh dược, túi đồ và giáp về đúng như trước khi gõ full; đồ nhặt thêm mất. Tin "Test mode off.". Gõ reset lần nữa: "Test mode is not on.".
4. Người không phải OP gõ `/testmode` không được.

## God Slayer (P-032, D96, D97)

1. Là OP, gõ `/boss summon` (hoặc `/boss summon <người>`): chữ lớn "God Slayer" giữa màn hình, tin chat tên người bị săn, tiếng trống, 10 giây cuối có đếm ngược. Gõ lại lúc đó: báo đang bận.
2. Sau 60 giây boss hiện ra cách người bị săn 15 đến 20 ô, có khói đen. Cao 5 ô (gấp 2,5 người chơi), da đỏ, tóc trắng dài, sừng cong lớn, dạng chân hai tay mở, hai katana lớn dựng đứng ở hai bên; chạy thì chân đung đưa, chém thì tay phải giơ lên rồi bổ xuống. Thanh máu đỏ chia 10 phần, tên "God Slayer — Lv 1", màn hình tối đi. Ra xa hơn 96 ô thì thanh biến mất.
3. Nó chạy rất nhanh và chém khi tới gần 3,5 ô: ở Lv 1 trừ 30 máu mỗi giây một nhát (độ khó Normal). Bạn đứng trên cao hoặc bay: nó bay lên bắt. Xuống nước: nó bơi nhanh hơn cá heo rất nhiều. Đứng sau tường: nó đập tường (khi `mobGriefing` bật), tắt thì nó dịch chuyển tới cạnh bạn. Cách xa hơn 48 ô cũng dịch chuyển.
4. Đánh nó bằng kiếm hoặc chiêu thức: mỗi nhát mất tối đa 40 máu. Lửa, nổ, quái khác, dung nham, rơi không làm nó mất máu. Frostbite và Repulse chỉ còn 10%. Không dắt, không cưỡi, không đẩy được.
5. Đi vào Nether lúc nó đang săn: nó theo sang. Người bị săn chết: nó quay sang người khác (tin chat), hết người thì nó thắng.
6. Hạ được nó: +100 level kinh nghiệm, 5 hộp thưởng, tin chat; lần sau nó mạnh hơn (Lv 2: 600 máu). Hết 10 phút mà còn sống: nó trêu rồi biến mất trong khói, cấp không đổi. `/boss giveup`: boss biến mất.
7. `/boss level 3` đặt cấp (1 đến 10), `/boss level` xem cấp. Người không phải OP gõ `/boss` không được.
8. Tự xuất hiện: từ ngày 100, mỗi lần Crimson Master đến lần thứ 4, 8, 12. Có người từ 30 tim thì săn người đó, không ai đủ thì tin chat "Weaklings. Come back when your hearts can bear my blade.".
9. Tắt game giữa lúc săn: mở lại không còn boss, không còn đồng hồ.
10. Kiểm bằng máy: `GodSlayerRulesTest`, `GodSlayerGameTest`, `GodSlayerClientTest` (ảnh `90-god-slayer-front`).
10. Heaven Blast choáng (P-034): vây vài Husk, dùng Hất tung. Chúng bị đẩy ra rồi đứng đơ khoảng 1 giây ở Lv 1 (lâu hơn ở level cao, tới 2,9 s), có hạt sao quay trên đầu, không đi, không đánh; hết giờ thì lại đuổi bạn. Bảng chiêu có dòng "Stun". Dùng lên boss: chỉ choáng chừng 0,1 giây.
