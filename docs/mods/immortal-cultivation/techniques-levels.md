# Chiêu thức — số liệu từng level (D43–D56, D72)

Tên trong game (D56): Lửa = Fireball, Băng = Frozen Breath, Hồi máu = Healing Domain (D80, trước là Healing Light), Xuyên tường = Shadow Walk, Hất tung = Heaven Blast, Hiến Tế = Undying Jutsu, Bay = Cloud Travel, Huyết Kiếm = Life Steal Blade.

Sinh từ `technique/TechniqueRules.java`: sửa số liệu ở đó rồi tạo lại bảng này. Sát thương tính bằng máu (2 = 1 tim), "r" là bán kính tính bằng block.

**Hồi chiêu** (không đổi theo level, D80): Lửa 16 giây · Băng 18 giây · Hồi máu 40 giây · Xuyên tường 60 giây · Hất tung 20 giây · Hiến Tế 45 giây · Bay 80 giây · Huyết Kiếm 12 giây (sau mỗi lần đánh trúng) · Quantum Leap 40 giây · Violet Guardian 120 giây · Warrior Angel 120 giây · Aqua Marine 100 giây · Illusion 110 giây · Cosmic Warping 90 giây ở Lv 1 giảm đều còn 40 giây ở Lv 20 (D81) · Thunder Strike 26 giây (D86, D95) · Seism 28 giây (D142, cả hai kiểu) · Crystal Prison 48 giây (D143) · Soul Grasp 120 giây (chỉ tính khi bắt thành công, D134). Huyết Kiếm: sát thương = cơ bản + cộng thêm của chiêu + linh dược, hút máu bằng lượng máu trừ được.

**Choáng (D99):** Hất tung choáng mọi sinh vật bị đẩy (không phải người chơi) 1,0 giây ở Lv 1, +0,1 giây mỗi level (2,9 giây ở Lv 20): đứng yên, không đi, không đánh, không bắn. God Slayer chỉ chịu 10%.

**Cố định:** Băng va chạm 2, vùng băng tồn tại 8 giây, chậm còn kéo dài 4 giây sau khi ra khỏi vùng (chậm chạy và chậm đánh cùng mức) · Hồi máu: vòng bán kính 4, đứng yên, 10 giây, hồi cho mọi sinh vật trong vòng · Lửa (D94): cháy 2 mỗi giây ở Lv 1, tăng đều tới 5 ở Lv 20 (không giảm nửa như D45), áp dụng suốt lúc mục tiêu đang cháy vì chiêu, cả trong bãi lửa lẫn lúc cháy dư; cháy dư 3 giây ở Lv 1, +0,2 giây mỗi level (6,8 giây ở Lv 20); xuống nước thì tắt lửa, hết cháy. Warrior Angel (D72): máu 16 → 30 (tuyến tính), sát thương 4 +1 mỗi level (Lv 20: 23).

**Mở ô chiêu** bằng ngọc lục bảo: ô 1 = 64 · ô 2 = +128 · ô 3 = +256 · ô 4 = +512 (tổng 960, D77). **Reset** một chiêu trả lại 1/2 số XP đã bỏ vào.

