#!/usr/bin/env bash
# Build mods and copy them into the "26.3 Fabric Essential" profile.
# Usage: scripts/install.sh [mod-id ...]   (default: every mod in mods/)
source "$(dirname "$0")/common.sh"

[ -d "$MODS_DIR" ] || { echo "Profile mods folder not found: $MODS_DIR" >&2; exit 1; }

IDS=($(mod_ids "$@"))
cd "$ROOT"
./gradlew $(printf ':%s:build ' "${IDS[@]}") --console=plain

# Fabric API (other mods' jars are never touched).
if ! ls "$MODS_DIR"/fabric-api-*.jar >/dev/null 2>&1; then
	fetch_fabric_api "$MODS_DIR"
	echo "Installed $FABRIC_API_JAR"
fi

for id in "${IDS[@]}"; do
	jar="$(mod_jar "$id")"
	[ -f "$jar" ] || { echo "Missing $jar" >&2; exit 1; }
	remove_mod_jars "$MODS_DIR" "$id"   # old versions of this mod only (matched by mod id, any file name)
	cp "$jar" "$MODS_DIR/"
	echo "Installed $(basename "$jar")"
done

pgrep -f "net.fabricmc.loader.impl.launch.knot.KnotClient" >/dev/null && echo "Minecraft is running: restart it to load the new jar."
echo "Mods folder: $MODS_DIR"
