#!/usr/bin/env bash
# Shared settings for install.sh / package.sh.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MC_DIR="${MC_DIR:-$HOME/Library/Application Support/minecraft}"
PROFILE_DIR="${PROFILE_DIR:-$MC_DIR/installations/26.3 Fabric Essential}"
MODS_DIR="$PROFILE_DIR/mods"

FABRIC_API_VERSION="$(grep '^fabric_api_version=' "$ROOT/gradle.properties" | cut -d= -f2)"
FABRIC_API_JAR="fabric-api-${FABRIC_API_VERSION}.jar"
FABRIC_API_URL="https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/${FABRIC_API_VERSION}/${FABRIC_API_JAR}"

JAVA_HOME="$(/usr/libexec/java_home -v 25)" || { echo "JDK 25 not found (see docs/system/00-environment.md)" >&2; exit 1; }
export JAVA_HOME

# All mod ids under mods/ (or the ones given as arguments).
mod_ids() {
	if [ "$#" -gt 0 ]; then printf '%s\n' "$@"; return; fi
	for d in "$ROOT"/mods/*/; do
		n="$(basename "$d")"
		[ -f "$d/gradle.properties" ] && [[ "$n" != _* ]] && echo "$n"
	done
}

# Value of a key in mods/<id>/gradle.properties (empty if missing).
mod_prop() {
	{ grep "^$2=" "$ROOT/mods/$1/gradle.properties" || true; } | head -1 | cut -d= -f2-
}

# Path of the built jar for a mod id: mods/<id>/build/libs/<mod_file or id>-<version>.jar
mod_jar() {
	local id="$1" ver file
	ver="$(mod_prop "$id" mod_version)"
	file="$(mod_prop "$id" mod_file)"
	echo "$ROOT/mods/$id/build/libs/${file:-$id}-${ver}.jar"
}

# Mod id declared in a jar's fabric.mod.json (empty if none).
jar_mod_id() {
	{ unzip -p "$1" fabric.mod.json 2>/dev/null || true; } | tr -d '\n\r' \
		| { grep -o '"id"[[:space:]]*:[[:space:]]*"[^"]*"' || true; } | head -1 \
		| sed 's/.*:[[:space:]]*"\(.*\)"/\1/'
}

# Delete jars in <dir> whose fabric.mod.json id equals <mod_id>; other jars are never touched.
remove_mod_jars() {
	local dir="$1" id="$2" j
	for j in "$dir"/*.jar; do
		[ -f "$j" ] || continue
		if [ "$(jar_mod_id "$j")" = "$id" ]; then rm -f "$j"; fi
	done
}

# Download Fabric API into $1 (a directory) if missing, verifying its sha1 from Maven.
fetch_fabric_api() {
	local dest="$1" cache="$ROOT/.cache"
	mkdir -p "$cache"
	if [ ! -f "$cache/$FABRIC_API_JAR" ]; then
		curl -sSfL -o "$cache/$FABRIC_API_JAR" "$FABRIC_API_URL"
		local want got
		want="$(curl -sSfL "$FABRIC_API_URL.sha1")"
		got="$(shasum -a 1 "$cache/$FABRIC_API_JAR" | cut -d' ' -f1)"
		if [ "$want" != "$got" ]; then rm -f "$cache/$FABRIC_API_JAR"; echo "Fabric API sha1 mismatch" >&2; exit 1; fi
	fi
	cp "$cache/$FABRIC_API_JAR" "$dest/$FABRIC_API_JAR"
}
