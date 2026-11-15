# Chiêu thức — số liệu từng level (D43–D56, D72)

Tên trong game (D56): Lửa = Fireball, Băng = Frozen Breath, Hồi máu = Healing Domain (D80, trước là Healing Light), Xuyên tường = Shadow Walk, Hất tung = Heaven Blast, Hiến Tế = Undying Jutsu, Bay = Cloud Travel, Huyết Kiếm = Life Steal Blade.

Sinh từ `technique/TechniqueRules.java`: sửa số liệu ở đó rồi tạo lại bảng này. Sát thương tính bằng máu (2 = 1 tim), "r" là bán kính tính bằng block.

**Hồi chiêu** (không đổi theo level, D80): Lửa 16 giây · Băng 18 giây · Hồi máu 40 giây · Xuyên tường 60 giây · Hất tung 20 giây · Hiến Tế 45 giây · Bay 80 giây · Huyết Kiếm 12 giây (sau mỗi lần đánh trúng) · Quantum Leap 40 giây · Violet Guardian 120 giây · Warrior Angel 120 giây · Aqua Marine 100 giây · Illusion 110 giây · Cosmic Warping 90 giây ở Lv 1 giảm đều còn 40 giây ở Lv 20 (D81) · Thunder Strike 24 giây (D86). Huyết Kiếm: sát thương = cơ bản + cộng thêm của chiêu + linh dược, hút máu bằng lượng máu trừ được.

**Cố định:** Băng va chạm 2, vùng băng tồn tại 8 giây, chậm còn kéo dài 4 giây sau khi ra khỏi vùng (chậm chạy và chậm đánh cùng mức) · Hồi máu: vòng bán kính 4, đứng yên, 10 giây, hồi cho mọi sinh vật trong vòng · Lửa cháy 1 mỗi giây ở Lv 1, +0,2 mỗi level (D72; D45 giảm nửa sau Lv 10). Warrior Angel (D72): máu 16 → 30 (tuyến tính), sát thương 4 +1 mỗi level (Lv 20: 23).

**Mở ô chiêu** bằng ngọc lục bảo: ô 1 = 64 · ô 2 = +128 · ô 3 = +256 · ô 4 = +512 (tổng 960, D77). **Reset** một chiêu trả lại 1/2 số XP đã bỏ vào.