| Lv | XP nâng lên Lv này | XP tổng | Lửa: tầm / va chạm / bãi lửa / cháy | Băng: tầm / vùng / chậm đi / chậm đánh | Hồi máu: tim trong 10 giây | Xuyên tường: thời gian / nhìn xuyên | Hất tung: bán kính / lực / sát thương / choáng | Hiến Tế: máu mất / bất tử | Bay: thời gian / tốc độ (so với Creative) | Huyết Kiếm: sát thương cộng thêm |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 6 | 6 | 24 block / 4 / r 3, 4 s / 2 mỗi giây, cháy dư 3 s | 24 block / r 3 / -40% / -40% | 12 | 10 s / 8 block | 4 / 1.5 / 5 / 1.0 s | 80% / 8 s | 15 s / 80% Creative | +5 |
| 2 | 7 | 13 | 26 block / 5 / r 3.2, 4.2 s / 2.16 mỗi giây, cháy dư 3.2 s | 26 block / r 3.2 / -43% / -43% | 13.9 | 10.5 s / 10 block | 4.2 / 1.75 / 5.5 / 1.1 s | 77% / 8.5 s | 16.32 s / 86% Creative | +5.5 |
| 3 | 7 | 20 | 28 block / 6 / r 3.4, 4.4 s / 2.32 mỗi giây, cháy dư 3.4 s | 28 block / r 3.4 / -46% / -46% | 15.8 | 11 s / 12 block | 4.4 / 2 / 6 / 1.2 s | 73% / 9 s | 17.63 s / 93% Creative | +6 |
| 4 | 7 | 27 | 30 block / 7 / r 3.6, 4.6 s / 2.47 mỗi giây, cháy dư 3.6 s | 30 block / r 3.6 / -49% / -49% | 17.7 | 11.5 s / 14 block | 4.6 / 2.25 / 6.5 / 1.3 s | 70% / 9.5 s | 18.95 s / 99% Creative | +6.5 |
| 5 | 8 | 35 | 32 block / 8 / r 3.8, 4.8 s / 2.63 mỗi giây, cháy dư 3.8 s | 32 block / r 3.8 / -52% / -52% | 19.6 | 12 s / 16 block | 4.8 / 2.5 / 7 / 1.4 s | 67% / 10 s | 20.26 s / 105% Creative | +7 |
| 6 | 8 | 43 | 34 block / 9 / r 4, 5 s / 2.79 mỗi giây, cháy dư 4 s | 34 block / r 4 / -55% / -55% | 21.5 | 12.5 s / 19 block | 5 / 2.75 / 7.5 / 1.5 s | 63% / 10.5 s | 21.58 s / 112% Creative | +7.5 |
| 7 | 9 | 52 | 36 block / 10 / r 4.2, 5.2 s / 2.95 mỗi giây, cháy dư 4.2 s | 36 block / r 4.2 / -58% / -58% | 23.4 | 13 s / 21 block | 5.2 / 3 / 8 / 1.6 s | 60% / 11 s | 22.89 s / 118% Creative | +8 |
| 8 | 9 | 61 | 38 block / 11 / r 4.4, 5.4 s / 3.11 mỗi giây, cháy dư 4.4 s | 38 block / r 4.4 / -61% / -61% | 25.3 | 13.5 s / 23 block | 5.4 / 3.25 / 8.5 / 1.7 s | 57% / 11.5 s | 24.21 s / 124% Creative | +8.5 |
| 9 | 9 | 70 | 40 block / 12 / r 4.6, 5.6 s / 3.26 mỗi giây, cháy dư 4.6 s | 40 block / r 4.6 / -64% / -64% | 27.2 | 14 s / 25 block | 5.6 / 3.5 / 9 / 1.8 s | 53% / 12 s | 25.53 s / 131% Creative | +9 |
| 10 | 10 | 80 | 42 block / 13 / r 4.8, 5.8 s / 3.42 mỗi giây, cháy dư 4.8 s | 42 block / r 4.8 / -67% / -67% | 29.1 | 14.5 s / 27 block | 5.8 / 3.75 / 9.5 / 1.9 s | 50% / 12.5 s | 26.84 s / 137% Creative | +9.5 |
| 11 | 10 | 90 | 43 block / 13.5 / r 4.9, 5.9 s / 3.58 mỗi giây, cháy dư 5 s | 43 block / r 4.9 / -69% / -69% | 30.9 | 14.75 s / 29 block | 5.9 / 3.88 / 9.75 / 2.0 s | 48% / 13 s | 28.16 s / 143% Creative | +10 |
| 12 | 11 | 101 | 44 block / 14 / r 5, 6 s / 3.74 mỗi giây, cháy dư 5.2 s | 44 block / r 5 / -72% / -72% | 32.8 | 15 s / 31 block | 6 / 4 / 10 / 2.1 s | 46% / 13.5 s | 29.47 s / 149% Creative | +10.5 |
| 13 | 11 | 112 | 45 block / 14.5 / r 5.1, 6.1 s / 3.89 mỗi giây, cháy dư 5.4 s | 45 block / r 5.1 / -75% / -75% | 34.7 | 15.25 s / 33 block | 6.1 / 4.12 / 10.25 / 2.2 s | 44% / 14 s | 30.79 s / 156% Creative | +11 |
| 14 | 11 | 123 | 46 block / 15 / r 5.2, 6.2 s / 4.05 mỗi giây, cháy dư 5.6 s | 46 block / r 5.2 / -78% / -78% | 36.6 | 15.5 s / 35 block | 6.2 / 4.25 / 10.5 / 2.3 s | 42% / 14.5 s | 32.11 s / 162% Creative | +11.5 |
| 15 | 12 | 135 | 47 block / 15.5 / r 5.3, 6.3 s / 4.21 mỗi giây, cháy dư 5.8 s | 47 block / r 5.3 / -81% / -81% | 38.5 | 15.75 s / 37 block | 6.3 / 4.38 / 10.75 / 2.4 s | 40% / 15 s | 33.42 s / 168% Creative | +12 |
| 16 | 12 | 147 | 48 block / 16 / r 5.4, 6.4 s / 4.37 mỗi giây, cháy dư 6 s | 48 block / r 5.4 / -84% / -84% | 40.4 | 16 s / 40 block | 6.4 / 4.5 / 11 / 2.5 s | 38% / 15.5 s | 34.74 s / 175% Creative | +12.5 |
| 17 | 13 | 160 | 49 block / 16.5 / r 5.5, 6.5 s / 4.53 mỗi giây, cháy dư 6.2 s | 49 block / r 5.5 / -87% / -87% | 42.3 | 16.25 s / 42 block | 6.5 / 4.62 / 11.25 / 2.6 s | 36% / 16 s | 36.05 s / 181% Creative | +13 |
| 18 | 13 | 173 | 50 block / 17 / r 5.6, 6.6 s / 4.68 mỗi giây, cháy dư 6.4 s | 50 block / r 5.6 / -90% / -90% | 44.2 | 16.5 s / 44 block | 6.6 / 4.75 / 11.5 / 2.7 s | 34% / 16.5 s | 37.37 s / 187% Creative | +13.5 |
| 19 | 13 | 186 | 51 block / 17.5 / r 5.7, 6.7 s / 4.84 mỗi giây, cháy dư 6.6 s | 51 block / r 5.7 / -93% / -93% | 46.1 | 16.75 s / 46 block | 6.7 / 4.88 / 11.75 / 2.8 s | 32% / 17 s | 38.68 s / 194% Creative | +14 |
| 20 | 14 | 200 | 52 block / 18 / r 5.8, 6.8 s / 5 mỗi giây, cháy dư 6.8 s | 52 block / r 5.8 / -96% / -96% | 48 | 17 s / 48 block | 6.8 / 5 / 12 / 2.9 s | 30% / 17.5 s | 40 s / 200% Creative | +14.5 |

