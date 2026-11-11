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
