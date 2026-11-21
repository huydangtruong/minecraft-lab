# Crystal Mayhem (D174, P-064)

Chiêu thứ 20, id `mayhem`, nhóm Attack (sau Life Stealer). Hồi chiêu 30 giây ở mọi cấp.

## Chuột trái: Shard Projection
- Bắn 1 mảnh pha lê nhanh (hitscan, tầm 64). Sát thương = `2 + extra * (1 + 0,05 * (L-1))`.
- Ánh sáng tại mục tiêu: sky = sky light thật trừ độ tối bầu trời, bằng 0 khi giờ trong ngày thuộc [16000, 23000). Lấy cái cao hơn giữa sky và block (hoà thì sky). `extra` = `sky/3` nếu sky thắng, ngược lại `block/5`.
- Một lượt có 20 mảnh. Giữa hai mảnh chờ `max(0,1 s; 1 - 0,05 * (L-1))` giây.
- Mỗi mảnh của lượt đầu để lại 1 cụm pha lê (cluster) 10 giây.
- Sau mảnh thứ 20 hồi chiêu 30 giây bắt đầu, nhưng vẫn dùng được kênh (Light Strike Reflection) một lần.

## Giữ chuột phải 1 giây: Light Strike Reflection
- Không dùng được trong lúc chờ giữa hai mảnh.
- Cụm pha lê trong 5 block quanh người chơi hoá thành budding, rồi dây chuyền: cụm trong 5 block của một budding cũng hoá, tối đa `4 + L` budding. Mỗi budding mọc cụm trang trí ở mọi mặt tiếp xúc không khí.
- Budding nối với người chơi (mắt xích tối đa 5 block) là budding đang hoạt động: sáng 15, làm chậm, đốt `1 + 0,5 * (L-1)` mỗi giây trong bán kính `3 + (L-1)/2`, mỗi mục tiêu một lần mỗi giây.
- Nạp lại 20 mảnh (mảnh nạp không để lại cụm).
- Sau 20 giây mọi block biến mất, hồi chiêu 30 giây bắt đầu. Kênh không có cụm nào vẫn nạp mảnh.
- Chết, đổi chiều không gian, thoát game: kết thúc ngay. Khởi động lại server: block còn sót bị dọn khi chunk tải (lưu trong `crystal_mayhem` saved data).

## Cách làm (kỹ thuật)
- Luật thuần: `MayhemRules` (JUnit `MayhemRulesTest`). Logic: `Mayhem`, block: `MayhemBlocks` (`mayhem_cluster`, `mayhem_budding`), dữ liệu: `MayhemSavedData`.
- Client: `ClientMayhem` (gói `mayhem_state`), dòng HUD "Shards N/20" và số giây còn lại trong `SkillBarHud`.
- Thử: `/technique cast mayhem <lv>`, tab Techniques trong `/admin`.
