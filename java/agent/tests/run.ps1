# run.ps1 - MCWorldAgent headless test master entry (ASCII only, PS5, no &&)
# Archived 2026-11-22 from %TEMP%\mcaut (B5 test-infra archival batch). Suites:
#   old 11 (A2 0618/0629/1121 + F1 RegistryEquals + VersionResolver)
#   a1.2.0 trio, b1.2_02 trio, b1.7.3 trio, b1.8.1 trio
#   B5LoaderTest x2 branches (classes-dir + jar resources), 218 assertions each
#   HookSnapshot byte-compare vs baseline\hook-snapshot.txt (9 hardcoded groups)
#   storage module StorageTest (skipped if java\storage\out\classes absent)
# All compile output goes to %TEMP%\mcaut\tests-out\ (repo stays artifact-free).
# Agent sources are compiled to java\agent\out\classes (gitignored build dir),
# same as build.ps1 / the original per-batch runners.
$ErrorActionPreference = 'Stop'
$J = 'C:\Program Files\Java\jdk1.8.0_202\bin'
$agent = 'E:\4k\github-projects\MCWorldAgent\java\agent'
$T = Join-Path $agent 'tests'
$O = Join-Path $env:TEMP 'mcaut\tests-out'
Set-Location $agent

if (Test-Path $O) { Remove-Item -Recurse -Force $O }
foreach ($d in @('', 'a2out', 'a2out120', 'a2out1202', 'a2out1401', 'a2out1501', 'a2out16', 'a2out173', 'a2out181', 'a2out19pre2', 'b5out', 'inj0618', 'storage_cls')) {
  New-Item -ItemType Directory -Force (Join-Path $O $d) | Out-Null
}

# ---- agent classes + fingerprint resources (dir-branch loader needs them) ----
$srcs = Get-ChildItem -Recurse -Filter *.java src | ForEach-Object { $_.FullName }
& "$J\javac.exe" -encoding UTF-8 -cp libs\asm-9.7.jar -d out\classes $srcs
if ($LASTEXITCODE -ne 0) { throw 'agent javac failed' }
$fpDst = 'out\classes\fingerprints'
New-Item -ItemType Directory -Force $fpDst | Out-Null
Copy-Item 'registry\fingerprints\*.json' $fpDst -Force
Write-Host 'AGENT COMPILE OK'

# ---- fake 0618 World (b(Z)V core, F1 ruling 2026-11-21) for InjectSmoke0618 ----
& "$J\javac.exe" -encoding UTF-8 -d (Join-Path $O 'inj0618') (Join-Path $T 'src\inj0618\g.java')
if ($LASTEXITCODE -ne 0) { throw 'inj0618 fake javac failed' }

# ---- old 11-suite ----
$root0629 = Get-ChildItem "$T\src\root0629" -Recurse -Filter *.java | ForEach-Object { $_.FullName }
$root1121 = Get-ChildItem "$T\src\root1121" -Recurse -Filter *.java | ForEach-Object { $_.FullName }
$old = @(
  "$T\src\com\mcworldagent\agent\A2TableTest.java",
  "$T\src\com\mcworldagent\agent\A2Table0629Test.java",
  "$T\src\com\mcworldagent\agent\A2Table1121Test.java",
  "$T\src\com\mcworldagent\agent\FakeOps0618Test.java",
  "$T\src\com\mcworldagent\agent\FakeOps0629Test.java",
  "$T\src\com\mcworldagent\agent\FakeOps1121Test.java",
  "$T\src\com\mcworldagent\agent\InjectSmoke0618Test.java",
  "$T\src\com\mcworldagent\agent\InjectSmoke0629Test.java",
  "$T\src\com\mcworldagent\agent\InjectSmoke1121Test.java",
  "$T\src\com\mcworldagent\agent\VersionResolverA2Test.java",
  "$T\src\net\minecraft\a\c\e.java",
  "$T\src\net\minecraft\client\Minecraft.java",
  "$T\src\RegistryEqualsTest.java") + $root0629 + $root1121
& "$J\javac.exe" -encoding UTF-8 -cp 'out\classes;libs\asm-9.7.jar' -d "$O\a2out" $old
if ($LASTEXITCODE -ne 0) { throw 'old suite javac failed' }

