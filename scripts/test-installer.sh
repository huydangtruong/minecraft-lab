#!/bin/bash
# Fixture test for minecraft-lab.command. No network, never touches the real Minecraft folder:
# HOME, MC_DIR, MCLAB_RELEASES and MCLAB_TTY all point into a temp dir, and pgrep is a stub.
# Usage: scripts/test-installer.sh [--screens <dir>]   (--screens also saves console text of each step)
cd "$(dirname "$0")/.." || exit 1
ROOT="$(pwd)"
INSTALLER="$ROOT/minecraft-lab.command"
SCREENS=""
[ "${1:-}" = "--screens" ] && SCREENS="${2:?--screens needs a folder}"

T="$(mktemp -d "${TMPDIR:-/tmp}/mclab-test.XXXXXX")"
trap 'rm -rf "$T"' EXIT
FAILS=0
pass() { echo "  ok   $1"; }
fail() { echo "  FAIL $1"; FAILS=$((FAILS + 1)); }
check() { # description, command...
	local d="$1"; shift
	if "$@" >/dev/null 2>&1; then pass "$d"; else fail "$d"; fi
}

# ---- fixture helpers
make_jar() { # path id version [extra text]
	local d; d="$(mktemp -d "$T/jar.XXXXXX")"
	printf '{\n\t"schemaVersion": 1,\n\t"id": "%s",\n\t"version": "%s",\n\t"name": "%s"\n}\n' "$2" "$3" "$2" > "$d/fabric.mod.json"
	printf '%s' "${4:-}" > "$d/payload.txt"
	rm -f "$1"; (cd "$d" && zip -q "$1" fabric.mod.json payload.txt)
}

HOME_DIR="$T/home"
MC="$HOME_DIR/Library/Application Support/minecraft"
PROFILE_DIR="$MC/installations/26.3 Fabric Essential"

make_minecraft() { # fabric(1|0)
	rm -rf "$MC"; mkdir -p "$PROFILE_DIR/mods"
	if [ "$1" = 1 ]; then
		cat > "$MC/launcher_profiles.json" <<JSON
{"profiles":{"Essential-installer-2026-11-11T15:39:43.477440Z":{"name":"26.3 Fabric Essential","lastVersionId":"fabric-loader-0.20.1-26.3","gameDir":"$PROFILE_DIR","icon":"Furnace"},
"bbb222":{"name":"","lastVersionId":"26.3","type":"latest-release"}},"settings":{"locale":"en-us"},"version":3}
JSON
	else
		cat > "$MC/launcher_profiles.json" <<JSON
{"profiles":{"bbb222":{"name":"","lastVersionId":"26.3","type":"latest-release"}},"version":3}
JSON
	fi
	make_jar "$PROFILE_DIR/mods/permahearts-0.7.1.jar" permahearts 0.7.1 "old"
	make_jar "$PROFILE_DIR/mods/Essential-1.5.2-26.3.jar" essential 1.5.2 "essential"
	make_jar "$PROFILE_DIR/mods/fabric-api-0.166.2+26.3.jar" fabric-api 0.166.2+26.3 "api"
}

VERSIONS="0.8.0 0.7.2 0.7.1 0.7.0 0.6.0 0.5.0 0.4.0"
make_releases() { # output json, asset dir, bad(0|1): wrong sha256 for 0.8.0
	local out="$1" assets="$2" bad="$3" v pre day=7 sep="" sha
	mkdir -p "$assets"
	printf '[\n' > "$out"
	for v in $VERSIONS; do
		mkdir -p "$assets/$v"
		make_jar "$assets/$v/immortal-cultivation-$v.jar" permahearts "$v" "build $v"
		sha="$(shasum -a 256 "$assets/$v/immortal-cultivation-$v.jar" | cut -d' ' -f1)"
		if [ "$bad" = 1 ] && [ "$v" = "0.8.0" ]; then sha="0000000000000000000000000000000000000000000000000000000000000000"; fi
		printf '{"minecraft":"26.3","fabric_loader":"0.20.1","mod_id":"permahearts","mod_name":"Immortal Cultivation","version":"%s","fabric_api":"0.166.2+26.3","jar":"immortal-cultivation-%s.jar","sha256":"%s"}\n' \
			"$v" "$v" "$sha" > "$assets/$v/release.json"
		pre=false; [ "$v" = "0.7.2" ] && pre=true
		printf '%s{"url":"https://api.github.com/x","tag_name":"permahearts-v%s","name":"Immortal Cultivation %s","draft":false,"prerelease":%s,"created_at":"2026-11-%02dT10:00:00Z","published_at":"2026-11-%02dT10:00:00Z","author":{"login":"huydangtruong"},"body":"Old saves keep working \\u2014 caf\\u00e9 \\ud83d\\ude00 \\"quoted\\"\\nline two","reactions":null,"assets":[{"name":"immortal-cultivation-%s.jar","label":null,"browser_download_url":"file://%s/%s/immortal-cultivation-%s.jar"},{"name":"release.json","label":null,"browser_download_url":"file://%s/%s/release.json"}]}' \
			"$sep" "$v" "$v" "$pre" "$day" "$day" "$v" "$assets" "$v" "$v" "$assets" "$v" >> "$out"
		sep=$',\n'; day=$((day - 1))
	done
	printf '\n]\n' >> "$out"
}

