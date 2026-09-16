$ErrorActionPreference = 'Stop'
$root = Split-Path (Split-Path $PSScriptRoot)
$pin = 'e2143d43bcb6762340d8a01f20e7b5fdf104f02f'
function Invoke-Checked([string]$Program, [string[]]$Arguments) {
    & $Program @Arguments
    if ($LASTEXITCODE -ne 0) { throw "$Program failed. See the message above." }
}
try {
    foreach ($tool in @('git', 'dotnet', 'node')) {
        if (!(Get-Command $tool -ErrorAction SilentlyContinue)) {
            throw 'Install Git for Windows, .NET SDK 9, and Node.js 22, then open Build Ryujinx Motion.cmd again. See docs/windows-setup.md.'
        }
    }
    if (!((& dotnet --list-sdks) -match '^9\.')) { throw 'Install .NET SDK 9 from https://dotnet.microsoft.com/download/dotnet/9.0 first.' }
    $build = Join-Path $root ('.local\builds\ryujinx-windows-' + [guid]::NewGuid().ToString('N'))
    $source = Join-Path $build 'source'
    $publish = Join-Path $build 'publish'
    $patch = Join-Path $root 'tools\ryubing-motion.patch'
    New-Item -ItemType Directory -Path $build | Out-Null
    $env:DOTNET_CLI_TELEMETRY_OPTOUT = '1'
    Invoke-Checked git @('clone', '--depth', '1', '--branch', '1.3.3', 'https://git.ryujinx.app/ryubing/ryujinx.git', $source)
    $revision = (& git -C $source rev-parse HEAD).Trim()
    if ($LASTEXITCODE -ne 0 -or $revision -ne $pin) { throw 'Source revision mismatch; no build was selected.' }
    Invoke-Checked git @('-C', $source, 'apply', '--check', $patch)
    Invoke-Checked git @('-C', $source, 'apply', $patch)
    Push-Location $source
    try {
        Invoke-Checked dotnet @('publish', 'src/Ryujinx', '-m:1', '-c', 'Release', '-r', 'win-x64', '--self-contained', 'true', '-p:DebugType=embedded', '-p:Version=1.3.3', '-p:SourceRevisionId=e2143d4-motion-air', '-o', $publish)
    } finally { Pop-Location }
    $env:MOTION_AIR_BUILD = $build
    Invoke-Checked node @((Join-Path $PSScriptRoot 'write-test-fixtures.mjs'))
    Invoke-Checked dotnet @('run', '--project', (Join-Path $PSScriptRoot 'tests\MotionContract.csproj'), "-p:RyujinxSource=$source", '-p:RyujinxRuntime=win-x64', '--', (Join-Path $build 'profile.json'), (Join-Path $build 'motion-0.bin'), (Join-Path $build 'motion-1.bin'))
    $executable = Join-Path $publish 'Ryujinx.exe'
    if (!(Test-Path -LiteralPath $executable)) { throw 'The build did not produce Ryujinx.exe.' }
    $manifest = @{ executable = $executable; maxPlayers = 6; controllerInputVersion = 1; sourceRevision = $pin; patchSHA256 = (Get-FileHash $patch -Algorithm SHA256).Hash; builtAt = [DateTime]::UtcNow.ToString('o') }
    $manifest | ConvertTo-Json | Set-Content -Encoding UTF8 (Join-Path $root '.local\windows-motion-build.json')
    Write-Host "Built: $executable"
    Write-Host 'Open Motion Air.cmd. The new build will be selected, using your existing Ryujinx data.'
} catch { Write-Host $_ -ForegroundColor Red; exit 1 }
