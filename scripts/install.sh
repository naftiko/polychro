#!/bin/sh
# Copyright 2026 Naftiko
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
# in compliance with the License. You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software distributed under the License
# is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
# or implied. See the License for the specific language governing permissions and limitations under
# the License.
# ============================================================
# Polychro CLI installer.
#
# Usage:
#   curl -fsSL https://raw.githubusercontent.com/naftiko/polychro/main/scripts/install.sh | sh
#
# To pin the *installer script itself* to a specific release (protects a
# scripted/automated install from a future change to this script,
# fetch it from the frozen per-tag path instead of
# the main-branch copy above:
#   curl -fsSL https://raw.githubusercontent.com/naftiko/polychro/v1.0.0/scripts/install.sh | sh
#
# This is independent from POLYCHRO_VERSION below, which pins the *polychro
# package* being installed - the two can be combined, e.g. to reproduce an
# install exactly as it happened at v1.0.0 (same installer logic, same
# package):
#   curl -fsSL https://raw.githubusercontent.com/naftiko/polychro/v1.0.0/scripts/install.sh | POLYCHRO_VERSION=v1.0.0 sh
#
# Env overrides:
#   POLYCHRO_VERSION   Install a specific release tag (e.g. "v1.0.0-beta7").
#                      Defaults to the latest GitHub release, including
#                      pre-releases.
#   POLYCHRO_HOME      Install root. Defaults to "$HOME/.polychro".
#   POLYCHRO_NO_MODIFY_RC=1   Skip editing the shell rc file (PATH must then
#                             be configured manually - see printed instructions).
#   POLYCHRO_BASE_URL  Override the base download URL (asset and checksum are
#                      fetched from "$POLYCHRO_BASE_URL/<asset>"). Defaults to
#                      the real GitHub Releases URL for $POLYCHRO_VERSION.
#
# What it does:
#   1. Detects OS/arch and downloads the matching tarball (a jlink runtime,
#      polychro.jar and its lib/ folder) from a GitHub Release (naftiko/polychro).
#   2. Verifies its SHA-256 against the release's per-asset checksum file.
#   3. Extracts it to a versioned directory under $POLYCHRO_HOME/versions/
#      and symlinks $POLYCHRO_HOME/current to it.
#   4. Writes a thin wrapper script at $POLYCHRO_HOME/bin/polychro.
#   5. Idempotently appends a PATH export to the caller's shell rc file.
#
# Nothing is written outside $POLYCHRO_HOME and the shell rc file - no /opt,
# no /usr/local, no admin/root privileges are ever required, and every
# account on a shared machine gets its own independent, fully isolated
# install (unlike Homebrew, which uses one shared, single-owner prefix).
# ============================================================

set -eu

REPO="naftiko/polychro"
POLYCHRO_HOME="${POLYCHRO_HOME:-$HOME/.polychro}"
POLYCHRO_VERSION="${POLYCHRO_VERSION:-}"
POLYCHRO_NO_MODIFY_RC="${POLYCHRO_NO_MODIFY_RC:-0}"
POLYCHRO_BASE_URL="${POLYCHRO_BASE_URL:-}"

info()  { printf '\033[1;34m==>\033[0m %s\n' "$1"; }
warn()  { printf '\033[1;33m[warn]\033[0m %s\n' "$1"; }
fail()  { printf '\033[1;31m[error]\033[0m %s\n' "$1" >&2; exit 1; }

# ------------------------------------------------------------------
# 1. Detect OS/arch and map to a release asset name.
# ------------------------------------------------------------------
os_raw=$(uname -s)
arch_raw=$(uname -m)

case "$os_raw" in
  Darwin) os="macos" ;;
  Linux)  os="linux" ;;
  *) fail "Unsupported OS: $os_raw. On Windows, use install.ps1." ;;
esac

case "$arch_raw" in
  arm64|aarch64) arch="arm64" ;;
  x86_64|amd64)  arch="amd64" ;;
  *) fail "Unsupported architecture: $arch_raw" ;;
esac

asset="polychro-cli-${os}-${arch}.tar.gz"
checksum="$asset.sha256"

# ------------------------------------------------------------------
# 2. Resolve the version/tag to install.
# ------------------------------------------------------------------
if [ -z "$POLYCHRO_VERSION" ]; then
  # Note: /releases (unlike /releases/latest) includes pre-releases - this is
  # intentional while the project is entirely pre-1.0.0.
  info "Resolving latest release of $REPO (including pre-releases)..."
  POLYCHRO_VERSION=$(curl -fsSL "https://api.github.com/repos/$REPO/releases" \
    | grep '"tag_name"' | head -n1 | sed -E 's/.*"tag_name": *"([^"]+)".*/\1/')
  [ -n "$POLYCHRO_VERSION" ] || fail "Could not resolve the latest release tag from the GitHub API."
fi

