# Phát hành một bản mod (D24)

Mỗi bản mod là một GitHub Release trên `huydangtruong/minecraft-lab`. Bạn chơi cài bằng `install-mods` (xem README gốc).

## Các bước

1. Dev xong plan, manager review, merge vào `main`, và `main` đã push lên `origin` (`git status` sạch).
2. Chạy thử không tạo gì: `scripts/release.sh permahearts --dry-run` (in lệnh `gh release create`, ghi `build/release/permahearts/release.json`).
3. Phát hành: `scripts/release.sh permahearts` (bản chính) hoặc `scripts/release.sh permahearts --pre` (bản sửa nhỏ/thử nghiệm). Ghi chú riêng: `--notes "..."`.
4. Script in link release. Mở link kiểm tra có đủ 2 file: jar và `release.json`.

## Script làm gì

- Từ chối nếu không ở `main`, cây làm việc bẩn, `HEAD` khác `origin/main`, hoặc tag đã có.
- Chạy `./gradlew :<id>:build` (test phải xanh), tính sha256 của jar, ghi `release.json`.
- Tạo release tag `<id>-v<bản>` (ví dụ `permahearts-v0.8.0`), tiêu đề `<tên> <bản>`, bằng token tài khoản `huydangtruong` (`gh auth token -u huydangtruong`); không đổi tài khoản gh đang active.

## Ghi chú cho bản 0.8.0

Nên ghi trong `--notes`: mod đổi tên PermaHearts → Immortal Cultivation, id bên trong giữ nguyên nên save cũ vẫn giữ tim, linh dược, nhiệm vụ. Người chơi chạy `install-mods` sẽ tự xoá `permahearts-*.jar` cũ.

## Nếu sai

Xoá release và tag trên GitHub (trang Releases), sửa, chạy lại. Đừng sửa jar đã đăng: sha256 trong `release.json` sẽ không khớp.
