# Minecraft Lab mod installer for Windows (Windows PowerShell 5.1 or newer).
# Run it through install-mods.bat, or paste in PowerShell:
#   irm https://raw.githubusercontent.com/huydangtruong/minecraft-lab/main/install-mods.ps1 | iex
#
# Test overrides (environment variables): MCLAB_RELEASES (local releases JSON instead of the GitHub API),
# MC_DIR (Minecraft root folder).
# Keep this file ASCII-only and free of PowerShell 7 syntax (no ??, ?:, &&, -Parallel).

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
Add-Type -AssemblyName System.IO.Compression.FileSystem

$Repo = 'huydangtruong/minecraft-lab'
$ApiUrl = "https://api.github.com/repos/$Repo/releases?per_page=100"
$DefaultMc = '26.3'
if ($env:MC_DIR) { $McRoot = $env:MC_DIR } else { $McRoot = Join-Path $env:APPDATA '.minecraft' }

$script:ProfileName = ''
$script:ProfileMc = ''
$script:ModsDir = ''
$script:Installed = @()
$script:Releases = @()
$script:Failed = $false

# ---------------------------------------------------------------- helpers

function Ask([string]$prompt) {
    $answer = Read-Host $prompt
    if ($null -eq $answer) { $answer = '' }
    return $answer.Trim()
}

function Test-Choice([string]$text, [int]$min, [int]$max) {
    $n = 0
    if (-not [int]::TryParse($text, [ref]$n)) { return $false }
    return ($n -ge $min -and $n -le $max)
}

# Download a URL to a file. file:// URLs are copied (used by tests).
function Get-File([string]$url, [string]$dest) {
    if ($url.StartsWith('file://')) {
        Copy-Item -LiteralPath ([Uri]$url).LocalPath -Destination $dest -Force
    } else {
        Invoke-WebRequest -UseBasicParsing -Uri $url -OutFile $dest -Headers @{ 'User-Agent' = 'minecraft-lab-installer' }
    }
}

function Get-Text([string]$url) {
    $tmp = [IO.Path]::GetTempFileName()
    try {
        Get-File $url $tmp
        return [IO.File]::ReadAllText($tmp)
    } finally {
        Remove-Item -LiteralPath $tmp -Force -ErrorAction SilentlyContinue
    }
}

function ConvertTo-Utc($value) {
    if ($value -is [datetime]) { return $value.ToUniversalTime() }
    return [datetime]::Parse([string]$value, [Globalization.CultureInfo]::InvariantCulture, [Globalization.DateTimeStyles]::AdjustToUniversal)
}

# Reads id and version from fabric.mod.json inside a jar. Returns $null if it is not a Fabric mod.
function Get-JarInfo([string]$path) {
    $zip = $null
    try {
        $zip = [System.IO.Compression.ZipFile]::OpenRead($path)
        $entry = $zip.GetEntry('fabric.mod.json')
        if ($null -eq $entry) { return $null }
        $reader = New-Object System.IO.StreamReader($entry.Open())
        try { $text = $reader.ReadToEnd() } finally { $reader.Dispose() }
        $json = $text | ConvertFrom-Json
        return New-Object psobject -Property @{ Id = [string]$json.id; Version = [string]$json.version }
    } catch {
        return $null
    } finally {
        if ($null -ne $zip) { $zip.Dispose() }
    }
}

function Update-Installed {
    $list = @()
    if (Test-Path -LiteralPath $script:ModsDir) {
        foreach ($f in Get-ChildItem -LiteralPath $script:ModsDir -Filter '*.jar' -File) {
            $info = Get-JarInfo $f.FullName
            if ($null -ne $info) { $list += $info }
        }
    }
    $script:Installed = $list
}

function Get-InstalledVersion([string]$modId) {
    foreach ($i in $script:Installed) {
        if ($i.Id -eq $modId) { return $i.Version }
    }
    return ''
}

# Delete jars in the mods folder whose fabric.mod.json id equals $modId. Nothing else is touched.
function Remove-ModJars([string]$modId) {
    foreach ($f in Get-ChildItem -LiteralPath $script:ModsDir -Filter '*.jar' -File) {
        $info = Get-JarInfo $f.FullName
        if ($null -ne $info -and $info.Id -eq $modId) {
            Remove-Item -LiteralPath $f.FullName -Force
            Write-Host ('  removed ' + $f.Name)
        }
    }
}

