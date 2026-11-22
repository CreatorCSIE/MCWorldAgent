# MCWorldAgent java agent build — javac(JDK8) + jar, no Maven/Gradle.
# Usage: powershell -File java/agent/build.ps1
$ErrorActionPreference = 'Stop'

$root  = Split-Path -Parent $MyInvocation.MyCommand.Path
$src   = Join-Path $root 'src'
$libs  = Join-Path $root 'libs'
$out   = Join-Path $root 'out'
$classes = Join-Path $out 'classes'

$JDK8 = 'C:\Program Files\Java\jdk1.8.0_202\bin'
if (-not (Test-Path (Join-Path $JDK8 'javac.exe'))) { throw "JDK8 not found at $JDK8" }

# --- ASM dependency (fetch if missing) ---
$asm = Join-Path $libs 'asm-9.7.jar'
if (-not (Test-Path $asm)) {
    New-Item -ItemType Directory -Force $libs | Out-Null
    Write-Host 'fetching asm-9.7.jar ...'
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/ow2/asm/asm/9.7/asm-9.7.jar' -OutFile $asm -UseBasicParsing
}
$asmSha = (Get-FileHash $asm -Algorithm SHA1).Hash.ToLower()
if ($asmSha -ne '073d7b3086e14beb604ced229c302feff6449723') { throw "asm-9.7.jar SHA1 mismatch: $asmSha" }

# --- compile ---
Remove-Item $out -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force $classes | Out-Null
$files = Get-ChildItem -Recurse -Filter *.java $src | ForEach-Object { $_.FullName }
& (Join-Path $JDK8 'javac.exe') -encoding UTF-8 -cp $asm -d $classes $files
if ($LASTEXITCODE -ne 0) { throw 'javac failed' }

# --- B5 runtime fingerprint resources (fingerprints/*.json -> jar resources) ---
$fpSrc = Join-Path $root 'registry\fingerprints'
$fpDst = Join-Path $classes 'fingerprints'
New-Item -ItemType Directory -Force $fpDst | Out-Null
Copy-Item (Join-Path $fpSrc '*.json') $fpDst -Force
Write-Host "fingerprint resources: $((Get-ChildItem $fpDst -Filter *.json).Count) json -> jar"

# --- jar with Premain-Class manifest ---
$manifest = Join-Path $out 'MANIFEST.MF'
@(
    'Manifest-Version: 1.0'
    'Premain-Class: com.mcworldagent.agent.MCWorldAgent'
    ''
) | Set-Content -Encoding ASCII $manifest

$jar = Join-Path $out 'mcworldagent-agent.jar'
& (Join-Path $JDK8 'jar.exe') cfm $jar $manifest -C $classes .
if ($LASTEXITCODE -ne 0) { throw 'jar failed' }

Write-Host "built: $jar"
