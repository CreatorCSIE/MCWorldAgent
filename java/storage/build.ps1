# MCWorldAgent java storage build — javac(JDK8) + jar, no deps (chunk-export batch).
# Usage: powershell -File java/storage/build.ps1
$ErrorActionPreference = 'Stop'

$root  = Split-Path -Parent $MyInvocation.MyCommand.Path
$src   = Join-Path $root 'src'
$out   = Join-Path $root 'out'
$classes = Join-Path $out 'classes'

$JDK8 = 'C:\Program Files\Java\jdk1.8.0_202\bin'
if (-not (Test-Path (Join-Path $JDK8 'javac.exe'))) { throw "JDK8 not found at $JDK8" }

Remove-Item $out -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force $classes | Out-Null
$files = Get-ChildItem -Recurse -Filter *.java $src | ForEach-Object { $_.FullName }
& (Join-Path $JDK8 'javac.exe') -encoding UTF-8 -d $classes $files
if ($LASTEXITCODE -ne 0) { throw 'javac failed' }

$jar = Join-Path $out 'mcworldagent-storage.jar'
& (Join-Path $JDK8 'jar.exe') cf $jar -C $classes .
if ($LASTEXITCODE -ne 0) { throw 'jar failed' }

Write-Host "built: $jar"