**Thunder Strike (D86):** nhắm tâm màn hình, bấm chuột trái hoặc phải (tầm 128 ô). Vùng quanh điểm đó sáng 1 giây rồi sét đánh, nổ cỡ creeper. Sát thương 14 ở Lv 1, +2 mỗi level (Lv 10: 32, Lv 20: 52), giảm còn một nửa ở rìa. Vùng phá 100% creeper ở Lv 1, +5% mỗi level (195% ở Lv 20). Hồi chiêu 26 giây (D95). Nhắm lên trời: sét đánh xuống mặt đất dưới điểm xa nhất. Heo, dân làng, creeper, mooshroom, Copper Golem trong vùng nổ biến đổi như bị sét thật. Người dùng và vật triệu hồi của mình không bị thương. Tôn trọng mobGriefing.

**Seism (D142, hệ Đất):** chọn ô chiêu rồi bấm chuột phải. Hồi chiêu 28 giây cho cả hai kiểu, bắt đầu khi chiêu thật sự tung ra.
- **Seismic Impact** (nhả chuột trước 1 giây, hoặc chuột trái): một tảng đá rơi xuống điểm ngắm (tối đa 48 ô, nhắm lên trời thì rơi xuống mặt đất dưới điểm xa nhất). Trong 0,2 giây tảng đá rơi, điểm rơi vẫn bám theo tâm màn hình. Gây `5 + Lv/2` tim (Lv 1: 5, Lv 2: 6, Lv 20: 15) cho quái thù địch (không phải boss) trong bán kính 3 ô. Không phá khối. Người chơi trong 8 ô thấy màn hình rung nhẹ.
- **Seismic Wave** (giữ chuột phải 1 giây, từ Lv 3): ba đợt sóng gai đá ở giây 0, 1, 2 quanh chỗ bạn đứng, bán kính 3, 4, 5 ô. Mỗi đợt đánh mọi sinh vật đang đứng trên mặt đất (trừ bạn; vật triệu hồi, mob đã bắt, người chơi khác đều dính). Sát thương mỗi đợt `min(8, 2 + (Lv-3)/2)` tim (Lv 3: 2, Lv 5: 3, Lv 15 trở lên: 8). Hất lên `5 + (Lv-3)/2` ô (Lv 19-20: 13 ô), đẩy ngang 2 ô. Mục tiêu đứng cao hơn bạn 1 ô thì bớt 1 tim và 1 ô hất mỗi ô cao thêm. Mục tiêu đang bay (vừa bị hất) bị bỏ qua. Boss (God Slayer, thủ lĩnh nhiệm vụ, Wither, Ender Dragon, Warden) chỉ nhận sát thương, không bị hất. Trong 3 giây sóng bạn chậm đi 80%.
- Dưới Lv 3 giữ chuột phải không hiện thanh nạp, nhả ra vẫn ra Impact kèm dòng "Seismic Wave unlocks at Lv 3".

