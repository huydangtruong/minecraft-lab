#!/usr/bin/env bash
# Zip the mods + Fabric API + install instructions for a friend: dist/minecraft-lab-<date>.zip
source "$(dirname "$0")/common.sh"

IDS=($(mod_ids "$@"))
cd "$ROOT"
./gradlew $(printf ':%s:build ' "${IDS[@]}") --console=plain

OUT="$ROOT/dist/minecraft-lab-$(date +%Y-%m-%d)"
rm -rf "$OUT" "$OUT.zip"
mkdir -p "$OUT/mods"
for id in "${IDS[@]}"; do cp "$(mod_jar "$id")" "$OUT/mods/"; done
fetch_fabric_api "$OUT/mods"

cat > "$OUT/INSTALL-vi.txt" <<TXT
Cài mod (Minecraft 26.3, Fabric Loader 0.20.1)

Cần sẵn: profile Fabric 26.3 có Essential (cài bằng Essential Installer, chọn Fabric 26.3).

1. Thoát Minecraft nếu đang mở.
2. Mở thư mục mods của profile: trong Minecraft Launcher vào tab Installations,
   rê chuột lên profile "26.3 Fabric Essential", bấm biểu tượng thư mục, mở thư mục "mods"
   (tạo mới nếu chưa có).
   Thường là: Mac     ~/Library/Application Support/minecraft/installations/26.3 Fabric Essential/mods
              Windows %APPDATA%\\.minecraft\\installations\\26.3 Fabric Essential\\mods
3. Xoá bản cũ của các mod này nếu có (ví dụ permahearts-0.7.1.jar hoặc immortal-cultivation-0.8.0.jar cũ), rồi chép TẤT CẢ file .jar
   trong thư mục "mods" của gói này vào đó. Không xoá file Essential.
4. Mở Launcher, chọn profile "26.3 Fabric Essential", bấm Play.

Host và khách phải cài cùng phiên bản mod.
TXT

(cd "$ROOT/dist" && zip -qr "$(basename "$OUT").zip" "$(basename "$OUT")")
echo "Created $OUT.zip"
