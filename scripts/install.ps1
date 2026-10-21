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
# Polychro CLI installer for Windows.
#
# Usage (run in PowerShell):
#   irm https://raw.githubusercontent.com/naftiko/polychro/main/scripts/install.ps1 | iex
#
# To pin the *installer script itself* to a specific release (protects a
# scripted/automated install from a future change to this script,
# fetch it from the frozen per-tag path instead of
# the main-branch copy above:
#   irm https://raw.githubusercontent.com/naftiko/polychro/v1.0.0/scripts/install.ps1 | iex
#
# This is independent from POLYCHRO_VERSION below, which pins the *polychro
# package* being installed — the two can be combined, e.g. to reproduce an
# install exactly as it happened at v1.0.0 (same installer logic, same
# package):
#   $env:POLYCHRO_VERSION = "v1.0.0"; irm https://raw.githubusercontent.com/naftiko/polychro/v1.0.0/scripts/install.ps1 | iex
#
# Env overrides (set as environment variables before running, or
# $env:POLYCHRO_VERSION = "v1.0.0-beta7" in the same session):
#   POLYCHRO_VERSION        Install a specific release tag. Defaults to the
#                           latest release, including pre-releases.
#   POLYCHRO_HOME           Install root. Defaults to "$env:USERPROFILE\.polychro".
#   POLYCHRO_NO_MODIFY_PATH Set to "1" to skip editing the user PATH (must then
#                           be configured manually — see printed instructions).
#   POLYCHRO_BASE_URL       Override the base download URL (asset and checksum
#                           are fetched from "$POLYCHRO_BASE_URL/<asset>"). Defaults
#                           to the real GitHub Releases URL for $PolychroVersion.
#
# What it does:
#   1. Detects CPU arch and downloads the matching zip (a jlink runtime,
#      polychro.jar and its lib\ folder) from a GitHub Release (naftiko/polychro).
#   2. Verifies its SHA-256 against the release's per-asset checksum file.
#   3. Extracts it to a versioned directory under $POLYCHRO_HOME\versions\
#      and repoints $POLYCHRO_HOME\current at it (junction, no admin needed —
#      unlike a symbolic link, which requires elevation or Developer Mode).
#   4. Writes a thin wrapper at $POLYCHRO_HOME\bin\polychro.cmd.
#   5. Idempotently prepends $POLYCHRO_HOME\bin to the *user* PATH
#      (HKCU, not HKLM/Machine) — this never requires an elevated prompt.
#
# Nothing is written outside $POLYCHRO_HOME and the user's own PATH registry
# value — no Program Files, no admin/UAC prompt, and every account on a
# shared machine gets its own independent, fully isolated install (same
# model as scripts/install.sh on macOS/Linux).
# ============================================================

#Requires -Version 5.1
$OriginalErrorActionPreference = $ErrorActionPreference
$ErrorActionPreference = "Stop"

$OriginalProgressPreference = $ProgressPreference
$ProgressPreference = "SilentlyContinue"

$Repo = "naftiko/polychro"
$PolychroHome = if ($env:POLYCHRO_HOME) { $env:POLYCHRO_HOME } else { Join-Path $env:USERPROFILE ".polychro" }
# Absolute from here on: the wrapper and the PATH entry must not depend on the
# directory the installer was run from.
$PolychroHome = $ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($PolychroHome)
$PolychroVersion = $env:POLYCHRO_VERSION
$NoModifyPath = $env:POLYCHRO_NO_MODIFY_PATH -eq "1"
$BaseUrlOverride = $env:POLYCHRO_BASE_URL

function Write-Info  { param($Message) Write-Host "==> $Message" -ForegroundColor Cyan }
function Write-Warn  { param($Message) Write-Host "[warn] $Message" -ForegroundColor Yellow }
function Write-Fail  { param($Message) Write-Error "[error] $Message" -ErrorAction Stop }

