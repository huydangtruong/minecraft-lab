#!/bin/bash
# Minecraft Lab mod installer for macOS.
# Run in Terminal:  bash <(curl -fsSL https://raw.githubusercontent.com/huydangtruong/minecraft-lab/main/minecraft-lab.command)
# or double-click this file. Needs only stock macOS tools: curl, unzip, shasum, plutil (macOS 12+).
# Written for the stock bash 3.2 (no mapfile, no associative arrays).
#
# Test overrides (environment): MCLAB_RELEASES (local releases JSON instead of the GitHub API),
# MC_DIR (Minecraft root), MCLAB_TTY (file with answers instead of /dev/tty).

REPO="huydangtruong/minecraft-lab"
API_URL="https://api.github.com/repos/$REPO/releases?per_page=100"
DEFAULT_MC="26.3"
MC_ROOT="${MC_DIR:-$HOME/Library/Application Support/minecraft}"
TAB="$(printf "\t")"
SEP=$'\037'

# Colours only on a real terminal (tests capture plain text).
if [ -t 1 ] && [ -z "${NO_COLOR:-}" ]; then
	GREEN=$'\033[1;32m'; RED=$'\033[1;31m'; RESET=$'\033[0m'
else
	GREEN=""; RED=""; RESET=""
fi
err() { printf '%s%s%s\n' "$RED" "$*" "$RESET" >&2; }

TMP="$(mktemp -d "${TMPDIR:-/tmp}/mclab-installer.XXXXXX")" || { echo "Cannot create a temp folder." >&2; exit 1; }
trap 'rm -rf "$TMP"' EXIT
RJSON="$TMP/releases.json"

# ---------------------------------------------------------------- input

# All answers come from one place so `bash <(curl ...)` works (stdin is the script then).
if ! ( exec 3<"${MCLAB_TTY:-/dev/tty}" ) 2>/dev/null; then
	echo "This installer needs a terminal to ask questions. Run it in the Terminal app." >&2
	exit 1
fi
exec 3<"${MCLAB_TTY:-/dev/tty}"

# ask "prompt": prints the prompt, reads one line into $REPLY.
ask() {
	printf '%s' "$1"
	if ! IFS= read -r -u 3 REPLY; then
		echo
		echo "No more input. Nothing else was changed."
		exit 1
	fi
	REPLY="${REPLY%$'\r'}"
	# When answers come from a file, echo them so the screen looks like a real session.
	if [ -n "${MCLAB_TTY:-}" ]; then printf '%s\n' "$REPLY"; fi
}

is_number_in_range() { # value min max
	case "$1" in ''|*[!0-9]*) return 1 ;; esac
	[ "$1" -ge "$2" ] && [ "$1" -le "$3" ]
}

# ---------------------------------------------------------------- JSON helpers (plutil)

# jget <keypath> <file>: raw value of a key path like 0.assets.1.name. Fails if missing.
jget() { plutil -extract "$1" raw -o - "$2" 2>/dev/null; }

# Id and version of a mod jar from its fabric.mod.json (plutil first, grep as a fallback).
jar_field() { # jar field
	local text val
	text="$(unzip -p "$1" fabric.mod.json 2>/dev/null)" || return 1
	[ -n "$text" ] || return 1
	val="$(printf '%s' "$text" | plutil -extract "$2" raw -o - - 2>/dev/null)"
	if [ -z "$val" ]; then
		val="$(printf '%s' "$text" | tr -d '\n\r' | grep -o "\"$2\"[[:space:]]*:[[:space:]]*\"[^\"]*\"" | head -1 | sed 's/.*:[[:space:]]*"\(.*\)"/\1/')"
	fi
	[ -n "$val" ] || return 1
	printf '%s' "$val"
}

# ---------------------------------------------------------------- releases

fetch_releases() {
	echo "Fetching the list of mods from GitHub..."
	if [ -n "${MCLAB_RELEASES:-}" ]; then
		cp "$MCLAB_RELEASES" "$RJSON" 2>/dev/null || { echo "Cannot read $MCLAB_RELEASES" >&2; exit 1; }
	else
		curl -fsSL -H 'Accept: application/vnd.github+json' -o "$RJSON" "$API_URL" || {
			echo "Could not download the release list from GitHub." >&2
			echo "Check your internet connection and try again." >&2
			exit 1
		}
	fi
	if ! jget "0.tag_name" "$RJSON" >/dev/null; then
		echo "No releases were found in $REPO." >&2
		exit 1
	fi
}

