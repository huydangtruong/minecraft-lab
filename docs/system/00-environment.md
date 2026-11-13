# Môi trường

Cập nhật: 2026-11-11

| Thứ | Giá trị |
|---|---|
| Minecraft | **26.3** (phát hành 2026-09-15), bản không obfuscate: dùng tên Mojang trực tiếp |
| Fabric Loader | 0.20.1 |
| Fabric API | 0.166.2+26.3 |
| Loom | 1.19.4, plugin `net.fabricmc.fabric-loom` (bản mới: `implementation`, không `modImplementation`, không remap) |
| Gradle | 9.8.2 (wrapper) |
| Java | **25** (bắt buộc cho 26.3). JDK 25 ở `~/Library/Java/JavaVirtualMachines/temurin-25.jdk`; JDK 17 cũ vẫn còn cho AllSeeing/Android |
| Profile | "26.3 Fabric Essential": `~/Library/Application Support/minecraft/installations/26.3 Fabric Essential/` (mods ở `mods/`; `saves`, `resourcepacks`, `screenshots` là symlink về thư mục minecraft chính) |
| Mod có sẵn trong profile | `Essential-1.5.2-26.3.jar` (không đụng) |
| Mẫu chính thức | `FabricMC/fabric-example-mod` nhánh `26.3`; mã nguồn Fabric API: `FabricMC/fabric-api` nhánh `26.3` |

## Essential và chơi chung

Essential mời bạn vào world của Huy → máy Huy chạy server tích hợp, nên logic mod chạy ở máy host và dữ liệu (lưu trong save) nằm ở host. Bạn nên cài cùng bộ jar (`scripts/package.sh`) để đồng bộ và thấy tin nhắn của mod.

## Tra tên class/hàm (không đoán)

```sh
J="$HOME/Library/Application Support/minecraft/runtime/java-runtime-epsilon/mac-os-arm64/java-runtime-epsilon/jre.bundle/Contents/Home/bin"
JAR="$HOME/Library/Application Support/minecraft/versions/26.3/26.3.jar"
"$J/javap" -cp "$JAR" net.minecraft.world.entity.LivingEntity | grep -i kill
unzip -l "$JAR" | grep -i 'monster/Enemy'
```

Mã Fabric API: `gh api "repos/FabricMC/fabric-api/contents/<path>?ref=26.3" --jq .content | base64 -d`.

## Chạy thử không đụng save thật

`./gradlew :<mod>:runClient` dùng thư mục `mods/<mod>/run/` (save và settings riêng, gitignored). Game test server: `./gradlew :<mod>:build`.

## Git / GitHub account

Repo `huydangtruong/minecraft-lab` belongs to Huy's personal account. The repo's local git config (shared by all worktrees) commits as `huydangtruong <58614464+huydangtruong@users.noreply.github.com>` and pushes with that account's token (`credential.https://github.com.helper` calls `gh auth token -u huydangtruong`). The machine's global git identity and gh's active account are the work account `huytruongrt`: never change them. For `gh` commands on this repo use `GH_TOKEN="$(gh auth token -u huydangtruong)" gh …`.
