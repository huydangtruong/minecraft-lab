# Chơi thử

## Một mình, không đụng save thật

```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
./gradlew :permahearts:runClient
```

Tạo world Survival mới (Creative bật cheat nếu cần lệnh). Kịch bản PermaHearts:

1. `/hearts` → 10 tim, tiến độ 0/100.
2. `/kill` → hồi sinh 9 tim, máu đầy, có tin báo.
3. `/hearts set @s 1` rồi `/kill` → vẫn 1 tim + tin "mức thấp nhất".
4. `/hearts progress @s 99`, `/summon zombie`, giết bằng cung → +1 tim, âm thanh level-up, tiến độ 0.
5. Giết bò → tiến độ không đổi. `/hearts set @s 20` + `/hearts progress @s 99` + giết zombie → vẫn 20 tim, báo "tối đa".
6. Thoát world, vào lại → giữ nguyên.

## Với bạn (Essential)

1. Huy: `scripts/install.sh`, khởi động lại profile "26.3 Fabric Essential", kiểm `logs/latest.log` có `PermaHearts loaded`.
2. Bạn: `scripts/package.sh` → gửi zip, bạn làm theo `INSTALL-vi.txt`.
3. Huy mở world, mời bạn qua Essential; chết thử → mất 1 tim; `/hearts` của mỗi người độc lập.
