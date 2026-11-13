# Immortal Cultivation (id `permahearts`, tên cũ PermaHearts) — luật chơi (0.2.0, tên mới từ 0.8.0)

- Mỗi người chơi bắt đầu **10 tim** (20 máu).
- **Chết**: −1 tim tối đa vĩnh viễn, sàn **1 tim** (D1). Chết ở 1 tim: không mất thêm, báo "đã ở mức thấp nhất". Hồi sinh với máu đầy theo mức mới. HeartXP và thống kê giữ nguyên.
- **HeartXP** (D8): giết mob hostile (`Enemy`: zombie, skeleton, creeper, slime, ghast, phantom, piglin, enderman, Wither, Warden, Ender Dragon…) được HeartXP = **máu tối đa của mob đó**, làm tròn, tối thiểu 1. Ví dụ: Zombie/Skeleton/Creeper +20, Spider +16, Enderman +40, Ravager +100, Ender Dragon +200, Wither +300, Warden +500. Người được tính là người nhận kill credit của vanilla (đánh, bắn, sói, đẩy rơi trong 5 giây). Mob không hostile không tính.
- **Đủ 500 HeartXP** = +1 tim tối đa, phần dư chuyển sang thanh mới (một lần giết hơn 500 có thể cho nhiều tim). Trần **50 tim** (D7): khi đạt trần, HeartXP về 0 và không tích nữa, báo "đã tối đa". Cộng tim: +2 máu hiện tại mỗi tim, âm thanh level-up, tin trong chat.
- Mỗi lần giết: overlay "+20 HeartXP".
- **Thanh HeartXP** (D9): góc dưới bên trái (cách mép trái và mép dưới 8px), chữ "HeartXP 320/500" (ở trần: "HeartXP MAX") trên một thanh đỏ. Ẩn khi Creative/Spectator và khi tắt HUD (F1). Chỉ thấy khi client cũng cài mod.
- Dữ liệu lưu theo người chơi trong save của world (Essential: save của máy host), đồng bộ xuống client của chính người đó.
- Lệnh: `/hearts` (tim, HeartXP, tổng kill, số lần chết); `/hearts set <targets> <1..50>` và `/hearts xp <targets> <0..499>` cần quyền gamemaster.
- Mọi chữ của mod (giao diện, tên item, tin nhắn) chỉ có **tiếng Anh** (D16). Tên đồ/quái của game gốc vẫn theo ngôn ngữ game.
- Host và bạn phải cài **cùng phiên bản** mod.

## Bật/tắt theo world (0.3.0, D10)

- Game rule `permahearts:enabled`, **mặc định tắt**. Tạo world: **More → Game Rules → PermaHearts** (nhóm Player) → bật. Trong game: `/gamerule permahearts:enabled true` hoặc `false` (cần quyền lệnh).
- Khi tắt: chết không mất tim, giết mob không có HeartXP, máu tối đa về 10 tim, thanh HeartXP ẩn. Dữ liệu tim/HeartXP vẫn lưu; bật lại là trở về như cũ. Đổi trạng thái thì mọi người đang online nhận tin báo.
- `/hearts` vẫn chạy khi tắt và nhắc cách bật.

## Công tắc ImmortalCultivation (0.4.0, D11)

- Game rule `permahearts:immortal_cultivation` (tên hiện trong Game Rules: **ImmortalCultivation**), mặc định tắt, thay `permahearts:enabled`. Một công tắc cho tất cả: tim, HeartXP, linh dược, Lữ Khách.
- World đã bật bằng lệnh cũ: chạy lại `/gamerule permahearts:immortal_cultivation true`. Dữ liệu tim không mất.

## Linh dược (0.4.0, D15)

| Item | Tên VN | Mỗi bình | Thuộc tính |
|---|---|---|---|
| `permahearts:elixir_strength` | Elixir of Strength | +0.5 sát thương | `ATTACK_DAMAGE` cộng |
| `permahearts:elixir_fury` | Elixir of Fury | +10% tốc đánh | `ATTACK_SPEED` ×base |
| `permahearts:elixir_swiftness` | Elixir of Swiftness | +5% tốc chạy | `MOVEMENT_SPEED` ×base |
| `permahearts:elixir_iron_skin` | Elixir of Iron Skin | +1 giáp | `ARMOR` cộng |
| `permahearts:elixir_reach` | Elixir of Reach | +0.5 tầm với | `BLOCK_INTERACTION_RANGE` + `ENTITY_INTERACTION_RANGE` cộng |
| `permahearts:elixir_mining` | Elixir of Mining | +10% tốc đào | `BLOCK_BREAK_SPEED` ×base |

- Uống như bình thuốc (trả lại chai rỗng), có hiệu ứng lấp lánh, xếp chồng 16. Không trần, lưu theo người chơi, chết không mất.
- Công tắc tắt: uống không được (báo cách bật), chỉ số đã có không tác dụng nhưng vẫn lưu.
- `/hearts` có thêm dòng chỉ số linh dược. Thử: `/give @s permahearts:elixir_strength 4`.

