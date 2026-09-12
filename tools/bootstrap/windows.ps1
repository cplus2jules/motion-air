$ErrorActionPreference = 'Stop'
$root = Split-Path (Split-Path $PSScriptRoot)
Set-Location -LiteralPath $root
$stage = $null
$runtimeLock = $null
try {
    if ([Environment]::OSVersion.Version.Major -lt 10) { throw 'Motion Air needs 64-bit Windows 10 or later.' }
    $processor = if ($env:PROCESSOR_ARCHITEW6432) { $env:PROCESSOR_ARCHITEW6432 } else { $env:PROCESSOR_ARCHITECTURE }
    # libnut-win32 ships an x64 native DLL. Windows 11 on ARM can run it with
    # x64 Node through emulation; native ARM Node cannot load this DLL.
    $arch = switch ($processor.ToUpperInvariant()) {
        'ARM64' {
            if ([Environment]::OSVersion.Version.Build -lt 22000) { throw 'ARM PCs need Windows 11 with x64 app emulation.' }
            Write-Host 'Using the x64 runtime for Windows app compatibility on this ARM PC.'
            'x64'
        }
        'AMD64' { 'x64' }
        default { throw 'Motion Air needs 64-bit Windows on an Intel, AMD or ARM processor.' }
    }
    $release = @(Import-Csv -LiteralPath (Join-Path $PSScriptRoot 'node-runtimes.csv') | Where-Object { $_.platform -eq 'win32' -and $_.arch -eq $arch })
    if ($release.Count -ne 1) { throw "No Node.js runtime is configured for Windows $arch." }
    $release = $release[0]
    $runtimes = Join-Path $root '.local\runtime'
    New-Item -ItemType Directory -Force -Path $runtimes | Out-Null
    $runtime = Join-Path $runtimes ($release.filename -replace '\.zip$', '')
    $node = Join-Path $runtime 'node.exe'
    $npm = Join-Path $runtime 'node_modules\npm\bin\npm-cli.js'
    $verified = Join-Path $runtime '.verified-sha256'
    $lockPath = Join-Path $runtimes 'setup.lock'
    Write-Host 'Checking Motion Air setup. If another window is installing, this window will wait.'
    $deadline = [DateTime]::UtcNow.AddMinutes(12)
    while ($null -eq $runtimeLock) {
        try { $runtimeLock = [IO.File]::Open($lockPath, [IO.FileMode]::OpenOrCreate, [IO.FileAccess]::ReadWrite, [IO.FileShare]::None) }
        catch [IO.IOException] {
            if ([DateTime]::UtcNow -gt $deadline) { throw 'Another Motion Air window is still setting up. Wait for it to finish and try again.' }
            Start-Sleep -Seconds 1
        }
    }
    $runtimeWorks = $false
    if (Test-Path -LiteralPath $node) {
        try { & $node --version *> $null; $runtimeWorks = $LASTEXITCODE -eq 0 } catch { $runtimeWorks = $false }
    }
    if (!$runtimeWorks -or !(Test-Path -LiteralPath $npm) -or !(Test-Path -LiteralPath $verified) -or (Get-Content -LiteralPath $verified -Raw).Trim() -ne $release.sha256) {
        Write-Host "First-time setup: downloading Node.js for your Windows PC ($arch)."
        Write-Host 'It stays inside the Motion Air folder. No separate Node.js installation is needed.'
        $stage = Join-Path $runtimes ('.node-download-' + [guid]::NewGuid().ToString('N'))
        New-Item -ItemType Directory -Path $stage | Out-Null
        $archive = Join-Path $stage $release.filename
        [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
        # Windows PowerShell's web/archive cmdlets can interpret brackets in an
        # output path as wildcards. .NET file APIs always treat these as literal.
        Add-Type -AssemblyName System.Net.Http
        $client = New-Object System.Net.Http.HttpClient
        $client.Timeout = [TimeSpan]::FromMinutes(10)
        try {
            $bytes = $client.GetByteArrayAsync(("https://nodejs.org/dist/v{0}/{1}" -f $release.version, $release.filename)).GetAwaiter().GetResult()
            [IO.File]::WriteAllBytes($archive, $bytes)
        } finally { $client.Dispose() }
        if ((Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash.ToLowerInvariant() -ne $release.sha256) {
            throw 'Node.js download verification failed. Open Motion Air again to retry.'
        }
        Add-Type -AssemblyName System.IO.Compression.FileSystem
        [IO.Compression.ZipFile]::ExtractToDirectory($archive, $stage)
        $extracted = Join-Path $stage ($release.filename -replace '\.zip$', '')
        if (!(Test-Path -LiteralPath (Join-Path $extracted 'node.exe')) -or !(Test-Path -LiteralPath (Join-Path $extracted 'node_modules\npm\bin\npm-cli.js'))) {
            throw 'The Node.js download is incomplete. Open Motion Air again to retry.'
        }
        Set-Content -LiteralPath (Join-Path $extracted '.verified-sha256') -Value $release.sha256 -Encoding ASCII
        if (Test-Path -LiteralPath $runtime) { Remove-Item -LiteralPath $runtime -Recurse -Force }
        Move-Item -LiteralPath $extracted -Destination $runtime
    }
    $env:PATH = $runtime + ';' + $env:PATH
    $env:MOTION_AIR_NPM_CLI = $npm
    & $node (Join-Path $PSScriptRoot 'install.mjs')
    if ($LASTEXITCODE -ne 0) { throw 'Setup failed. Check your internet connection and open Motion Air again.' }
    $runtimeLock.Dispose()
    $runtimeLock = $null
    if ($null -ne $stage) { Remove-Item -LiteralPath $stage -Recurse -Force; $stage = $null }
    if ($args -contains '--install-only') { exit 0 }
    & $node (Join-Path $root 'tools\start-pairing.mjs') --launch @args
    $result = $LASTEXITCODE
    if ($result -ne 0) { exit $result }
} catch {
    Write-Host $_ -ForegroundColor Red
    exit 1
} finally {
    if ($null -ne $runtimeLock) { $runtimeLock.Dispose() }
    if ($null -ne $stage -and (Test-Path -LiteralPath $stage)) { Remove-Item -LiteralPath $stage -Recurse -Force }
}