# ---- per-version trios (separate out dirs: root letter collisions) ----
function Compile-Trio([string]$tag, [string[]]$trio) {
  $root = Get-ChildItem "$T\src\root$tag" -Recurse -Filter *.java | ForEach-Object { $_.FullName }
  $files = @("$T\src\com\mcworldagent\agent\A2TableTest.java") +
    ($trio | ForEach-Object { "$T\src\com\mcworldagent\agent\${_}.java" }) + $root
  & "$J\javac.exe" -encoding UTF-8 -cp 'out\classes;libs\asm-9.7.jar' -d "$O\a2out$tag" $files
  if ($LASTEXITCODE -ne 0) { throw "suite $tag javac failed" }
}
Compile-Trio '120' @('A2Table120Test', 'FakeOps120Test', 'InjectSmoke120Test')
Compile-Trio '1202' @('A2Table1202Test', 'FakeOps1202Test', 'InjectSmoke1202Test')
Compile-Trio '173' @('A2Table173Test', 'FakeOps173Test', 'InjectSmoke173Test')
Compile-Trio '181' @('A2Table181Test', 'FakeOps181Test', 'InjectSmoke181Test')
Compile-Trio '19pre2' @('A2Table19pre2Test', 'FakeOps19pre2Test', 'InjectSmoke19pre2Test')
Compile-Trio '1501' @('A2Table1501Test', 'FakeOps1501Test', 'InjectSmoke1501Test')
Compile-Trio '1401' @('A2Table1401Test', 'FakeOps1401Test', 'InjectSmoke1401Test')
Compile-Trio '16' @('A2Table16Test', 'FakeOps16Test')

