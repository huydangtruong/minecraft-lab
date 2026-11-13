#!/usr/bin/env bash
# Publish a mod version as a GitHub Release on huydangtruong/minecraft-lab.
# Usage: scripts/release.sh <mod-id> [--pre] [--notes "<text>"] [--dry-run]
#   --pre      mark the release as a pre-release
#   --dry-run  run the checks as warnings, build, write release.json, print the gh command, create nothing
source "$(dirname "$0")/common.sh"

REPO="huydangtruong/minecraft-lab"
ID="" PRE=0 NOTES="" DRY=0
while [ "$#" -gt 0 ]; do
	case "$1" in
		--pre) PRE=1 ;;
		--dry-run) DRY=1 ;;
		--notes) shift; NOTES="${1:?--notes needs a text}" ;;
		-*) echo "Unknown option: $1" >&2; exit 2 ;;
		*) ID="$1" ;;
	esac
	shift
done
[ -n "$ID" ] || { echo "Usage: scripts/release.sh <mod-id> [--pre] [--notes \"<text>\"] [--dry-run]" >&2; exit 2; }
[ -f "$ROOT/mods/$ID/gradle.properties" ] || { echo "Unknown mod: $ID" >&2; exit 2; }

NAME="$(mod_prop "$ID" mod_name)"
VER="$(mod_prop "$ID" mod_version)"
TAG="${ID}-v${VER}"
MC_VER="$(grep '^minecraft_version=' "$ROOT/gradle.properties" | cut -d= -f2)"
LOADER_VER="$(grep '^loader_version=' "$ROOT/gradle.properties" | cut -d= -f2)"
cd "$ROOT"

# 1. Safety checks (warnings only with --dry-run).
problems=()
[ "$(git rev-parse --abbrev-ref HEAD)" = "main" ] || problems+=("not on branch main")
[ -z "$(git status --porcelain)" ] || problems+=("working tree is not clean")
git fetch -q origin main 2>/dev/null || problems+=("cannot fetch origin/main")
[ "$(git rev-parse HEAD)" = "$(git rev-parse origin/main 2>/dev/null || echo none)" ] || problems+=("HEAD is not equal to origin/main")
if [ -n "$(git ls-remote --tags origin "refs/tags/$TAG" 2>/dev/null)" ]; then problems+=("tag $TAG already exists"); fi
if [ "${#problems[@]}" -gt 0 ]; then
	for p in "${problems[@]}"; do
		if [ "$DRY" = 1 ]; then echo "WARNING (dry run): $p"; else echo "Refusing to release: $p" >&2; fi
	done
	[ "$DRY" = 1 ] || exit 1
fi

# 2. Build with tests.
./gradlew ":$ID:build" --console=plain

# 3. release.json
JAR="$(mod_jar "$ID")"
[ -f "$JAR" ] || { echo "Missing $JAR" >&2; exit 1; }
SHA="$(shasum -a 256 "$JAR" | cut -d' ' -f1)"
OUT="$ROOT/build/release/$ID"
mkdir -p "$OUT"
cat > "$OUT/release.json" <<JSON
{"mod_id":"$ID","mod_name":"$NAME","version":"$VER",
 "minecraft":"$MC_VER","fabric_loader":"$LOADER_VER","fabric_api":"$FABRIC_API_VERSION",
 "jar":"$(basename "$JAR")","sha256":"$SHA"}
JSON

if [ -z "$NOTES" ]; then
	NOTES="$NAME $VER for Minecraft $MC_VER (Fabric Loader $LOADER_VER, Fabric API $FABRIC_API_VERSION). Install with minecraft-lab.command (Mac) or minecraft-lab.bat (Windows) from the repository root."
fi

# 4. Create the release as the personal account (never switch gh's active account).
ARGS=(release create "$TAG" --repo "$REPO" --target "$(git rev-parse HEAD)" --title "$NAME $VER" --notes "$NOTES")
[ "$PRE" = 1 ] && ARGS+=(--prerelease)
ARGS+=("$JAR" "$OUT/release.json")

if [ "$DRY" = 1 ]; then
	echo "Dry run: nothing created. Would run:"
	printf 'GH_TOKEN="$(gh auth token -u huydangtruong)" gh'
	printf ' %q' "${ARGS[@]}"
	echo
	echo "release.json:"; cat "$OUT/release.json"
	exit 0
fi

GH_TOKEN="$(gh auth token -u huydangtruong)" gh "${ARGS[@]}"
# 5. URL
echo "https://github.com/$REPO/releases/tag/$TAG"
