# Chiêu thức — ý tưởng của Huy (2026-11-14)

Tóm tắt lời Huy, chưa phải quyết định cuối. Câu hỏi còn mở và đề xuất nằm ở [P-011](../../plans/P-011-techniques.md).

## Học và nâng chiêu

- Mọi người có **cùng một danh sách chiêu thức**.
- Học/nâng chiêu **tốn kinh nghiệm (XP level)**, giống phù phép đồ. Level chiêu càng cao, mỗi lần nâng càng tốn.
- Mỗi chiêu có trang chi tiết: mô tả, chỉ số hiện tại, nút **Upgrade** và nút **Reset về 0**. Reset chỉ trả lại **1/2** số kinh nghiệm đã bỏ vào chiêu đó.

## Ô chiêu thức

- Tối đa **4 ô** dùng cùng lúc. Mở ô bằng **ngọc lục bảo**, bỏ dần vào như nộp đồ cho Crimson Master:
  - ô 1: 64
  - ô 2: thêm 256
  - ô 3: thêm 512
  - ô 4: thêm 1024
- Tab Techniques: phần trên là các ô chiêu và ô nộp ngọc lục bảo; phần dưới là danh sách chiêu (hàng dọc).
- Gắn chiêu vào ô:
  1. Bấm một ô → viền vàng.
  2. Cuối mỗi dòng chiêu hiện nút ✓, bấm để thay chiêu đang ở ô vàng.
  3. Bấm lại ô vàng để huỷ.
- **Chỉ đổi được khi chiêu đang ở ô đó hết hồi chiêu.** Lý do: tránh dùng hết chiêu rồi đổi sang chiêu mới ngay.

## Dùng chiêu

- Bên phải thanh 1–9, đối xứng với thanh HeartXP bên trái, có một dãy ô:
  - ô **✕** (không dùng chiêu, mặc định);
  - các ô chiêu.
- Bấm phím (Huy nói Tab) thì chuyển vòng: ✕ → chiêu 1 → 2 → 3 → 4 → ✕. Về ✕ thì trên tay hiện lại món đồ đang cầm.
- Đang ở ô chiêu: chuột phải hoặc chuột trái là dùng chiêu, giống cách dùng cung hay kiếm.

## Ba chiêu đầu tiên

- **Lửa**: phóng ra một bãi lửa thiêu đối phương.
  - Giữ chuột phải càng lâu thì bay càng xa, như bắn cung.
  - Nâng cấp tăng: tầm xa tối đa, sát thương va chạm, và (nhẹ hơn) sát thương lửa.
  - Lv 1 cháy như lửa thường; Lv 6 cháy mạnh hơn.
- **Băng**: ném quả cầu băng (cách ném như Lửa).
  - Chạm đất thì tạo vùng; ai trong vùng bị đi chậm và đánh chậm.
  - Mức chậm tăng theo level.
- **Hồi máu**: vùng tròn khoảng 8×8 quanh mình sáng lên và hồi máu.
  - Khởi điểm 10 tim trong 10 giây, nâng cấp thì hồi nhiều hơn.
  - Mạnh nên hồi chiêu khoảng 60 giây.

## Bổ sung (Huy, cùng ngày)

- Hồi chiêu Lửa và Băng **gấp đôi**; màu lửa giữ nguyên. Lv tối đa **20**; số liệu Lv 10 như đề xuất là ổn.
- XP: tới Lv 10 khoảng **80 level**, tới Lv 20 khoảng **200 level** (tôi hiểu là tổng cộng dồn).
- Thêm 2 chiêu: **Xuyên tường** (10 giây đi xuyên mọi thứ từ độ cao đang đứng trở lên, không bị rơi) và **Hất tung** (đẩy văng mọi thứ xung quanh + sát thương nhỏ, thiên về phòng thủ).
- Phím **R**; thích kiểu giữ R ra bảng tròn của Essential (như chat wheel Dota): rê chuột lên chiêu để chọn.
- Hồi máu: hồi cho người chơi và cả sinh vật xung quanh (chó, động vật, kể cả kẻ thù nếu đứng trong vòng); vòng đứng yên.
- Lửa: đốt sinh vật như lửa thường, không phá công trình. Chuột trái = ném ngay với lực tối đa.
- Không bật PvP thì chiêu vẫn gây sát thương và ảnh hưởng người chơi khác như với quái.
