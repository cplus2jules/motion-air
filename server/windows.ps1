param([Parameter(Mandatory=$true)][string]$Action)
$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
switch ($Action) {
    'SelectExe' {
        Add-Type -AssemblyName System.Windows.Forms
        $dialog = New-Object System.Windows.Forms.OpenFileDialog
        $dialog.Title = 'Choose your Ryujinx.exe'
        $dialog.Filter = 'Ryujinx application (*.exe)|*.exe'
        if ($dialog.ShowDialog() -eq 'OK') { [Console]::Write($dialog.FileName) }
        $dialog.Dispose()
    }
    'SelectConfig' {
        Add-Type -AssemblyName System.Windows.Forms
        $dialog = New-Object System.Windows.Forms.OpenFileDialog
        $dialog.Title = 'Choose Ryujinx Config.json (File > Open Ryujinx Folder)'
        $dialog.Filter = 'Ryujinx settings (Config.json)|Config.json'
        if ($dialog.ShowDialog() -eq 'OK') { [Console]::Write($dialog.FileName) }
        $dialog.Dispose()
    }
    'Running' {
        $running = @(Get-Process | Where-Object { $_.ProcessName -match '^Ryujinx(\.Avalonia)?$' })
        [Console]::Write(($running.Count -gt 0).ToString().ToLowerInvariant())
    }
    'Browser' { Start-Process -FilePath $env:MOTION_AIR_URL }
    'Launch' {
        $existing = @(Get-CimInstance Win32_Process | Where-Object { $_.Name -match '^Ryujinx(\.Avalonia)?\.exe$' })
        if ($existing.Count -gt 0) {
            $expected = '--root-data-dir "' + $env:MOTION_AIR_CONFIG + '"'
            if ($existing.Count -ne 1 -or $existing[0].ExecutablePath -ne $env:MOTION_AIR_EXE -or
                !$existing[0].CommandLine.Contains($expected)) {
                throw 'Quit Ryujinx and open Motion Air again to use the selected controller profile.'
            }
            $shell = New-Object -ComObject WScript.Shell
            $null = $shell.AppActivate([int]$existing[0].ProcessId)
        } else {
            Start-Process -FilePath $env:MOTION_AIR_EXE -WorkingDirectory (Split-Path $env:MOTION_AIR_EXE) -ArgumentList @('--root-data-dir', ('"' + $env:MOTION_AIR_CONFIG + '"'))
        }
    }
    'Focus' {
        Add-Type @'
using System;
using System.Runtime.InteropServices;
public static class MotionAirForeground {
    [DllImport("user32.dll")] public static extern IntPtr GetForegroundWindow();
    [DllImport("user32.dll")] public static extern uint GetWindowThreadProcessId(IntPtr window, out uint process);
}
'@
        [uint32]$foregroundProcess = 0
        $null = [MotionAirForeground]::GetWindowThreadProcessId([MotionAirForeground]::GetForegroundWindow(), [ref]$foregroundProcess)
        if ($foregroundProcess -ne 0) { [Console]::Write((Get-Process -Id $foregroundProcess).ProcessName) }
    }
    default { throw "Unknown desktop action: $Action" }
}
