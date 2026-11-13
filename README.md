# Minecraft Lab

Các mod Fabric cho Minecraft Java **26.3** (Fabric Loader 0.20.1) do Huy làm để chơi chung với bạn bè.

## Có gì

| Mod | Làm gì | Bản mới nhất |
|---|---|---|
| **Immortal Cultivation** (id `permahearts`, [luật chơi](docs/mods/permahearts/spec.md)) | Tu tiên: chết mất 1 tim vĩnh viễn, giết quái được HeartXP để lấy lại tim, linh dược, Lữ Khách áo đỏ giao nhiệm vụ | [Releases](https://github.com/huydangtruong/minecraft-lab/releases) |

Tên cũ là PermaHearts. Từ 0.8.0 mod đổi tên hiển thị, nhưng save cũ vẫn chạy bình thường (id bên trong không đổi).

## Cài mod cho người chơi

Cần sẵn: Minecraft Launcher có profile **Fabric Loader cho Minecraft 26.3** (ví dụ cài bằng Essential Installer, chọn Fabric 26.3). Script sẽ tự tìm profile đó, tải mod, kiểm tra file, cài kèm Fabric API nếu thiếu. Script không đụng save và các mod khác (như Essential). Hãy thoát Minecraft trước khi cài.

### Mac

Mở **Terminal** (Cmd+Space, gõ Terminal), dán một dòng này rồi Enter:

```sh
bash <(curl -fsSL https://raw.githubusercontent.com/huydangtruong/minecraft-lab/main/minecraft-lab.command)
```

Hoặc tải file [`minecraft-lab.command`](minecraft-lab.command) về, chuột phải → **Open** → Open (lần đầu macOS sẽ cảnh báo vì file tải từ mạng).

### Windows

Tải file [`minecraft-lab.bat`](minecraft-lab.bat) (chỉ cần file này), bấm đúp để chạy. Nếu Windows SmartScreen hiện cảnh báo: bấm **More info** → **Run anyway**.

### Dùng script

1. Chọn mod trong danh sách (gõ số, Enter).
2. Chọn một trong 6 bản gần nhất. Bản đang cài có chữ `(Installed)`, bản thử nghiệm có chữ `(pre-release)`.
3. Script tải, kiểm tra, xoá bản cũ của mod đó và chép bản mới vào thư mục `mods` của profile.
4. Mở Launcher, chọn đúng profile (ví dụ "26.3 Fabric Essential"), bấm Play.

Chơi chung: host và bạn phải cài **cùng phiên bản** mod.

Không dùng được script? Vào [Releases](https://github.com/huydangtruong/minecraft-lab/releases), tải file `.jar` và chép vào thư mục `mods` của profile (kèm Fabric API cùng bản).

## Cho người làm mod (Huy)

Cần JDK 25 (xem [docs/system/00-environment.md](docs/system/00-environment.md)).

```sh
./gradlew :permahearts:build         # build + test, jar ở mods/permahearts/build/libs/
./gradlew :permahearts:runClient     # chạy Minecraft thử riêng, không đụng save thật
scripts/install.sh                   # build + cài vào profile "26.3 Fabric Essential" của Huy
scripts/release.sh permahearts [--pre]   # đăng một GitHub Release (xem docs/workflow/release.md)
scripts/test-installer.sh            # thử script cài trên dữ liệu giả, không mạng
scripts/package.sh                   # zip gửi bạn (cách cũ, dự phòng)
```

Tài liệu: [docs/README.md](docs/README.md). Thêm mod mới: [docs/workflow/new-mod.md](docs/workflow/new-mod.md). Giấy phép: [MIT](LICENSE).
