# Chiêu thức — số liệu từng level (D43–D47)

Sinh từ `technique/TechniqueRules.java`: sửa số liệu ở đó rồi tạo lại bảng này. Sát thương tính bằng máu (2 = 1 tim), "r" là bán kính tính bằng block.

**Hồi chiêu** (không đổi theo level): Lửa 16 giây · Băng 24 giây · Hồi máu 60 giây · Xuyên tường 90 giây · Hất tung 20 giây · Hiến Tế 45 giây.

**Cố định:** Băng va chạm 2, vùng băng tồn tại 6 giây, chậm còn kéo dài 3 giây sau khi ra khỏi vùng · Hồi máu: vòng bán kính 4, đứng yên, 10 giây, hồi cho mọi sinh vật trong vòng · Lửa từ Lv 6 cháy mạnh gấp đôi (màu lửa giữ nguyên).

**Mở ô chiêu** bằng ngọc lục bảo: ô 1 = 64 · ô 2 = +256 · ô 3 = +512 · ô 4 = +1024 (tổng 1856). **Reset** một chiêu trả lại 1/2 số XP đã bỏ vào.

| Lv | XP nâng lên Lv này | XP tổng | Lửa: tầm / va chạm / bãi lửa / cháy | Băng: tầm / vùng / chậm đi / chậm đánh | Hồi máu: tim trong 10 giây | Xuyên tường | Hất tung: bán kính / lực / sát thương | Hiến Tế: máu mất / bất tử |
|---|---|---|---|---|---|---|---|---|
| 1 | 6 | 6 | 16 block / 4 / r 2, 4 s / 1 mỗi giây | 16 block / r 3 / -20% / -15% | 10 | 10 s | 4 / 1 / 2 | 80% / 5 s |
| 2 | 7 | 13 | 18 block / 5 / r 2.1, 4.2 s / 1 mỗi giây | 18 block / r 3.2 / -24% / -19% | 12 | 10.5 s | 4.2 / 1.15 / 2.5 | 77% / 5.33 s |
| 3 | 7 | 20 | 20 block / 6 / r 2.2, 4.4 s / 1 mỗi giây | 20 block / r 3.4 / -28% / -23% | 14 | 11 s | 4.4 / 1.3 / 3 | 73% / 5.67 s |
| 4 | 7 | 27 | 22 block / 7 / r 2.3, 4.6 s / 1 mỗi giây | 22 block / r 3.6 / -32% / -27% | 16 | 11.5 s | 4.6 / 1.45 / 3.5 | 70% / 6 s |
| 5 | 8 | 35 | 24 block / 8 / r 2.4, 4.8 s / 1 mỗi giây | 24 block / r 3.8 / -36% / -31% | 18 | 12 s | 4.8 / 1.6 / 4 | 67% / 6.33 s |
| 6 | 8 | 43 | 26 block / 9 / r 2.5, 5 s / 2 mỗi giây | 26 block / r 4 / -40% / -35% | 20 | 12.5 s | 5 / 1.75 / 4.5 | 63% / 6.67 s |
| 7 | 9 | 52 | 28 block / 10 / r 2.6, 5.2 s / 2 mỗi giây | 28 block / r 4.2 / -44% / -39% | 22 | 13 s | 5.2 / 1.9 / 5 | 60% / 7 s |
| 8 | 9 | 61 | 30 block / 11 / r 2.7, 5.4 s / 2 mỗi giây | 30 block / r 4.4 / -48% / -43% | 24 | 13.5 s | 5.4 / 2.05 / 5.5 | 57% / 7.33 s |
| 9 | 9 | 70 | 32 block / 12 / r 2.8, 5.6 s / 2 mỗi giây | 32 block / r 4.6 / -52% / -47% | 26 | 14 s | 5.6 / 2.2 / 6 | 53% / 7.67 s |
| 10 | 10 | 80 | 34 block / 13 / r 2.9, 5.8 s / 2 mỗi giây | 34 block / r 4.8 / -56% / -51% | 28 | 14.5 s | 5.8 / 2.35 / 6.5 | 50% / 8 s |
| 11 | 10 | 90 | 35 block / 13.5 / r 2.95, 5.9 s / 2 mỗi giây | 35 block / r 4.9 / -59% / -53% | 29 | 14.75 s | 5.9 / 2.42 / 6.75 | 48% / 8.2 s |
| 12 | 11 | 101 | 36 block / 14 / r 3, 6 s / 2 mỗi giây | 36 block / r 5 / -62% / -55% | 30 | 15 s | 6 / 2.5 / 7 | 46% / 8.4 s |
| 13 | 11 | 112 | 37 block / 14.5 / r 3.05, 6.1 s / 2 mỗi giây | 37 block / r 5.1 / -65% / -57% | 31 | 15.25 s | 6.1 / 2.58 / 7.25 | 44% / 8.6 s |
| 14 | 11 | 123 | 38 block / 15 / r 3.1, 6.2 s / 2 mỗi giây | 38 block / r 5.2 / -68% / -59% | 32 | 15.5 s | 6.2 / 2.65 / 7.5 | 42% / 8.8 s |
| 15 | 12 | 135 | 39 block / 15.5 / r 3.15, 6.3 s / 2 mỗi giây | 39 block / r 5.3 / -71% / -61% | 33 | 15.75 s | 6.3 / 2.72 / 7.75 | 40% / 9 s |
| 16 | 12 | 147 | 40 block / 16 / r 3.2, 6.4 s / 2 mỗi giây | 40 block / r 5.4 / -74% / -63% | 34 | 16 s | 6.4 / 2.8 / 8 | 38% / 9.2 s |
| 17 | 13 | 160 | 41 block / 16.5 / r 3.25, 6.5 s / 2 mỗi giây | 41 block / r 5.5 / -77% / -65% | 35 | 16.25 s | 6.5 / 2.87 / 8.25 | 36% / 9.4 s |
| 18 | 13 | 173 | 42 block / 17 / r 3.3, 6.6 s / 2 mỗi giây | 42 block / r 5.6 / -80% / -67% | 36 | 16.5 s | 6.6 / 2.95 / 8.5 | 34% / 9.6 s |
| 19 | 13 | 186 | 43 block / 17.5 / r 3.35, 6.7 s / 2 mỗi giây | 43 block / r 5.7 / -83% / -69% | 37 | 16.75 s | 6.7 / 3.02 / 8.75 | 32% / 9.8 s |
| 20 | 14 | 200 | 44 block / 18 / r 3.4, 6.8 s / 2 mỗi giây | 44 block / r 5.8 / -86% / -71% | 38 | 17 s | 6.8 / 3.1 / 9 | 30% / 10 s |
