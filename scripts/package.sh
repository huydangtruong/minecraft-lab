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

1. Cài Fabric Loader 0.20.1 cho Minecraft 26.3 (hoặc dùng Essential Installer chọn Fabric 26.3).
2. Chép TẤT CẢ file .jar trong thư mục "mods" của gói này vào thư mục mods của profile:
   Mac:     ~/Library/Application Support/minecraft/mods
            (hoặc .../minecraft/installations/<tên profile>/mods nếu profile có thư mục riêng)
   Windows: %APPDATA%\\.minecraft\\mods
3. Mở Minecraft Launcher, chọn profile Fabric 26.3, bấm Play.
4. Cần Java 25 (Launcher tự có).
TXT

(cd "$ROOT/dist" && zip -qr "$(basename "$OUT").zip" "$(basename "$OUT")")
echo "Created $OUT.zip"