function Test-MinecraftRunning {
    try {
        $procs = Get-CimInstance Win32_Process -Filter "Name='java.exe' OR Name='javaw.exe'"
        foreach ($p in $procs) {
            if ($p.CommandLine -and $p.CommandLine -like '*knot.KnotClient*') { return $true }
        }
    } catch { }
    return $false
}

# ---------------------------------------------------------------- releases

function Get-Releases {
    Write-Host 'Fetching the list of mods from GitHub...'
    try {
        if ($env:MCLAB_RELEASES) {
            $raw = [IO.File]::ReadAllText($env:MCLAB_RELEASES)
            $all = @($raw | ConvertFrom-Json)
        } else {
            $all = @(Invoke-RestMethod -Uri $ApiUrl -Headers @{ 'User-Agent' = 'minecraft-lab-installer'; 'Accept' = 'application/vnd.github+json' })
        }
    } catch {
        Write-Host 'Could not download the release list from GitHub.'
        Write-Host 'Check your internet connection and try again.'
        Write-Host ('(' + $_.Exception.Message + ')')
        exit 1
    }
    $list = @()
    foreach ($r in $all) {
        if ($r.draft) { continue }
        if ($r.tag_name -match '^(.+)-v(\d.*)$') {
            $title = [string]$r.name
            if ($title.EndsWith(' ' + $Matches[2])) { $title = $title.Substring(0, $title.Length - $Matches[2].Length - 1) }
            if ($title -eq '') { $title = $Matches[1] }
            $list += New-Object psobject -Property @{
                Mod       = $Matches[1]
                Version   = $Matches[2]
                Title     = $title
                Pre       = [bool]$r.prerelease
                Published = (ConvertTo-Utc $r.published_at)
                Assets    = @($r.assets)
            }
        }
    }
    $script:Releases = @($list | Sort-Object -Property Published -Descending)
    if ($script:Releases.Count -eq 0) {
        Write-Host "No mod releases were found in $Repo."
        exit 1
    }
}

# ---------------------------------------------------------------- profiles

function Find-Profiles([string]$mc) {
    $found = @()
    $file = Join-Path $McRoot 'launcher_profiles.json'
    if (-not (Test-Path -LiteralPath $file)) { return $found }
    $json = [IO.File]::ReadAllText($file) | ConvertFrom-Json
    if ($null -eq $json.profiles) { return $found }
    foreach ($p in $json.profiles.PSObject.Properties) {
        $v = $p.Value
        if ($null -eq $v.lastVersionId) { continue }
        if ($v.lastVersionId -notlike "fabric-loader-*-$mc") { continue }
        $name = [string]$v.name
        if ($name -eq '') { $name = $p.Name }
        $dir = [string]$v.gameDir
        if ($dir -eq '') { $dir = $McRoot }
        $found += New-Object psobject -Property @{ Name = $name; Dir = $dir }
    }
    return $found
}

# Sets ProfileName / ProfileMc / ModsDir. Returns $false when the user gives up.
function Select-Profile([string]$mc) {
    $profiles = @(Find-Profiles $mc)
    if ($profiles.Count -eq 0) {
        Write-Host ''
        Write-Host "No Minecraft profile with Fabric Loader for Minecraft $mc was found."
        Write-Host "Install Fabric Loader for Minecraft $mc first (https://fabricmc.net/use/installer/,"
        Write-Host "or the Essential installer with Fabric $mc), then run this installer again."
        Write-Host ''
        while ($true) {
            $typed = (Ask 'Or type the full path of your mods folder (empty = exit)').Trim('"')
            if ($typed -eq '') { return $false }
            if (Test-Path -LiteralPath $typed -PathType Container) {
                $script:ProfileName = '(mods folder you typed)'
                $script:ProfileMc = $mc
                $script:ModsDir = $typed
                return $true
            }
            Write-Host 'That folder does not exist. Try again.'
        }
    }
    $index = 0
    if ($profiles.Count -gt 1) {
        Write-Host ''
        Write-Host "Minecraft profiles with Fabric for Minecraft $mc"
        for ($i = 0; $i -lt $profiles.Count; $i++) {
            Write-Host ('  {0}) {1}' -f ($i + 1), $profiles[$i].Name)
        }
        Write-Host '  0) Exit'
        while ($true) {
            $a = Ask 'Choose a profile and press Enter'
            if (Test-Choice $a 0 $profiles.Count) { break }
            Write-Host 'Please type a number from the list.'
        }
        if ([int]$a -eq 0) { return $false }
        $index = [int]$a - 1
    }
    $script:ProfileName = $profiles[$index].Name
    $script:ProfileMc = $mc
    $script:ModsDir = Join-Path $profiles[$index].Dir 'mods'
    if (-not (Test-Path -LiteralPath $script:ModsDir)) {
        New-Item -ItemType Directory -Path $script:ModsDir -Force | Out-Null
    }
    return $true
}