# pgrep stub: exits 1 (Minecraft not running), or 0 when $T/running exists.
mkdir -p "$T/bin"
cat > "$T/bin/pgrep" <<STUB
#!/bin/bash
[ -f "$T/running" ]
STUB
chmod +x "$T/bin/pgrep"

OUT=""; RC=0
run_installer() { # releases json, answers (newline separated string)
	printf '%s\n' "$2" > "$T/answers"
	OUT="$(HOME="$HOME_DIR" PATH="$T/bin:$PATH" MC_DIR="$MC" MCLAB_RELEASES="$1" MCLAB_TTY="$T/answers" bash "$INSTALLER" 2>&1)"
	RC=$?
}
sum_of() { shasum -a 256 "$1" | cut -d' ' -f1; }
mods() { ls "$PROFILE_DIR/mods"; }

make_releases "$T/releases.json" "$T/assets" 0
make_releases "$T/releases-bad.json" "$T/assets-bad" 1

# ---- case 1: install 0.8.0 over 0.7.1
echo "Case 1: install over an older version"
make_minecraft 1
ESS="$(sum_of "$PROFILE_DIR/mods/Essential-1.5.2-26.3.jar")"
API="$(sum_of "$PROFILE_DIR/mods/fabric-api-0.166.2+26.3.jar")"
touch "$T/running"   # also exercises the "Minecraft is running" prompt (needs the extra Enter)
run_installer "$T/releases.json" $'1\n1\n\n1\n0\n0'   # mod, version, Enter (running), back to versions, back to mods, exit
rm -f "$T/running"
[ -n "$SCREENS" ] && printf '%s\n' "$OUT" > "$T/case1.out"
check "exit code 0" test "$RC" -eq 0
check "menu shows installed 0.7.1" grep -q 'Immortal Cultivation   installed 0.7.1' <<<"$OUT"
VMENU="$(printf '%s\n' "$OUT" | awk '/^Immortal Cultivation$/{f=1;next} /^  0\) Back/{if(f)exit} f')"
check "versions menu has exactly 6 versions" test "$(printf '%s\n' "$VMENU" | grep -c '^  [0-9])')" -eq 6
check "0.7.2 is marked (pre-release)" grep -q '^  2) 0.7.2  (pre-release)$' <<<"$VMENU"
check "0.7.1 is marked (Installed)" grep -q '^  3) 0.7.1  (Installed)$' <<<"$VMENU"
check "0.4.0 is not offered" bash -c '! grep -q "0.4.0" <<<"$1"' _ "$VMENU"
check "warned that Minecraft is running" grep -q 'Minecraft seems to be running' <<<"$OUT"
check "success line printed" grep -q '^SUCCESS! Immortal Cultivation 0.8.0 is installed.$' <<<"$OUT"
check "profile hint printed" grep -q '^Start Minecraft with the "26.3 Fabric Essential" profile.$' <<<"$OUT"
check "asks back-or-exit right after the result" bash -c 'grep -A3 "^Start Minecraft with" <<<"$1" | grep -q "1) Back to the version list"' _ "$OUT"
check "version list shows 0.8.0 (Installed) after going back" grep -q '^  1) 0.8.0  (Installed)$' <<<"$OUT"
check "new jar present" test -f "$PROFILE_DIR/mods/immortal-cultivation-0.8.0.jar"
check "old permahearts-0.7.1.jar removed" test ! -e "$PROFILE_DIR/mods/permahearts-0.7.1.jar"
check "Essential untouched" test "$(sum_of "$PROFILE_DIR/mods/Essential-1.5.2-26.3.jar")" = "$ESS"
check "Fabric API untouched" test "$(sum_of "$PROFILE_DIR/mods/fabric-api-0.166.2+26.3.jar")" = "$API"
check "mods folder has exactly 3 jars" test "$(mods | wc -l | tr -d ' ')" -eq 3
check "menu shows installed 0.8.0 afterwards" grep -q 'installed 0.8.0' <<<"$OUT"