## Lữ Khách và nhiệm vụ (0.5.0–0.6.0, D12–D14)

- Mỗi 3–5 ngày Minecraft (ngẫu nhiên): chọn một người chơi ở Overworld, lấy điểm cách ~450–550 block (D27) hướng ngẫu nhiên, báo mọi người "Một Lữ Khách đang chờ ở X, Z". NPC hiện khi có người trong ~96 block. Trên mặt nước: đặt bè gỗ 3×3.
- NPC: Wandering Trader đứng yên, bất tử, không biến mất, tên "Lữ Khách". Bấm vào: màn hình nhiệm vụ (không phải bảng trao đổi).
- Màn hình: lời giới thiệu + **Nhận** → danh sách 2–4 dòng. Mọi người thấy cùng tiến độ.
  - **Thu thập**: "Tìm 64 Sắt" + ô bỏ đồ; bỏ vào là trừ đồ, cộng tiến độ, dư trả lại. Số lượng (D18): 64 sắt / than / redstone / lapis / cà rốt / khoai tây / lúa mì / gỗ sồi · 48 vàng / thịt cừu / thịt bò / xương / tơ / da · 32 thuốc súng / ngọc ender · 16 kim cương / ngọc lục bảo.
  - **Giết**: "Giết 16 Zombie" + số đếm; ai giết cũng tính. 16 zombie / skeleton / nhện / creeper / enderman / drowned · 8 phù thủy.
  - **Trăng máu** (0.6.0): ngẫu nhiên 1 trong 6 loại (D29): đêm **Zombie**, **Piglin Brute** (không biến thành zombie piglin), **Creeper**, **Skeleton**, **Spider** hoặc **Zombie Horseman** (zombie cầm giáo sắt cưỡi ngựa zombie, như bản gốc; một kỵ sĩ tính là 1 con trong giới hạn 30; dòng này không đi cùng dòng giết zombie hay trùm zombie). Nút Bắt đầu → trời thành đêm, mỗi 10 giây 5 con loại đó quanh mỗi người chơi ở Overworld (tối đa 30 con sống), không ngủ được; người bấm sống tới sáng là xong, chết thì dòng quay về chưa làm.
  - **Thám hiểm** (0.6.0): "Đặt chân tới một Làng/Đền sa mạc/…".
  - **Hạ thủ lĩnh** (0.6.0): nút Triệu hồi → trùm to gấp đôi, 150 máu, 4 đệ tử, cạnh NPC. Trùm: Zombie, Skeleton, Evoker (đệ tử là Vindicator), Blaze, Vindicator Chieftain.
- Dòng xong → ô thưởng có 1 linh dược ngẫu nhiên; ai lấy trước được. Xong hết + lấy hết thưởng → NPC chào và đi, đếm lại 3–5 ngày.
- Lệnh: `/quest` (NPC ở đâu, còn bao lâu, tiến độ), `/quest giveup` (ai cũng dùng được, như nút Give up: Master rời đi, lần sau 5–7 ngày; D28). Admin: `/quest summon` (gọi NPC ngay cạnh mình với nhiệm vụ mới), `/quest skip` (NPC xuất hiện ngay ở ~500 block), `/quest complete <dòng>`, `/quest reset`.

## 0.7.1 (P-008, D19, D20)

- **Give up**: nút chữ đỏ ở cuối danh sách nhiệm vụ (cả trước và sau Nhận). Bấm 2 lần trong 3 giây: dừng sự kiện, xóa quái/trùm/đệ tử, Lữ Khách đi, thưởng chưa lấy mất. Ai cũng bấm được.
- **Lữ Khách không bị quái nhắm** (bất tử vĩnh viễn nên quái bỏ qua).
- Trùm và quái Trăng máu **xuất hiện ngang tầm** người chơi / Lữ Khách (kể cả trong hang), tìm chỗ đứng quanh Y đó chứ không lấy mặt đất.
- Trùm bị mất (rơi, unload) thì nút quay về **Summon**; summon lại được, bản cũ nếu trở lại sẽ bị xóa.
- **Ở 3 ngày Minecraft** kể từ lúc báo tọa độ; màn hình có dòng đếm ngược "Leaves in"; chat báo trước ~2 phút thực; không rời khi Trăng máu hoặc trùm đang chạy. Rời đi (hết giờ, Give up, xong hết) thì 5–7 ngày sau mới có Lữ Khách mới; chuyến đầu tiên của world vẫn 3–5 ngày.
- Thủ lĩnh và đệ tử không bị mặt trời thiêu (mũ không vỡ).
- `/quest` in thêm thời gian còn lại.

- Tên hiển thị của Lữ Khách: **Crimson Master** (D21).
