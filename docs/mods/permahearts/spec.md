# PermaHearts — luật chơi (0.2.0)

- Mỗi người chơi bắt đầu **10 tim** (20 máu).
- **Chết**: −1 tim tối đa vĩnh viễn, sàn **1 tim** (D1). Chết ở 1 tim: không mất thêm, báo "đã ở mức thấp nhất". Hồi sinh với máu đầy theo mức mới. HeartXP và thống kê giữ nguyên.
- **HeartXP** (D8): giết mob hostile (`Enemy`: zombie, skeleton, creeper, slime, ghast, phantom, piglin, enderman, Wither, Warden, Ender Dragon…) được HeartXP = **máu tối đa của mob đó**, làm tròn, tối thiểu 1. Ví dụ: Zombie/Skeleton/Creeper +20, Spider +16, Enderman +40, Ravager +100, Ender Dragon +200, Wither +300, Warden +500. Người được tính là người nhận kill credit của vanilla (đánh, bắn, sói, đẩy rơi trong 5 giây). Mob không hostile không tính.
- **Đủ 500 HeartXP** = +1 tim tối đa, phần dư chuyển sang thanh mới (một lần giết hơn 500 có thể cho nhiều tim). Trần **50 tim** (D7): khi đạt trần, HeartXP về 0 và không tích nữa, báo "đã tối đa". Cộng tim: +2 máu hiện tại mỗi tim, âm thanh level-up, tin trong chat.
- Mỗi lần giết: overlay "+20 HeartXP".
- **Thanh HeartXP** (D9): góc dưới bên trái, chữ "HeartXP 320/500" (ở trần: "HeartXP MAX") trên một thanh đỏ. Ẩn khi Creative/Spectator và khi tắt HUD (F1). Chỉ thấy khi client cũng cài mod.
- Dữ liệu lưu theo người chơi trong save của world (Essential: save của máy host), đồng bộ xuống client của chính người đó.
- Lệnh: `/hearts` (tim, HeartXP, tổng kill, số lần chết); `/hearts set <targets> <1..50>` và `/hearts xp <targets> <0..499>` cần quyền gamemaster.
- Tin nhắn có en_us và vi_vn; người không cài mod vẫn đọc được bản tiếng Anh.
- Host và bạn phải cài **cùng phiên bản** mod.

## Bật/tắt theo world (0.3.0, D10)

- Game rule `permahearts:enabled`, **mặc định tắt**. Tạo world: **More → Game Rules → PermaHearts** (nhóm Player) → bật. Trong game: `/gamerule permahearts:enabled true` hoặc `false` (cần quyền lệnh).
- Khi tắt: chết không mất tim, giết mob không có HeartXP, máu tối đa về 10 tim, thanh HeartXP ẩn. Dữ liệu tim/HeartXP vẫn lưu; bật lại là trở về như cũ. Đổi trạng thái thì mọi người đang online nhận tin báo.
- `/hearts` vẫn chạy khi tắt và nhắc cách bật.