**Crystal Prison (D143, hệ Khống chế):** chọn ô chiêu. Hồi chiêu 48 giây, bắt đầu khi chiêu thật sự tung ra (nhắm trượt thì không tốn).
- **Chuột trái:** nhốt thứ bạn đang ngắm (quái hoặc người chơi, tối đa 64 ô). Ngắm vào khối thì lồng hiện ngay trước khối đó. Ngắm trượt, hoặc ngắm vào sinh vật cao/rộng hơn 3 ô (Ghast...) thì không tung.
- **Chuột phải:** nhốt bạn và sinh vật gần nhất quanh bạn.
- Lồng là hộp rỗng 3x3x3, vỏ tinh thể dày 1 ô trên 6 mặt (54 khối, không có cạnh và góc). Khối bên trong bị xoá (nước cũng mất); khối không phá được (bedrock...) giữ nguyên; rương bị đè thì đồ rơi ra. Tinh thể phát sáng mức 7, quái không sinh ra trong lồng.
- Tối đa 2 sinh vật bị nhốt: người/mục tiêu bạn chọn trước, còn chỗ thì bốc ngẫu nhiên trong số đứng gần. Phần còn lại (và mọi thứ cao/rộng hơn 3 ô) bị đẩy ra ngoài vỏ.
- Quái thù địch bị nhốt (không phải vật triệu hồi, mob đã bắt) bị **Disarmed**: không chọn mục tiêu, không đánh trong `3 + 0,5·(Lv-1)` giây (Lv 1: 3 giây, Lv 20: 12,5 giây). Trong Death Match, đối thủ bị nhốt cũng bị Disarmed (không đánh, không dùng chiêu). Bạn và người chơi khác ngoài Death Match không bị. Enderman trong lồng không dịch chuyển được.
- Lồng đứng `8 + (Lv-1)` giây (Lv 1: 8, Lv 20: 27), hết giờ tinh thể biến mất (lưu trong world nên thoát game vẫn biến mất đúng hạn).
- Tinh thể có 20 mức theo level: độ cứng `0,3 + 2,7·(Lv-1)/19` (kính 0,3 → deepslate 3) và chịu nổ `0,3 + 9,7·(Lv-1)/19` (Lv 20 = 10). Phá bằng tay hoặc cuốc đều được, không rơi đồ.