info "Installing polychro $POLYCHRO_VERSION ($os/$arch) into $POLYCHRO_HOME"

if [ -n "$POLYCHRO_BASE_URL" ]; then
  base_url="$POLYCHRO_BASE_URL"
else
  base_url="https://github.com/$REPO/releases/download/$POLYCHRO_VERSION"
fi

# ------------------------------------------------------------------
# 3. Download + verify checksum in a scratch temp dir.
# ------------------------------------------------------------------
tmp_dir=$(mktemp -d)
trap 'rm -rf "$tmp_dir"' EXIT

info "Downloading $asset..."
curl -fsSL "$base_url/$asset" -o "$tmp_dir/$asset" \
  || fail "Download failed: $base_url/$asset (does this version/platform combination exist?)"

info "Verifying checksum..."
curl -fsSL "$base_url/$checksum" -o "$tmp_dir/$checksum" \
  || fail "Could not download $checksum for verification."

expected_sha=$(grep "  ${asset}\$" "$tmp_dir/$checksum" | awk '{print $1}')
[ -n "$expected_sha" ] || fail "No checksum entry found for $asset in $checksum."

if command -v shasum >/dev/null 2>&1; then
  actual_sha=$(shasum -a 256 "$tmp_dir/$asset" | awk '{print $1}')
elif command -v sha256sum >/dev/null 2>&1; then
  actual_sha=$(sha256sum "$tmp_dir/$asset" | awk '{print $1}')
else
  fail "Neither shasum nor sha256sum is available to verify the download."
fi

[ "$expected_sha" = "$actual_sha" ] \
  || fail "Checksum mismatch for $asset (expected $expected_sha, got $actual_sha). Aborting."

# ------------------------------------------------------------------
# 4. Extract to a versioned directory and symlink "current".
# ------------------------------------------------------------------
version_dir=${POLYCHRO_VERSION#v}
case "$version_dir" in
 ""|"."|".."|*[!A-Za-z0-9._+-]*) fail "Invalid POLYCHRO_VERSION: $POLYCHRO_VERSION" ;;
esac
# Use an absolute install root from here on: the "current" symlink, the
# wrapper and the PATH line must not depend on the directory the installer
# was run from.
mkdir -p "$POLYCHRO_HOME"
POLYCHRO_HOME=$(CDPATH='' cd -- "$POLYCHRO_HOME" && pwd) \
  || fail "Cannot use POLYCHRO_HOME: $POLYCHRO_HOME"
install_dir="$POLYCHRO_HOME/versions/$version_dir"
rm -rf "$install_dir"
mkdir -p "$install_dir"
tar xzf "$tmp_dir/$asset" -C "$install_dir"

mkdir -p "$POLYCHRO_HOME/bin"
ln -sfn "$install_dir" "$POLYCHRO_HOME/current"

# ------------------------------------------------------------------
# 5. Write the launcher wrapper.
# ------------------------------------------------------------------
# The wrapper remembers this install's POLYCHRO_HOME: the variable only exists
# while the installer runs, and the rc file only gets the PATH line. Without
# this, a custom POLYCHRO_HOME fell back to ~/.polychro in every new shell (naftiko/ikanos#805).
# The path is written single-quoted, so spaces, $, ` and ' in it stay literal.
quoted_home=$(printf '%s' "$POLYCHRO_HOME" | sed "s/'/'\\\\''/g")
cat > "$POLYCHRO_HOME/bin/polychro" <<WRAPPER
#!/bin/sh
# Generated by polychro's install.sh - do not edit by hand.
# A POLYCHRO_HOME set at run time still wins over the install location.
polychro_install_home='$quoted_home'
POLYCHRO_HOME="\${POLYCHRO_HOME:-\$polychro_install_home}"
# polychro.jsa is the class cache (AppCDS) written at install time. It is only read here:
# a missing, stale or damaged cache (VerifySharedSpaces) is skipped without a message.
exec "\$POLYCHRO_HOME/current/jre/bin/java" -XX:+VerifySharedSpaces \\
  -XX:SharedArchiveFile="\$POLYCHRO_HOME/current/polychro.jsa" "-Xlog:cds*=off" \\
  -jar "\$POLYCHRO_HOME/current/polychro.jar" "\$@"
WRAPPER
chmod +x "$POLYCHRO_HOME/bin/polychro"

# Class cache: one lint that uses a JSON Schema and a JavaScript rule function, run with
# ArchiveClassesAtExit, writes the classes it loaded to polychro.jsa. Later runs map that file
# instead of loading about 2,500 classes from the jars. The lint result does not matter.
info "Preparing the start-up cache..."
warm_up_dir="$tmp_dir/warm-up"
mkdir -p "$warm_up_dir/functions"
printf 'name: warm-up\n' > "$warm_up_dir/doc.yaml"
printf '{"type":"object","required":["name"]}\n' > "$warm_up_dir/schema.json"
printf 'export default function (input) {\n  return input === "warm-up" ? [] : [{ message: "unexpected" }];\n}\n' \
  > "$warm_up_dir/functions/warm_up.js"
