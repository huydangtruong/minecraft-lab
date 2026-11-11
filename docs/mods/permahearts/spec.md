# PermaHearts — luật chơi

- Mỗi người chơi bắt đầu **10 tim** (20 máu).
- **Chết**: −1 tim tối đa vĩnh viễn, sàn **1 tim** (D1). Chết ở 1 tim: không mất thêm, báo "đã ở mức thấp nhất". Hồi sinh với máu đầy theo mức mới.
- **Giết mob hostile**: mob là `Enemy` (zombie, skeleton, creeper, slime, ghast, phantom, piglin…), người được tính là người nhận kill credit của vanilla (đánh trực tiếp, bắn cung, sói của mình, đẩy rơi trong 5 giây). Slime tách nhỏ tính từng con. Mob không hostile (bò, golem…) không tính.
- **Mỗi 100 kill**: +1 tim tối đa vĩnh viễn, trần **30 tim** (D6, thay D2). Ở 30 tim thì tiến độ về 0 và báo "đã tối đa". Cộng tim: +2 máu hiện tại, âm thanh level-up, tin trong chat.
- Tiến độ kill **không** reset khi chết. Overlay "Hostile 37/100" mỗi lần giết.
- Dữ liệu lưu theo người chơi trong save (khi chơi Essential: save của máy host). Sống sót qua chết, thoát/vào lại, về từ End.
- Lệnh: `/hearts` (xem tim, tiến độ, tổng kill, số lần chết); `/hearts set <targets> <1..30>` và `/hearts progress <targets> <0..99>` cần quyền gamemaster (để test/cứu).
- Tin nhắn có bản en_us và vi_vn; người không cài mod vẫn đọc được bản tiếng Anh (fallback).