| Lv | Lồng đứng | Disarmed | Độ cứng | Chịu nổ |
|---|---|---|---|---|
| 1 | 8 giây | 3 giây | 0,3 | 0,3 |
| 10 | 17 giây | 7,5 giây | 1,6 | 4,9 |
| 20 | 27 giây | 12,5 giây | 3 | 10 |

| Lv | Impact (tim) | Wave (tim/đợt) | Hất (ô) |
|---|---|---|---|
| 1 | 5 | - | - |
| 2 | 6 | - | - |
| 3 | 6 | 2 | 5 |
| 5 | 7 | 3 | 6 |
| 7 | 8 | 4 | 7 |
| 10 | 10 | 5 | 8 |
| 15 | 12 | 8 | 11 |
| 20 | 15 | 8 | 13 |

**Soul Grasp (D134–D136):** nhắm một sinh vật trong 16 block, giữ chuột phải để kéo sợi dây; đầy thanh là sinh vật thành của mình (hồi chiêu 120 giây chỉ khi thành công, huỷ giữa chừng không tốn). Thời gian giữ = máu tối đa / 20 giây, từ 1 đến 5 giây. Chuột trái mở danh sách (Follow, Stay, Guard, Call, Release).

| Lv | Bắt được sinh vật máu tối đa | Máu cộng thêm | Số sinh vật cùng lúc |
|---|---|---|---|
| 1 | 20 (10 tim) | +5 | 1 |
| 5 | 40 | +13 | 3 |
| 10 | 65 | +23 | 6 |
| 20 | 115 | +43 | 11 |

Công thức: máu tối đa bắt được = 20 + 5 × (Lv − 1); máu cộng = 5 + 2 × (Lv − 1); số sinh vật = 1 + Lv / 2 (làm tròn xuống). Boss, người chơi, Crimson Master, bản sao Illusion, sinh vật triệu hồi và sinh vật đã có chủ thì không bắt được.

## Nội tại (tab Passives, D148)

Mỗi nội tại 10 cấp, mỗi cấp 5 cấp XP. Không reset.

| Cấp | Reduce cooldown | Increase vision | Hunger sustain (đùi gà) | XP gain bonus | Inventory protect (thêm) |
|---|---|---|---|---|---|
| 1 | 3% | 20% | 2,5 | 3% | Tay chính |
| 2 | 4,5% | 31,1% | 3 | 6% | Tay phụ |
| 3 | 6% | 42,2% | 3,5 | 9% | Mũ |
| 4 | 7,5% | 53,3% | 4 | 12% | Giày |
| 5 | 9% | 64,4% | 4,5 | 15% | Áo |
| 6 | 10,5% | 75,6% | 5 | 18% | Quần |
| 7 | 12% | 86,7% | 5,5 | 21% | 9 ô hotbar |
| 8 | 13,5% | 97,8% | 6 | 24% | Hàng túi 1 |
| 9 | 15% | 108,9% | 6,5 | 27% | Hàng túi 2 |
| 10 | 16,5% | 120% | 7 | 30% | Hàng túi 3 |
