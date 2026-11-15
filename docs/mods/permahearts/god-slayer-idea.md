# God Slayer (Kẻ Tiêu Diệt Thần Thánh): ý tưởng

> Bản nháp ý tưởng, **chưa phải spec**. Chờ Huy trả lời các câu hỏi cuối file rồi mới viết spec và plan.

## Lời Huy (2026-11-15, đã bỏ tạp âm)

- Một boss vô cùng mạnh, **không thể bị hạ hoặc bị lừa bằng những cách thông thường**.
- **Ngoại hình:** đặc biệt, giống **quỷ Oni** Nhật Bản. **Hai tay cầm hai thanh kiếm Nhật.**
- **Dưới nước:**
  - giống **thủy quái trong đền dưới biển**, con bắn ra tia;
  - **bơi nhanh gấp 3 lần cá heo**.
- **Trên không:** người chơi bay để tránh thì nó cũng bay. AI lúc này giống **Vex** của Evoker.
- **Trên mặt đất:**
  - chạy rất nhanh;
  - **vừa bắn xa vừa đánh gần**;
  - lâu lâu **dừng lại, không đuổi nữa, để bắn xa**. Đòn bắn **phá hủy môi trường**.
- **Blast Protection không cứu được.** Ngoài đòn nổ, nó còn bắn những thứ giống **mũi tên nhưng có hoạt ảnh và hình dáng khác**.
- **Liên tục đuổi và đánh người chơi "cứ năm mươi ngày"** (chưa rõ ý, xem câu hỏi 1).
- **Xuất hiện:**
  - chỉ sau **100 ngày** kể từ lúc bắt đầu game;
  - chỉ khi người chơi **đủ sẵn sàng**: có **từ 30 tim trở lên**;
  - **cứ 4 lần Crimson Master xuất hiện thì nó xuất hiện 1 lần**. Hiện Crimson Master đến mỗi 4 ngày và ở 2 ngày, nên khoảng 24 ngày một lần.
  - Nếu người chơi còn yếu, nó **nhắn một câu khinh thường** rồi không xuất hiện.
- **Tên:** God Slayer.

## Đề xuất của manager

### Ngoại hình (tự vẽ, không lấy hình của game khác)

- **Trên cạn:**
  - quỷ Oni cao khoảng 3 block, da đỏ sậm, hai sừng, nanh, tóc trắng rối;
  - giáp vai kiểu samurai đen, hai thanh katana phát sáng mờ;
  - mắt vàng sáng trong bóng tối.
- **Dưới nước:** biến thành **thủy quái Oni**, thân dài có vảy, mặt nạ Oni, một mắt lớn ở giữa trán để bắn tia. Lúc biến hình có hiệu ứng bọt và khói. Lên bờ thì biến lại.
- **Khi bay:** dạng Oni bình thường, có vệt khói đen sau lưng.
- **Thanh máu boss** màu đỏ đen trên đầu màn hình cho mọi người ở gần, tên "God Slayer".

### Đòn đánh

| Đòn | Khi nào | Gì |
|---|---|---|
| Song kiếm | Cận chiến | Chuỗi 3 nhát nhanh. Nhát cuối hất văng. Xuyên một phần giáp |
| Lao chém | Cách 6–15 block | Lao tới như tia chớp rồi chém, đi xuyên qua đường thẳng |
| Hỏa cầu Oni | Đứng lại bắn xa | Quả cầu lửa đỏ đen nổ **phá block** (cỡ Ghast lớn hơn). Đây là đòn phá môi trường |
| Phi đao linh hồn | Đứng lại bắn xa | Loạt 5–7 lưỡi dao tím bay như mũi tên, hình và hoạt ảnh riêng. **Không phải nổ**, nên Blast Protection không giảm |
| Tia mắt | Dưới nước | Ngắm 1–2 giây, có tia báo trước (như thủy quái đền), rồi bắn sát thương lớn |

### Chống lừa (phần khó nhất, cũng là điểm đặc biệt)

- **Không bị kẹt:**
  - không vào được thuyền, xe mỏ, không bị dắt dây;
  - bị kẹt trong block hoặc lỗ thì **phá block xung quanh** như Wither;
  - không bị piston đẩy.
- **Không chết vì môi trường:** miễn sát thương rơi, lửa, dung nham, ngạt, chết đuối, xương rồng, bẫy TNT, đe rơi, và quái khác đánh trúng. **Chỉ người chơi** làm nó mất máu.
- **Không trốn được:**
  - xây cột lên cao thì nó bay;
  - xuống nước thì nó bơi gấp 3 cá heo;
  - trốn sau tường thì đòn nổ phá tường;
  - đứng xa hơn 64 block quá lâu thì nó **dịch chuyển tới gần**;
  - đi qua chiều không gian khác (Nether, End) thì nó **đi theo**.
- **Không bị one-shot:** mỗi đòn chỉ trừ tối đa một mức máu (ví dụ 40). Chiêu khống chế như Frozen Breath làm chậm, Repulse hất văng chỉ còn tác dụng rất ít.
- **Không bị đánh từ chỗ an toàn:** đánh từ chỗ nó không tới được thì nó hồi máu nhanh và bắn phi đao xuyên tường mỏng.
- **Không bị kéo dài:** người chơi né mãi không đánh thì nó hồi máu dần.