| Lv | XP nâng lên Lv này | XP tổng | Lửa: tầm / va chạm / bãi lửa / cháy | Băng: tầm / vùng / chậm đi / chậm đánh | Hồi máu: tim trong 10 giây | Xuyên tường: thời gian / nhìn xuyên | Hất tung: bán kính / lực / sát thương | Hiến Tế: máu mất / bất tử | Bay: thời gian / tốc độ (so với Creative) | Huyết Kiếm: sát thương cộng thêm |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 6 | 6 | 24 block / 4 / r 3, 4 s / 1 mỗi giây | 24 block / r 3 / -40% / -40% | 12 | 10 s / 8 block | 4 / 1.5 / 5 | 80% / 8 s | 15 s / 80% Creative | +5 |
| 2 | 7 | 13 | 26 block / 5 / r 3.2, 4.2 s / 1.2 mỗi giây | 26 block / r 3.2 / -43% / -43% | 13.9 | 10.5 s / 10 block | 4.2 / 1.75 / 5.5 | 77% / 8.5 s | 16.32 s / 86% Creative | +5.5 |
| 3 | 7 | 20 | 28 block / 6 / r 3.4, 4.4 s / 1.4 mỗi giây | 28 block / r 3.4 / -46% / -46% | 15.8 | 11 s / 12 block | 4.4 / 2 / 6 | 73% / 9 s | 17.63 s / 93% Creative | +6 |
| 4 | 7 | 27 | 30 block / 7 / r 3.6, 4.6 s / 1.6 mỗi giây | 30 block / r 3.6 / -49% / -49% | 17.7 | 11.5 s / 14 block | 4.6 / 2.25 / 6.5 | 70% / 9.5 s | 18.95 s / 99% Creative | +6.5 |
| 5 | 8 | 35 | 32 block / 8 / r 3.8, 4.8 s / 1.8 mỗi giây | 32 block / r 3.8 / -52% / -52% | 19.6 | 12 s / 16 block | 4.8 / 2.5 / 7 | 67% / 10 s | 20.26 s / 105% Creative | +7 |
| 6 | 8 | 43 | 34 block / 9 / r 4, 5 s / 2 mỗi giây | 34 block / r 4 / -55% / -55% | 21.5 | 12.5 s / 19 block | 5 / 2.75 / 7.5 | 63% / 10.5 s | 21.58 s / 112% Creative | +7.5 |
| 7 | 9 | 52 | 36 block / 10 / r 4.2, 5.2 s / 2.2 mỗi giây | 36 block / r 4.2 / -58% / -58% | 23.4 | 13 s / 21 block | 5.2 / 3 / 8 | 60% / 11 s | 22.89 s / 118% Creative | +8 |
| 8 | 9 | 61 | 38 block / 11 / r 4.4, 5.4 s / 2.4 mỗi giây | 38 block / r 4.4 / -61% / -61% | 25.3 | 13.5 s / 23 block | 5.4 / 3.25 / 8.5 | 57% / 11.5 s | 24.21 s / 124% Creative | +8.5 |
| 9 | 9 | 70 | 40 block / 12 / r 4.6, 5.6 s / 2.6 mỗi giây | 40 block / r 4.6 / -64% / -64% | 27.2 | 14 s / 25 block | 5.6 / 3.5 / 9 | 53% / 12 s | 25.53 s / 131% Creative | +9 |
| 10 | 10 | 80 | 42 block / 13 / r 4.8, 5.8 s / 2.8 mỗi giây | 42 block / r 4.8 / -67% / -67% | 29.1 | 14.5 s / 27 block | 5.8 / 3.75 / 9.5 | 50% / 12.5 s | 26.84 s / 137% Creative | +9.5 |
| 11 | 10 | 90 | 43 block / 13.5 / r 4.9, 5.9 s / 2.9 mỗi giây | 43 block / r 4.9 / -69% / -69% | 30.9 | 14.75 s / 29 block | 5.9 / 3.88 / 9.75 | 48% / 13 s | 28.16 s / 143% Creative | +10 |
| 12 | 11 | 101 | 44 block / 14 / r 5, 6 s / 3 mỗi giây | 44 block / r 5 / -72% / -72% | 32.8 | 15 s / 31 block | 6 / 4 / 10 | 46% / 13.5 s | 29.47 s / 149% Creative | +10.5 |
| 13 | 11 | 112 | 45 block / 14.5 / r 5.1, 6.1 s / 3.1 mỗi giây | 45 block / r 5.1 / -75% / -75% | 34.7 | 15.25 s / 33 block | 6.1 / 4.12 / 10.25 | 44% / 14 s | 30.79 s / 156% Creative | +11 |
| 14 | 11 | 123 | 46 block / 15 / r 5.2, 6.2 s / 3.2 mỗi giây | 46 block / r 5.2 / -78% / -78% | 36.6 | 15.5 s / 35 block | 6.2 / 4.25 / 10.5 | 42% / 14.5 s | 32.11 s / 162% Creative | +11.5 |
| 15 | 12 | 135 | 47 block / 15.5 / r 5.3, 6.3 s / 3.3 mỗi giây | 47 block / r 5.3 / -81% / -81% | 38.5 | 15.75 s / 37 block | 6.3 / 4.38 / 10.75 | 40% / 15 s | 33.42 s / 168% Creative | +12 |
| 16 | 12 | 147 | 48 block / 16 / r 5.4, 6.4 s / 3.4 mỗi giây | 48 block / r 5.4 / -84% / -84% | 40.4 | 16 s / 40 block | 6.4 / 4.5 / 11 | 38% / 15.5 s | 34.74 s / 175% Creative | +12.5 |
| 17 | 13 | 160 | 49 block / 16.5 / r 5.5, 6.5 s / 3.5 mỗi giây | 49 block / r 5.5 / -87% / -87% | 42.3 | 16.25 s / 42 block | 6.5 / 4.62 / 11.25 | 36% / 16 s | 36.05 s / 181% Creative | +13 |
| 18 | 13 | 173 | 50 block / 17 / r 5.6, 6.6 s / 3.6 mỗi giây | 50 block / r 5.6 / -90% / -90% | 44.2 | 16.5 s / 44 block | 6.6 / 4.75 / 11.5 | 34% / 16.5 s | 37.37 s / 187% Creative | +13.5 |
| 19 | 13 | 186 | 51 block / 17.5 / r 5.7, 6.7 s / 3.7 mỗi giây | 51 block / r 5.7 / -93% / -93% | 46.1 | 16.75 s / 46 block | 6.7 / 4.88 / 11.75 | 32% / 17 s | 38.68 s / 194% Creative | +14 |
| 20 | 14 | 200 | 52 block / 18 / r 5.8, 6.8 s / 3.8 mỗi giây | 52 block / r 5.8 / -96% / -96% | 48 | 17 s / 48 block | 6.8 / 5 / 12 | 30% / 17.5 s | 40 s / 200% Creative | +14.5 |

**Thunder Strike (D86):** nhắm tâm màn hình, bấm chuột trái hoặc phải (tầm 128 ô). Vùng quanh điểm đó sáng 1 giây rồi sét đánh, nổ cỡ creeper. Sát thương 14 ở Lv 1, +2 mỗi level (Lv 10: 32, Lv 20: 52), giảm còn một nửa ở rìa. Vùng phá 100% creeper ở Lv 1, +5% mỗi level (195% ở Lv 20). Hồi chiêu 24 giây. Nhắm lên trời: sét đánh xuống mặt đất dưới điểm xa nhất. Heo, dân làng, creeper, mooshroom, Copper Golem trong vùng nổ biến đổi như bị sét thật. Người dùng và vật triệu hồi của mình không bị thương. Tôn trọng mobGriefing.