# Fills RT_* arrays, newest published first. One entry per non-draft release with a tag like <mod>-v<version>.
parse_releases() {
	local n=0 tag draft pub pre name mod ver
	: > "$TMP/rel.unsorted"
	while tag="$(jget "$n.tag_name" "$RJSON")"; do
		draft="$(jget "$n.draft" "$RJSON")"
		pub="$(jget "$n.published_at" "$RJSON")"
		pre="$(jget "$n.prerelease" "$RJSON")"
		name="$(jget "$n.name" "$RJSON")"
		mod="${tag%-v*}"
		ver="${tag##*-v}"
		if [ "$draft" != "true" ] && [ "$mod" != "$tag" ] && [ -n "$mod" ] && [ -n "$ver" ]; then
			printf '%s\037%s\037%s\037%s\037%s\037%s\037%s\n' "$pub" "$n" "$mod" "$ver" "$pre" "$name" "$tag" >> "$TMP/rel.unsorted"
		fi
		n=$((n + 1))
	done
	sort -r "$TMP/rel.unsorted" > "$TMP/rel.sorted"
	RT_IDX=(); RT_MOD=(); RT_VER=(); RT_PRE=(); RT_TITLE=()
	local p2 i2 m2 v2 pr2 nm2 t2
	while IFS="$SEP" read -r p2 i2 m2 v2 pr2 nm2 t2; do
		RT_IDX[${#RT_IDX[@]}]="$i2"
		RT_MOD[${#RT_MOD[@]}]="$m2"
		RT_VER[${#RT_VER[@]}]="$v2"
		RT_PRE[${#RT_PRE[@]}]="$pr2"
		if [ -n "$nm2" ]; then RT_TITLE[${#RT_TITLE[@]}]="${nm2% $v2}"; else RT_TITLE[${#RT_TITLE[@]}]="$m2"; fi
	done < "$TMP/rel.sorted"
	if [ "${#RT_IDX[@]}" -eq 0 ]; then
		echo "No mod releases were found in $REPO." >&2
		exit 1
	fi
}

# ---------------------------------------------------------------- profiles and installed mods

# find_profiles <minecraft>: fills P_NAME[] and P_DIR[] for Fabric profiles of that Minecraft version.
find_profiles() {
	P_NAME=(); P_DIR=()
	local f="$MC_ROOT/launcher_profiles.json" k lv gd nm
	[ -f "$f" ] || return 0
	# Profile keys can contain dots (the Essential installer names its profile
	# "Essential-installer-<timestamp with dots>"), which plutil key paths cannot address.
	# So read the whole "profiles" dict once as XML and walk it: one line per profile, key/lastVersionId/gameDir/name.
	plutil -extract profiles xml1 -o - "$f" 2>/dev/null | awk -v T="$TAB" '
		function unxml(s) { gsub(/&lt;/, "<", s); gsub(/&gt;/, ">", s); gsub(/&quot;/, "\"", s); gsub(/&apos;/, "\047", s); gsub(/&amp;/, "\\&", s); return s }
		function val(s) { sub(/^[\t ]*<string>/, "", s); sub(/<\/string>[\t ]*$/, "", s); return unxml(s) }
		function flush() { if (key != "") printf "%s\037%s\037%s\037%s\n", key, lv, gd, nm; key = lv = gd = nm = "" }
		index($0, T "<key>") == 1 && substr($0, 2, 1) != T { flush(); k = $0; sub(/^\t<key>/, "", k); sub(/<\/key>$/, "", k); key = unxml(k); want = ""; next }
		index($0, T T "<key>") == 1 && substr($0, 3, 1) != T { want = $0; sub(/^\t\t<key>/, "", want); sub(/<\/key>$/, "", want); next }
		index($0, T T "<string>") == 1 {
			if (want == "lastVersionId") lv = val($0); else if (want == "gameDir") gd = val($0); else if (want == "name") nm = val($0)
			want = ""; next
		}
		{ if (substr($0, 3, 1) != T) want = "" }
		END { flush() }
	' > "$TMP/profiles"
	while IFS="$SEP" read -r k lv gd nm; do
		case "$lv" in fabric-loader-*-"$1") ;; *) continue ;; esac
		[ -n "$nm" ] || nm="$k"
		[ -n "$gd" ] || gd="$MC_ROOT"
		P_NAME[${#P_NAME[@]}]="$nm"
		P_DIR[${#P_DIR[@]}]="$gd"
	done < "$TMP/profiles"
}

# choose_profile <minecraft>: sets PROFILE_NAME, PROFILE_MC and MODS_DIR. Returns 1 when the user gives up.
choose_profile() {
	local mc="$1" i
	find_profiles "$mc"
	if [ "${#P_NAME[@]}" -eq 0 ]; then
		echo
		echo "No Minecraft profile with Fabric Loader for Minecraft $mc was found."
		echo "Install Fabric Loader for Minecraft $mc first (https://fabricmc.net/use/installer/,"
		echo "or the Essential installer with Fabric $mc), then run this installer again."
		echo
		while :; do
			ask "Or type the full path of your mods folder (empty = exit): "
			REPLY="${REPLY#\"}"; REPLY="${REPLY%\"}"
			[ -n "$REPLY" ] || return 1
			case "$REPLY" in "~"*) REPLY="$HOME${REPLY#\~}" ;; esac
			if [ -d "$REPLY" ]; then
				PROFILE_NAME="(mods folder you typed)"; PROFILE_MC="$mc"; MODS_DIR="$REPLY"
				return 0
			fi
			echo "That folder does not exist. Try again."
		done
	fi
	if [ "${#P_NAME[@]}" -eq 1 ]; then
		i=0
	else
		echo
		echo "Minecraft profiles with Fabric for Minecraft $mc"
		i=0
		while [ "$i" -lt "${#P_NAME[@]}" ]; do
			printf '  %d) %s\n' "$((i + 1))" "${P_NAME[$i]}"
			i=$((i + 1))
		done
		echo "  0) Exit"
		while :; do
			ask "Choose a profile and press Enter: "
			is_number_in_range "$REPLY" 0 "${#P_NAME[@]}" && break
			echo "Please type a number from the list."
		done
		[ "$REPLY" -ne 0 ] || return 1
		i=$((REPLY - 1))
	fi
	PROFILE_NAME="${P_NAME[$i]}"
	PROFILE_MC="$mc"
	MODS_DIR="${P_DIR[$i]}/mods"
	mkdir -p "$MODS_DIR" || { echo "Cannot create $MODS_DIR" >&2; return 1; }
	return 0
}

# scan_installed: fills INST_ID[], INST_VER[] from the jars in $MODS_DIR.
scan_installed() {
	INST_ID=(); INST_VER=()
	local j id ver
	for j in "$MODS_DIR"/*.jar; do
		[ -f "$j" ] || continue
		id="$(jar_field "$j" id)" || continue
		ver="$(jar_field "$j" version)" || ver="?"
		INST_ID[${#INST_ID[@]}]="$id"
		INST_VER[${#INST_VER[@]}]="$ver"
	done
}

installed_version() { # mod id -> version or empty
	local i=0
	while [ "$i" -lt "${#INST_ID[@]}" ]; do
		if [ "${INST_ID[$i]}" = "$1" ]; then printf '%s' "${INST_VER[$i]}"; return 0; fi
		i=$((i + 1))
	done
	return 1
}

# Delete jars in $1 whose fabric.mod.json id equals $2. Nothing else is touched.
remove_mod_jars() {
	local j
	for j in "$1"/*.jar; do
		[ -f "$j" ] || continue
		if [ "$(jar_field "$j" id)" = "$2" ]; then
			rm -f "$j" && echo "  removed $(basename "$j")"
		fi
	done
}

# ---------------------------------------------------------------- install

asset_url() { # release index, asset name
	local a=0 nm
	while nm="$(jget "$1.assets.$a.name" "$RJSON")"; do
		if [ "$nm" = "$2" ]; then jget "$1.assets.$a.browser_download_url" "$RJSON"; return $?; fi
		a=$((a + 1))
	done
	return 1
}

# install_release <position in RT arrays>. Returns 1 on any problem; the mods folder is unchanged until the last step.
install_release() {
	local pos="$1" idx mod ver title work url jar want got mc fab fabjar fabsha
	idx="${RT_IDX[$pos]}"; mod="${RT_MOD[$pos]}"; ver="${RT_VER[$pos]}"; title="${RT_TITLE[$pos]}"
	work="$TMP/work"; rm -rf "$work"; mkdir -p "$work"

	if pgrep -f net.fabricmc.loader.impl.launch.knot.KnotClient >/dev/null 2>&1; then
		echo
		echo "Minecraft seems to be running. Close it before installing."
		ask "Press Enter when Minecraft is closed (or to continue anyway): "
	fi

	echo
	echo "Installing $title $ver"
	echo "Downloading release.json..."
	url="$(asset_url "$idx" "release.json")" || { err "ERROR: this release has no release.json."; return 1; }
	curl -fsSL -o "$work/release.json" "$url" || { err "ERROR: could not download release.json."; return 1; }
	jar="$(jget jar "$work/release.json")"; want="$(jget sha256 "$work/release.json")"
	mc="$(jget minecraft "$work/release.json")"; fab="$(jget fabric_api "$work/release.json")"
	if [ -z "$jar" ] || [ -z "$want" ]; then err "ERROR: release.json is incomplete."; return 1; fi
	case "$jar" in */*|*..*) err "ERROR: release.json has a bad jar name."; return 1 ;; esac

	if [ -n "$mc" ] && [ "$mc" != "$PROFILE_MC" ]; then
		echo "This version is for Minecraft $mc. Pick the matching profile."
		choose_profile "$mc" || return 1
		scan_installed
	fi

	echo "Downloading $jar..."
	url="$(asset_url "$idx" "$jar")" || { err "ERROR: the release has no file $jar."; return 1; }
	curl -fsSL -o "$work/$jar" "$url" || { err "ERROR: could not download $jar."; return 1; }

	echo "Verifying checksum (sha256)..."
	got="$(shasum -a 256 "$work/$jar" | cut -d' ' -f1)"
	want="$(printf '%s' "$want" | tr 'A-F' 'a-f')"
	if [ "$got" != "$want" ]; then
		rm -f "$work/$jar"
		err "ERROR: sha256 mismatch for $jar"
		echo "  expected $want" >&2
		echo "  got      $got" >&2
		echo "The download is damaged or was changed. Nothing was installed and your mods folder is untouched." >&2
		return 1
	fi
	echo "Checksum OK."

	if [ -z "$(installed_version fabric-api)" ] && [ -n "$fab" ]; then
		fabjar="fabric-api-$fab.jar"
		url="https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/$fab/$fabjar"
		echo "Fabric API is missing. Downloading $fabjar..."
		curl -fsSL -o "$work/$fabjar" "$url" || { err "ERROR: could not download Fabric API."; return 1; }
		fabsha="$(curl -fsSL "$url.sha1" | tr -d ' \r\n')"
		got="$(shasum -a 1 "$work/$fabjar" | cut -d' ' -f1)"
		if [ -z "$fabsha" ] || [ "$got" != "$fabsha" ]; then
			err "ERROR: sha1 mismatch for Fabric API. Nothing was installed."
			return 1
		fi
		cp "$work/$fabjar" "$MODS_DIR/" || { err "ERROR: cannot write to $MODS_DIR"; return 1; }
		echo "Installed $fabjar."
	fi

	echo "Removing old versions of $title..."
	remove_mod_jars "$MODS_DIR" "$mod"
	echo "Copying $jar to the mods folder..."
	cp "$work/$jar" "$MODS_DIR/$jar" || { err "ERROR: cannot write to $MODS_DIR"; return 1; }
	echo
	printf '%sSUCCESS! %s %s is installed.%s\n' "$GREEN" "$title" "$ver" "$RESET"
	echo "Mods folder: $MODS_DIR"
	echo "Start Minecraft with the \"$PROFILE_NAME\" profile."
	return 0
}

# ---------------------------------------------------------------- menus

# Prints the distinct mod ids (newest first) into MOD_IDS[] and titles into MOD_TITLES[].
list_mods() {
	MOD_IDS=(); MOD_TITLES=()
	local i=0 seen=" "
	while [ "$i" -lt "${#RT_MOD[@]}" ]; do
		case "$seen" in *" ${RT_MOD[$i]} "*) ;; *)
			seen="$seen${RT_MOD[$i]} "
			MOD_IDS[${#MOD_IDS[@]}]="${RT_MOD[$i]}"
			MOD_TITLES[${#MOD_TITLES[@]}]="${RT_TITLE[$i]}"
		;; esac
		i=$((i + 1))
	done
}

versions_menu() { # mod id, title
	local mod="$1" title="$2" pos i shown inst label
	while :; do
		inst="$(installed_version "$mod")"
		echo
		echo "$title"
		VPOS=(); i=0; shown=0
		while [ "$i" -lt "${#RT_MOD[@]}" ] && [ "$shown" -lt 6 ]; do
			if [ "${RT_MOD[$i]}" = "$mod" ]; then
				shown=$((shown + 1))
				VPOS[${#VPOS[@]}]="$i"
				label="${RT_VER[$i]}"
				[ "${RT_PRE[$i]}" = "true" ] && label="$label  (pre-release)"
				[ -n "$inst" ] && [ "$inst" = "${RT_VER[$i]}" ] && label="$label  (Installed)"
				printf '  %d) %s\n' "$shown" "$label"
			fi
			i=$((i + 1))
		done
		echo "  0) Back"
		ask "Choose a version and press Enter: "
		if is_number_in_range "$REPLY" 0 "${#VPOS[@]}"; then
			[ "$REPLY" -ne 0 ] || return 0
			pos="${VPOS[$((REPLY - 1))]}"
			install_release "$pos" || INSTALL_FAILED=1
			scan_installed
			# Do not jump straight back into a menu: let the player read the result first.
			echo
			echo "  1) Back to the version list"
			echo "  0) Exit"
			while :; do
				ask "Choose and press Enter: "
				case "$REPLY" in
					1) continue 2 ;;
					0) exit "$INSTALL_FAILED" ;;
				esac
				echo "Please type 1 or 0."
			done
		fi
		echo "Please type a number from the list."
	done
}

mods_menu() {
	local i width inst
	while :; do
		list_mods
		width=0; i=0
		while [ "$i" -lt "${#MOD_TITLES[@]}" ]; do
			[ "${#MOD_TITLES[$i]}" -gt "$width" ] && width="${#MOD_TITLES[$i]}"
			i=$((i + 1))
		done
		echo
		echo "Minecraft Lab mods  (profile: $PROFILE_NAME)"
		i=0
		while [ "$i" -lt "${#MOD_IDS[@]}" ]; do
			inst="$(installed_version "${MOD_IDS[$i]}")"
			if [ -n "$inst" ]; then
				printf '  %d) %-*s   installed %s\n' "$((i + 1))" "$width" "${MOD_TITLES[$i]}" "$inst"
			else
				printf '  %d) %s\n' "$((i + 1))" "${MOD_TITLES[$i]}"
			fi
			i=$((i + 1))
		done
		echo "  0) Exit"
		ask "Choose a mod and press Enter: "
		if is_number_in_range "$REPLY" 0 "${#MOD_IDS[@]}"; then
			[ "$REPLY" -ne 0 ] || return 0
			versions_menu "${MOD_IDS[$((REPLY - 1))]}" "${MOD_TITLES[$((REPLY - 1))]}"
		else
			echo "Please type a number from the list."
		fi
	done
}

# ---------------------------------------------------------------- main

if [ "$(uname -s)" != "Darwin" ]; then
	echo "This installer is for macOS. On Windows use minecraft-lab.bat." >&2
	exit 1
fi
command -v plutil >/dev/null 2>&1 || { echo "plutil not found (needs macOS 12 or newer)." >&2; exit 1; }

echo "Minecraft Lab mod installer"
fetch_releases
parse_releases
choose_profile "$DEFAULT_MC" || exit 1
scan_installed
INSTALL_FAILED=0
mods_menu
exit "$INSTALL_FAILED"   # non-zero if any install failed during this session