printf 'functionsDir: functions\nfunctions: [warm_up]\nrules:\n  warm-up:\n    message: "{{error}}"\n    severity: info\n    given: "$.name"\n    then:\n      function: warm_up\n' \
  > "$warm_up_dir/ruleset.yml"
# Run through "current", the path the launcher uses, so the cache matches its class path.
current_dir="$POLYCHRO_HOME/current"
# The user's JVM options (an agent, -Xshare:off...) must not shape the cache.
(unset JAVA_TOOL_OPTIONS _JAVA_OPTIONS JDK_JAVA_OPTIONS
  cd "$warm_up_dir" && "$current_dir/jre/bin/java" -XX:ArchiveClassesAtExit="$current_dir/polychro.jsa" \
  -jar "$current_dir/polychro.jar" lint doc.yaml -s schema.json -r ruleset.yml) >/dev/null 2>&1 || true
[ -s "$install_dir/polychro.jsa" ] || warn "Could not prepare the start-up cache; polychro will start more slowly."

info "Installed polychro $POLYCHRO_VERSION -> $install_dir"

# ------------------------------------------------------------------
# 6. Wire up PATH (idempotent).
# ------------------------------------------------------------------
bin_dir="$POLYCHRO_HOME/bin"
marker="# >>> polychro-cli PATH (managed by install.sh, safe to remove) >>>"
marker_end="# <<< polychro-cli PATH <<<"

detect_rc_file() {
  case "${SHELL:-}" in
    */zsh)  echo "$HOME/.zshrc" ;;
    */bash) [ -f "$HOME/.bash_profile" ] && echo "$HOME/.bash_profile" || echo "$HOME/.bashrc" ;;
    */fish) echo "$HOME/.config/fish/config.fish" ;;
    *)      echo "$HOME/.profile" ;;
  esac
}

rc_file=$(detect_rc_file)
# bin_dir is single-quoted, like in the wrapper, so the shell never expands $
# or ` from the path at startup. fish escapes \ and ' inside single quotes.
if [ "$rc_file" = "$HOME/.config/fish/config.fish" ]; then
  quoted_bin=$(printf '%s' "$bin_dir" | sed -e 's/\\/\\\\/g' -e "s/'/\\\\'/g")
  path_line="set -gx PATH '$quoted_bin' \$PATH"
else
  quoted_bin=$(printf '%s' "$bin_dir" | sed "s/'/'\\\\''/g")
  path_line="export PATH='$quoted_bin':\"\$PATH\""
fi

if [ "$POLYCHRO_NO_MODIFY_RC" = "1" ]; then
  warn "POLYCHRO_NO_MODIFY_RC=1 set - skipping shell rc update."
  info "Add this to your shell config manually: $path_line"
else
  if [ -f "$rc_file" ] && grep -qF "$marker" "$rc_file" 2>/dev/null && grep -qF "$marker_end" "$rc_file" 2>/dev/null; then
    # A marker block exists, but it may have been written by a previous
    # install with a different POLYCHRO_HOME (and therefore a different
    # bin_dir/path_line). Compare the block's actual content, not just the
    # marker's presence, before declaring the PATH already configured -
    # otherwise a reinstall into a new POLYCHRO_HOME silently leaves the old
    # bin directory on PATH while reporting success.
    existing_path_line=$(awk -v m="$marker" -v me="$marker_end" '
      $0==m {inblock=1; next}
      $0==me {inblock=0}
      inblock {print; exit}
    ' "$rc_file")
    if [ "$existing_path_line" = "$path_line" ]; then
      info "PATH already configured in $rc_file"
    else
      info "Updating polychro PATH entry in $rc_file"
      tmp_rc=$(mktemp)
      # The new line goes through the environment: awk -v would turn the
      # backslashes of a fish-escaped path into escape sequences.
      polychro_path_line="$path_line" awk -v m="$marker" -v me="$marker_end" '
        $0==m {print; print ENVIRON["polychro_path_line"]; inblock=1; next}
        $0==me {print; inblock=0; next}
        inblock {next}
        {print}
      ' "$rc_file" > "$tmp_rc"
      cat "$tmp_rc" > "$rc_file"
      rm -f "$tmp_rc"
    fi
  else
    info "Adding polychro to PATH in $rc_file"
    mkdir -p "$(dirname "$rc_file")"
    {
      printf '\n%s\n' "$marker"
      printf '%s\n' "$path_line"
      printf '%s\n' "$marker_end"
    } >> "$rc_file"
  fi
fi

echo ""
info "Done. Open a new shell, or run:"
echo "    $path_line"
echo "Then verify with:"
echo "    polychro --version"
