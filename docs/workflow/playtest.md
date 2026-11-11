# Chơi thử

## Một mình, không đụng save thật

```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
./gradlew :permahearts:runClient
```

Tạo world Survival mới (Creative bật cheat nếu cần lệnh). Kịch bản PermaHearts:

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