### Xuất hiện (theo lời Huy)

1. World đã qua **ngày 100**.
2. Đến **lần thứ 4** Crimson Master xuất hiện (lần 4, 8, 12…), God Slayer "xét" người chơi:
   - **Có ai ≥ 30 tim:** chữ to giữa màn hình, tiếng trống, rồi nó xuất hiện cách người đó khoảng 60 block và lao tới.
   - **Không ai đủ:** mọi người nhận một câu khinh thường trong chat, ví dụ "God Slayer: *Weaklings. Come back when your hearts can bear my blade.*" Nó không xuất hiện.


## Huy trả lời (2026-11-15, lần 2), thay các đề xuất ở trên khi khác nhau

- **Dưới nước:** không biến thành con khác. Ý là **dùng lại AI của Elder Guardian** (boss trong đền dưới biển, bắn tia laser) khi ở dưới nước.
  - Mục tiêu chung: **tận dụng AI của các quái có sẵn** trong game (Elder Guardian dưới nước, Vex khi bay, …) để đỡ phải code mới.
- **Đòn đánh** (song kiếm, lao chém, hỏa cầu phá block, phi đao, tia dưới nước): Huy thấy hợp lý.
- **"50 ngày" là nói lộn.** Mỗi lần xuất hiện, nó đuổi người chơi **tối đa 10 phút**, hoặc tới khi **giết hết mọi người chơi**.
- **Cách xuất hiện:**
  - xuất hiện ngẫu nhiên ở vị trí một người chơi;
  - **60 giây trước**, mọi người thấy thông báo trên màn hình: "God Slayer sắp xuất hiện ở vị trí của người chơi A".
- **Săn lần lượt:**
  - giết xong một người, nó tìm trong danh sách người chơi người nào **nó chưa giết** và **dịch chuyển tới người đó**, kể cả ở chiều không gian khác;
  - ví dụ: xuất hiện ở chỗ A, giết được B; A đã trốn xuống Nether thì nó lập tức dịch chuyển xuống Nether đuổi A.
- **Mạnh dần:**
  - **khởi điểm 500 máu, mỗi nhát kiếm 30 sát thương**;
  - **mỗi lần bị hạ** nó mạnh hơn và **mở khóa skill mới**.
- **Skill mở dần (ví dụ của Huy):**
  - **Tự nổ:** phá hủy một vùng quanh nó, gây sát thương người đứng gần và người đang đánh nó.
  - **Đấu trường:**
    - tạo vùng tròn quanh nó, người chơi **không thoát ra được**, như có tường chắn;
    - tồn tại khoảng 10 giây.
  - **Gọi đệ:**
    - mỗi lần dùng gọi **4 con** quái bất kỳ trong game (Vindicator, Piglin Brute, Evoker, … mọi loại quái);
  - **Che tầm nhìn:** làm người chơi mù hoàn toàn, hoặc chỉ thấy 1–2 block, như Warden (con quái dưới lòng đất).
- **Chưa làm.** Ghi vào backlog (BL-007), brainstorm tiếp sau.
- **Còn mở:**
  - mỗi lần thua nó mạnh lên bao nhiêu;
  - thứ tự mở skill;
  - phần thưởng khi hạ;
  - bay có xuyên tường không;
  - có luật game bật/tắt không.

## Câu hỏi cho Huy (lần 1)

1. **"Cứ 50 ngày" nghĩa là gì?**
   - Gợi ý: nó **đuổi theo người chơi tối đa 50 ngày Minecraft**. Không giết được nó thì hết 50 ngày nó bỏ đi, chờ lần xét tiếp theo.
   - Hay là nó xuất hiện mỗi 50 ngày? Ý này trùng với "cứ 4 lần Crimson Master".
2. **Bay kiểu Vex:** khi bay nó có **xuyên qua tường** như Vex không? Gợi ý: có, nhưng chỉ khi đang đuổi người chơi đang bay.
3. **Mạnh cỡ nào?**
   - Gợi ý: **2000 máu** (1000 tim). Mỗi đòn nhận tối đa 40. Song kiếm 20–30 mỗi nhát. Hỏa cầu 30 ở tâm.
   - Người 30 tim (60 máu) gần như chắc chết nếu đánh một mình. Người 150 tim trở lên mới có cửa.
4. **Phần thưởng khi hạ được?** Gợi ý một trong mấy cái sau:
   - **thanh katana God Slayer** (vũ khí mạnh nhất mod);
   - **+10 tim vĩnh viễn**;
   - **một chiêu mới chỉ có được từ nó**;
   - rất nhiều XP.
5. **Chơi nhiều người:**
   - Nó nhắm ai? Gợi ý: người **nhiều tim nhất** đang ≥ 30 tim.
   - Người khác có được vào đánh cùng không?
   - Người bị nhắm chết thì nó **bỏ đi và cười nhạo**, hay đổi sang người khác?
6. **Có tắt được không?** Gợi ý: một luật game riêng "God Slayer" bật/tắt trong nhóm Immortal Cultivation, mặc định bật.