# ---------------------------------------------------------------- install

function Get-AssetUrl($release, [string]$name) {
    foreach ($a in $release.Assets) {
        if ($a.name -eq $name) { return [string]$a.browser_download_url }
    }
    return $null
}

# Returns $true on success. The mods folder is unchanged until the very last steps.
function Install-Release($release) {
    $work = Join-Path ([IO.Path]::GetTempPath()) ('mclab-' + [guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Path $work -Force | Out-Null
    try {
        if (Test-MinecraftRunning) {
            Write-Host ''
            Write-Host 'Minecraft seems to be running. Close it before installing.'
            [void](Ask 'Press Enter when Minecraft is closed (or to continue anyway)')
        }
        Write-Host ''
        Write-Host ('Installing {0} {1}' -f $release.Title, $release.Version)

        Write-Host 'Downloading release.json...'
        $url = Get-AssetUrl $release 'release.json'
        if (-not $url) { Write-Host 'ERROR: this release has no release.json.'; return $false }
        $metaFile = Join-Path $work 'release.json'
        Get-File $url $metaFile
        $meta = [IO.File]::ReadAllText($metaFile) | ConvertFrom-Json
        $jarName = [string]$meta.jar
        $want = ([string]$meta.sha256).ToLower()
        if ($jarName -eq '' -or $want -eq '') { Write-Host 'ERROR: release.json is incomplete.'; return $false }
        if ($jarName.Contains('/') -or $jarName.Contains('\') -or $jarName.Contains('..')) {
            Write-Host 'ERROR: release.json has a bad jar name.'; return $false
        }

        if ($meta.minecraft -and $meta.minecraft -ne $script:ProfileMc) {
            Write-Host ('This version is for Minecraft {0}. Pick the matching profile.' -f $meta.minecraft)
            if (-not (Select-Profile ([string]$meta.minecraft))) { return $false }
            Update-Installed
        }

        Write-Host ('Downloading {0}...' -f $jarName)
        $jarUrl = Get-AssetUrl $release $jarName
        if (-not $jarUrl) { Write-Host ('ERROR: the release has no file {0}.' -f $jarName); return $false }
        $jarFile = Join-Path $work $jarName
        Get-File $jarUrl $jarFile

        Write-Host 'Verifying checksum (sha256)...'
        $got = (Get-FileHash -Algorithm SHA256 -LiteralPath $jarFile).Hash.ToLower()
        if ($got -ne $want) {
            Remove-Item -LiteralPath $jarFile -Force
            Write-Host ('ERROR: sha256 mismatch for {0}' -f $jarName)
            Write-Host ('  expected {0}' -f $want)
            Write-Host ('  got      {0}' -f $got)
            Write-Host 'The download is damaged or was changed. Nothing was installed and your mods folder is untouched.'
            return $false
        }
        Write-Host 'Checksum OK.'

        $fab = [string]$meta.fabric_api
        if ((Get-InstalledVersion 'fabric-api') -eq '' -and $fab -ne '') {
            $fabJar = "fabric-api-$fab.jar"
            $fabUrl = "https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/$fab/$fabJar"
            Write-Host "Fabric API is missing. Downloading $fabJar..."
            $fabFile = Join-Path $work $fabJar
            Get-File $fabUrl $fabFile
            $wantSha1 = (Get-Text "$fabUrl.sha1").Trim().ToLower()
            $gotSha1 = (Get-FileHash -Algorithm SHA1 -LiteralPath $fabFile).Hash.ToLower()
            if ($wantSha1 -eq '' -or $gotSha1 -ne $wantSha1) {
                Write-Host 'ERROR: sha1 mismatch for Fabric API. Nothing was installed.'
                return $false
            }
            Copy-Item -LiteralPath $fabFile -Destination (Join-Path $script:ModsDir $fabJar) -Force
            Write-Host "Installed $fabJar."
        }

        Write-Host ('Removing old versions of {0}...' -f $release.Title)
        Remove-ModJars $release.Mod
        Write-Host ('Copying {0} to the mods folder...' -f $jarName)
        Copy-Item -LiteralPath $jarFile -Destination (Join-Path $script:ModsDir $jarName) -Force
        Write-Host ('Installed {0} {1} into {2}. Start Minecraft with the "{3}" profile.' -f $release.Title, $release.Version, $script:ModsDir, $script:ProfileName)
        return $true
    } catch {
        Write-Host ('ERROR: ' + $_.Exception.Message)
        return $false
    } finally {
        Remove-Item -LiteralPath $work -Recurse -Force -ErrorAction SilentlyContinue
    }
}

# ---------------------------------------------------------------- menus

function Show-VersionsMenu([string]$modId, [string]$title) {
    while ($true) {
        $inst = Get-InstalledVersion $modId
        $versions = @($script:Releases | Where-Object { $_.Mod -eq $modId } | Select-Object -First 6)
        Write-Host ''
        Write-Host $title
        for ($i = 0; $i -lt $versions.Count; $i++) {
            $label = $versions[$i].Version
            if ($versions[$i].Pre) { $label += '  (pre-release)' }
            if ($inst -ne '' -and $inst -eq $versions[$i].Version) { $label += '  (Installed)' }
            Write-Host ('  {0}) {1}' -f ($i + 1), $label)
        }
        Write-Host '  0) Back'
        $a = Ask 'Choose a version and press Enter'
        if (Test-Choice $a 0 $versions.Count) {
            if ([int]$a -eq 0) { return }
            $ok = Install-Release $versions[[int]$a - 1]
            if (-not $ok) { $script:Failed = $true }
            Update-Installed
            return
        }
        Write-Host 'Please type a number from the list.'
    }
}

function Show-ModsMenu {
    while ($true) {
        $mods = @()
        $seen = @{}
        foreach ($r in $script:Releases) {
            if (-not $seen.ContainsKey($r.Mod)) {
                $seen[$r.Mod] = $true
                $mods += New-Object psobject -Property @{ Id = $r.Mod; Title = $r.Title }
            }
        }
        $width = 0
        foreach ($m in $mods) { if ($m.Title.Length -gt $width) { $width = $m.Title.Length } }
        Write-Host ''
        Write-Host ('Minecraft Lab mods  (profile: {0})' -f $script:ProfileName)
        for ($i = 0; $i -lt $mods.Count; $i++) {
            $inst = Get-InstalledVersion $mods[$i].Id
            if ($inst -ne '') {
                Write-Host ('  {0}) {1}   installed {2}' -f ($i + 1), $mods[$i].Title.PadRight($width), $inst)
            } else {
                Write-Host ('  {0}) {1}' -f ($i + 1), $mods[$i].Title)
            }
        }
        Write-Host '  0) Exit'
        $a = Ask 'Choose a mod and press Enter'
        if (Test-Choice $a 0 $mods.Count) {
            if ([int]$a -eq 0) { return }
            Show-VersionsMenu $mods[[int]$a - 1].Id $mods[[int]$a - 1].Title
        } else {
            Write-Host 'Please type a number from the list.'
        }
    }
}

# ---------------------------------------------------------------- main

Write-Host 'Minecraft Lab mod installer'
Get-Releases
if (-not (Select-Profile $DefaultMc)) { exit 1 }
Update-Installed
Show-ModsMenu
if ($script:Failed) { exit 1 }