# ---- B5 loader + snapshot tools + guard batch ----
& "$J\javac.exe" -encoding UTF-8 -cp 'out\classes;libs\asm-9.7.jar' -d "$O\b5out" `
  "$T\src\com\mcworldagent\agent\B5LoaderTest.java" "$T\src\com\mcworldagent\agent\GuardTest.java" `
  "$T\src\com\mcworldagent\agent\D8GateTest.java" `
  "$T\src\com\mcworldagent\agent\HookSnapshot.java"
if ($LASTEXITCODE -ne 0) { throw 'b5 test javac failed' }
Write-Host 'COMPILE OK'

# ---- agent jar (jar-branch + resource assertions) ----
& powershell -File (Join-Path $agent 'build.ps1')
if ($LASTEXITCODE -ne 0) { throw 'jar build failed' }
$jar = Join-Path $agent 'out\mcworldagent-agent.jar'

$results = @()
function Invoke-Test([string]$name, [string]$cp, [string]$cls, [string[]]$argv) {
  # tests print expected stderr noise (javac notes / stack traces on failure);
  # keep Stop from promoting native stderr to a terminating error
  $eap = $ErrorActionPreference
  $ErrorActionPreference = 'Continue'
  $out = & "$J\java.exe" -cp $cp $cls @argv 2>&1
  $code = $LASTEXITCODE
  $ErrorActionPreference = $eap
  $status = if ($code -eq 0) { 'GREEN' } else { 'RED' }
  $last = (($out | Where-Object { $_ -is [string] } | Select-Object -Last 1))
  if ($null -eq $last) { $last = '' }
  $script:results += [pscustomobject]@{ Name = $name; Status = $status; Summary = $last }
  Write-Host ("{0} {1} :: {2}" -f $status, $name, $last)
  if ($status -ne 'GREEN') { ($out | Select-Object -Last 12) | ForEach-Object { Write-Host "  | $_" } }
}

# old 11
$cpOld = "$O\a2out;out\classes;libs\asm-9.7.jar"
foreach ($cls in @('com.mcworldagent.agent.A2TableTest',
                   'com.mcworldagent.agent.A2Table0629Test',
                   'com.mcworldagent.agent.A2Table1121Test',
                   'com.mcworldagent.agent.FakeOps0618Test',
                   'com.mcworldagent.agent.FakeOps0629Test',
                   'com.mcworldagent.agent.FakeOps1121Test',
                   'com.mcworldagent.agent.InjectSmoke0618Test',
                   'com.mcworldagent.agent.InjectSmoke0629Test',
                   'com.mcworldagent.agent.InjectSmoke1121Test',
                   'com.mcworldagent.agent.VersionResolverA2Test',
                   'RegistryEqualsTest')) {
  Invoke-Test $cls $cpOld $cls @()
}
# trios
foreach ($vtag in @('120', '1202', '173', '181', '19pre2')) {
  $cp = "$O\a2out$vtag;out\classes;libs\asm-9.7.jar"
  foreach ($cls in @("com.mcworldagent.agent.A2Table$($vtag)Test",
                     "com.mcworldagent.agent.FakeOps$($vtag)Test",
                     "com.mcworldagent.agent.InjectSmoke$($vtag)Test")) {
    Invoke-Test $cls $cp $cls @()
  }
}
# b1.5_01 trio (2026-11-28 转正批: builtin build1501()+TARGETS_1501 in, InjectSmoke added;
# A2Table1501Test keeps injectTable as anchor-regression leg after builtin cross-check)
$cp1501 = "$O\a2out1501;out\classes;libs\asm-9.7.jar"
foreach ($cls in @("com.mcworldagent.agent.A2Table1501Test",
                   "com.mcworldagent.agent.FakeOps1501Test",
                   "com.mcworldagent.agent.InjectSmoke1501Test")) {
  Invoke-Test $cls $cp1501 $cls @()
}
# b1.4_01 trio (2026-11-28 转正批: builtin build1401()+TARGETS_1401 in, InjectSmoke added;
# A2Table1401Test keeps injectTable as anchor-regression leg after builtin cross-check)
$cp1401 = "$O\a2out1401;out\classes;libs\asm-9.7.jar"
foreach ($cls in @("com.mcworldagent.agent.A2Table1401Test",
                   "com.mcworldagent.agent.FakeOps1401Test",
                   "com.mcworldagent.agent.InjectSmoke1401Test")) {
  Invoke-Test $cls $cp1401 $cls @()
}
# b1.6 pair (JSON-only draft anchor, V-7 batch: weather/getbiome OK legs + 3 loud)
$cp16 = "$O\a2out16;out\classes;libs\asm-9.7.jar"
foreach ($cls in @("com.mcworldagent.agent.A2Table16Test",
                   "com.mcworldagent.agent.FakeOps16Test")) {
  Invoke-Test $cls $cp16 $cls @()
}
# B5 loader: dir branch + jar branch
Invoke-Test 'B5LoaderTest(dir)' "$O\b5out;out\classes;libs\asm-9.7.jar" 'com.mcworldagent.agent.B5LoaderTest' @($jar)
Invoke-Test 'B5LoaderTest(jar)' "$O\b5out;$jar;libs\asm-9.7.jar" 'com.mcworldagent.agent.B5LoaderTest' @($jar)
# guard batch (strict loader + 13-JSON gate + MiniJson edges + envelope fuzz)
Invoke-Test 'GuardTest' "$O\b5out;out\classes;libs\asm-9.7.jar" 'com.mcworldagent.agent.GuardTest' @("$agent\registry\fingerprints")
# D8 gate: weather must UNSUPPORTED loudly on L0 lines (own JVMs, static T bind)
Invoke-Test 'D8GateTest(0624)' "$O\b5out;out\classes;libs\asm-9.7.jar" 'com.mcworldagent.agent.D8GateTest' @('inf-20100624')
Invoke-Test 'D8GateTest(a1.0.3)' "$O\b5out;out\classes;libs\asm-9.7.jar" 'com.mcworldagent.agent.D8GateTest' @('a1.0.3')

# HookSnapshot regression: byte-compare vs archived baseline
$snap = & "$J\java.exe" -cp "$O\b5out;out\classes;libs\asm-9.7.jar" com.mcworldagent.agent.HookSnapshot 2>&1
$snapFile = Join-Path $O 'hook-snapshot.txt'
[System.IO.File]::WriteAllLines($snapFile, ($snap | ForEach-Object { "$_" }), (New-Object System.Text.UTF8Encoding($false)))
$base = Get-Content (Join-Path $T 'baseline\hook-snapshot.txt')
$cur = Get-Content $snapFile
$diff = @(Compare-Object $base $cur)
if ($diff.Count -eq 0) {
  $results += [pscustomobject]@{ Name = 'HookSnapshot-vs-baseline'; Status = 'GREEN'; Summary = "byte-identical ($($cur.Count) lines)" }
  Write-Host "GREEN HookSnapshot-vs-baseline :: byte-identical ($($cur.Count) lines)"
} else {
  $results += [pscustomobject]@{ Name = 'HookSnapshot-vs-baseline'; Status = 'RED'; Summary = "$($diff.Count) diff lines" }
  Write-Host "RED HookSnapshot-vs-baseline :: $($diff.Count) diff lines"
  $diff | Select-Object -First 10 | ForEach-Object { Write-Host ("  {0} {1}" -f $_.SideIndicator, $_.InputObject) }
}

# storage module regression (conditional)
$stg = 'E:\4k\github-projects\MCWorldAgent\java\storage\out\classes'
if (Test-Path $stg) {
  & "$J\javac.exe" -encoding UTF-8 -cp $stg -d "$O\storage_cls" "$T\src\storage\StorageTest.java"
  if ($LASTEXITCODE -ne 0) { throw 'StorageTest javac failed' }
  Invoke-Test 'storage.StorageTest' "$O\storage_cls;$stg" 'storage.StorageTest' @()
} else {
  $results += [pscustomobject]@{ Name = 'storage.StorageTest'; Status = 'SKIP'; Summary = 'java\storage\out\classes missing' }
  Write-Host 'SKIP storage.StorageTest (storage classes not built)'
}

# ---- final tally ----
Write-Host ''
Write-Host '===== HEADLESS FINAL TALLY ====='
$results | ForEach-Object { Write-Host ("{0,-4} {1,-55} {2}" -f $_.Status, $_.Name, $_.Summary) }
$green = @($results | Where-Object { $_.Status -eq 'GREEN' }).Count
$red = @($results | Where-Object { $_.Status -eq 'RED' }).Count
$skip = @($results | Where-Object { $_.Status -eq 'SKIP' }).Count
Write-Host ("TOTAL items={0} green={1} red={2} skip={3}" -f $results.Count, $green, $red, $skip)
if ($red -gt 0) { exit 1 }
Write-Host 'ALL HEADLESS GREEN (repo-archived suite)'
exit 0