try {
  # ------------------------------------------------------------------
  # 1. Detect arch and map to a release asset name. Windows-only script,
  #    so the OS component of the asset name is fixed at "windows".
  # ------------------------------------------------------------------
  $archRaw = $env:PROCESSOR_ARCHITECTURE
  switch -Regex ($archRaw) {
    "ARM64"        { $arch = "arm64" }
    "AMD64|x86_64" { $arch = "amd64" }
    default        { Write-Fail "Unsupported architecture: $archRaw" }
  }

  $asset = "polychro-cli-windows-$arch.zip"
  $checksum = "$asset.sha256"

  # ------------------------------------------------------------------
  # 2. Resolve the version/tag to install.
  # ------------------------------------------------------------------
  if (-not $PolychroVersion) {
    # Note: /releases (unlike /releases/latest) includes pre-releases — this is
    # intentional while the project is entirely pre-1.0.0.
    Write-Info "Resolving latest release of $Repo (including pre-releases)..."
    $releases = Invoke-RestMethod -UseBasicParsing "https://api.github.com/repos/$Repo/releases"
    $PolychroVersion = $releases[0].tag_name
    if (-not $PolychroVersion) { Write-Fail "Could not resolve the latest release tag from the GitHub API." }
  }

  Write-Info "Installing polychro $PolychroVersion (windows/$arch) into $PolychroHome"

  $baseUrl = if ($BaseUrlOverride) { $BaseUrlOverride } else { "https://github.com/$Repo/releases/download/$PolychroVersion" }

  # ------------------------------------------------------------------
  # 3. Download + verify checksum in a scratch temp dir.
  # ------------------------------------------------------------------
  $tmpDir = Join-Path $env:TEMP ("polychro-install-" + [System.Guid]::NewGuid().ToString("N"))
  New-Item -ItemType Directory -Path $tmpDir -Force | Out-Null
  try {
    $assetPath = Join-Path $tmpDir $asset
    Write-Info "Downloading $asset..."
    try {
      Invoke-WebRequest -UseBasicParsing -Uri "$baseUrl/$asset" -OutFile $assetPath
    } catch {
      Write-Fail "Download failed: $baseUrl/$asset (does this version/platform combination exist?)"
    }

    Write-Info "Verifying checksum..."
    $checksumsPath = Join-Path $tmpDir $checksum
    try {
      Invoke-WebRequest -UseBasicParsing -Uri "$baseUrl/$checksum" -OutFile $checksumsPath
    } catch {
      Write-Fail "Could not download $checksum for verification."
    }

    $checksumLine = Select-String -Path $checksumsPath -Pattern "  $([regex]::Escape($asset))$" | Select-Object -First 1
    if (-not $checksumLine) { Write-Fail "No checksum entry found for $asset in $checksum." }
    $expectedSha = ($checksumLine.Line -split "\s+")[0]

    $actualSha = (Get-FileHash -Path $assetPath -Algorithm SHA256).Hash.ToLower()
    if ($expectedSha.ToLower() -ne $actualSha) {
      Write-Fail "Checksum mismatch for $asset (expected $expectedSha, got $actualSha). Aborting."
    }

    # ------------------------------------------------------------------
    # 4. Extract to a versioned directory and repoint "current".
    # ------------------------------------------------------------------
    $versionDir = $PolychroVersion -replace '^v', ''
    $installDir = Join-Path $PolychroHome "versions\$versionDir"
    if ([string]::IsNullOrWhiteSpace($versionDir) -or $versionDir -in @(".", "..") -or $versionDir -notmatch "^[A-Za-z0-9._+-]+$") {
      Write-Fail "Invalid POLYCHRO_VERSION: $PolychroVersion"
    }
    if (Test-Path $installDir) { Remove-Item -Recurse -Force $installDir }
    New-Item -ItemType Directory -Path $installDir -Force | Out-Null

    Expand-Archive -Path $assetPath -DestinationPath $installDir -Force

    $binDir = Join-Path $PolychroHome "bin"
    New-Item -ItemType Directory -Path $binDir -Force | Out-Null

    $currentLink = Join-Path $PolychroHome "current"
    if (Test-Path $currentLink) { Remove-Item -Recurse -Force $currentLink }
    # A directory junction (not a symbolic link) is used deliberately: on
    # Windows, creating a symbolic link requires either an elevated prompt
    # or Developer Mode enabled, while a junction needs neither — it is the
    # only link type NTFS allows any unprivileged user to create.
    New-Item -ItemType Junction -Path $currentLink -Target $installDir | Out-Null

    # ------------------------------------------------------------------
    # 5. Write the launcher wrapper.
    # ------------------------------------------------------------------
    # The wrapper finds its install root at run time, as the parent of its own
    # folder (%~dp0..): the POLYCHRO_HOME variable only exists while the
    # installer runs, so a custom POLYCHRO_HOME used to fall back to
    # %USERPROFILE%\.polychro in every new terminal (naftiko/ikanos#805). No path
    # is written into the file: cmd.exe reads batch files in the console's OEM code page,
    # not UTF-8, so a literal path with non-ASCII characters (an accented user
    # name) would be garbled. The file is pure ASCII and holds no path.
    $wrapperPath = Join-Path $binDir "polychro.cmd"
    $wrapperContent = @"
@echo off
rem Generated by polychro's install.ps1 - do not edit by hand.
rem The install root is the parent of this folder. A POLYCHRO_HOME set at run time still wins.
if not defined POLYCHRO_HOME for %%I in ("%~dp0..") do set "POLYCHRO_HOME=%%~fI"
"%POLYCHRO_HOME%\current\jre\bin\java.exe" -jar "%POLYCHRO_HOME%\current\polychro.jar" %*
"@
    [System.IO.File]::WriteAllText($wrapperPath, $wrapperContent, [System.Text.Encoding]::ASCII)

    Write-Info "Installed polychro $PolychroVersion -> $installDir"

    # ------------------------------------------------------------------
    # 6. Wire up PATH (idempotent, user-scope only — no admin required).
    # ------------------------------------------------------------------
    if ($NoModifyPath) {
      Write-Warn "POLYCHRO_NO_MODIFY_PATH=1 set — skipping PATH update."
      Write-Info "Add this directory to your PATH manually: $binDir"
    } else {
      $userPath = [Environment]::GetEnvironmentVariable("Path", "User")
      $pathEntries = @()
      if ($userPath) { $pathEntries = $userPath -split ";" | Where-Object { $_ -ne "" } }

      if ($pathEntries -contains $binDir) {
        Write-Info "PATH already configured (user scope)"
      } else {
        Write-Info "Adding $binDir to the user PATH"
        $newPath = if ($userPath) { "$binDir;$userPath" } else { $binDir }
        [Environment]::SetEnvironmentVariable("Path", $newPath, "User")
      }

      # Update the current session's PATH too, regardless of whether the
      # persistent user-scope PATH needed changing above — a prior install
      # may have updated the registry after this terminal session started,
      # so "already configured (user scope)" does not imply this process's
      # $env:Path already has it. Idempotent: skip if already present.
      $sessionPaths = $env:Path -split ";" | Where-Object { $_ -ne "" }
      if ($sessionPaths -notcontains $binDir) {
        $env:Path = "$binDir;$env:Path"
      }
    }

    Write-Host ""
    if ($NoModifyPath) {
      Write-Warn "Neither the persistent user PATH nor this terminal session's PATH were updated."
      Write-Info "To use polychro in this session, run:"
      Write-Host "    `$env:Path = `"$binDir;`$env:Path`""
      Write-Info "To use it permanently, add $binDir to your PATH manually."
    } else {
      Write-Info "Done. This session's PATH already includes polychro; new terminals will pick it up too."
    }
    Write-Host "Verify with:"
    Write-Host "    polychro --version"
  } finally {
    Remove-Item -Recurse -Force $tmpDir -ErrorAction SilentlyContinue
  }
} finally {
  if ($OriginalProgressPreference) { $ProgressPreference = $OriginalProgressPreference }
  $ErrorActionPreference = $OriginalErrorActionPreference
}
