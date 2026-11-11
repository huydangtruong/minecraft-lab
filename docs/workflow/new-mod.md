# Thêm mod mới

1. Ghi ý của Huy vào `docs/mods/<id>/idea.md`, luật vào `spec.md`; thêm dòng vào `docs/mods/README.md`.
2. Sao chép `mods/permahearts/` thành `mods/<id>/`; sửa `gradle.properties` (`mod_id`, `mod_name`, `mod_version`, `mod_description`), `fabric.mod.json` (entrypoint, ở cả `src/main/resources` và `src/gametest/resources`), package `io.github.huydangtruong.<id>`. Xoá code cũ.
3. `./gradlew :<id>:build` phải xanh trước khi viết logic (settings.gradle tự nhận thư mục mới).
4. Viết plan `P-NNN` theo [TEMPLATE](../plans/TEMPLATE.md).