# ---- case 1b: exit right after installing
echo "Case 1b: exit after installing"
make_minecraft 1
run_installer "$T/releases.json" $'1\n1\n0'
check "exit code 0" test "$RC" -eq 0
check "no menu printed after the result" bash -c '! sed -n "/^SUCCESS!/,\$p" <<<"$1" | grep -q "^Minecraft Lab mods"' _ "$OUT"
check "new jar present" test -f "$PROFILE_DIR/mods/immortal-cultivation-0.8.0.jar"

# ---- case 2: wrong sha256
echo "Case 2: wrong checksum"
make_minecraft 1
BEFORE="$(mods | tr '\n' '|')"
run_installer "$T/releases-bad.json" $'1\n1\n0'
[ -n "$SCREENS" ] && printf '%s\n' "$OUT" > "$T/case2.out"
check "exit code non-zero" test "$RC" -ne 0
check "sha256 mismatch reported" grep -q 'sha256 mismatch' <<<"$OUT"
check "mods folder unchanged" test "$(mods | tr '\n' '|')" = "$BEFORE"
check "old jar still there" test -f "$PROFILE_DIR/mods/permahearts-0.7.1.jar"

# ---- case 3: no Fabric profile
echo "Case 3: no Fabric profile"
make_minecraft 0
run_installer "$T/releases.json" $''
[ -n "$SCREENS" ] && printf '%s\n' "$OUT" > "$T/case3.out"
check "exit code non-zero" test "$RC" -ne 0
check "Fabric message shown" grep -q 'No Minecraft profile with Fabric Loader for Minecraft 26.3' <<<"$OUT"
check "Fabric installer link shown" grep -q 'https://fabricmc.net/use/installer/' <<<"$OUT"

# ---- optional: save console text of each step
if [ -n "$SCREENS" ]; then
	mkdir -p "$SCREENS"
	make_minecraft 1
	run_installer "$T/releases.json" $'0'
	printf '%s\n' "$OUT" > "$SCREENS/01-mods-menu.txt"
	run_installer "$T/releases.json" $'1\n0\n0'
	printf '%s\n' "$OUT" | awk '{print} /^Choose a version and press Enter: 0$/{exit}' > "$SCREENS/02-versions-menu.txt"
	make_minecraft 1
	run_installer "$T/releases.json" $'1\n1\n1\n0\n0'
	printf '%s\n' "$OUT" > "$T/walk.out"
	awk '/^Choose a version and press Enter: 1$/{f=1} f{print} /^Choose and press Enter/{exit}' "$T/walk.out" > "$SCREENS/03-installing.txt"
	awk '/^Minecraft Lab mods/{buf=""} {buf=buf $0 "\n"} END{printf "%s", buf}' "$T/walk.out" > "$SCREENS/04-mods-menu-after.txt"
	cp "$T/case3.out" "$SCREENS/05-no-fabric.txt"
	awk '/^Choose a version and press Enter: 1$/{f=1} /^Minecraft Lab mods/{if(f&&seen){exit}} f{print} /^Minecraft Lab mods/{seen=1}' "$T/case2.out" > "$SCREENS/06-bad-checksum.txt"
	# Show a plain home folder instead of the temp fixture path.
	for f in "$SCREENS"/*.txt; do sed -i '' "s#$HOME_DIR#/Users/player#g" "$f"; done
	echo "Screens saved in $SCREENS"
fi

echo
if [ "$FAILS" -eq 0 ]; then echo "All installer checks passed."; else echo "$FAILS installer check(s) FAILED."; exit 1; fi
