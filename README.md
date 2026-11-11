# Minecraft Lab

Nơi Huy code các mod Fabric cho Minecraft Java **26.3** (Fabric Loader 0.20.1) để chơi chung với bạn qua Essential.

## Có gì

| Mod | Làm gì | Trạng thái |
|---|---|---|
| [permahearts](docs/mods/permahearts/spec.md) | Chết mất 1 tim vĩnh viễn; giết 100 mob hostile được +1 tim | Khung build xong, [P-001](docs/plans/P-001-permahearts.md) chờ code |

## Dùng

Cần JDK 25 (đã cài ở `~/Library/Java/JavaVirtualMachines/temurin-25.jdk`).

```sh
scripts/install.sh            # build + cài vào profile "26.3 Fabric Essential" (khởi động lại game sau đó)
scripts/package.sh            # tạo dist/minecraft-lab-<ngày>.zip gửi bạn (kèm Fabric API và hướng dẫn cài)
./gradlew :permahearts:runClient   # chạy Minecraft thử riêng, không đụng save thật
```

Chơi chung: người host world chạy mod; bạn cần cài **cùng bộ jar** (gói từ `package.sh`). Chi tiết: [docs/README.md](docs/README.md).

## Thêm mod mới

[docs/workflow/new-mod.md](docs/workflow/new-mod.md).
