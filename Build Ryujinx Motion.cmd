@echo off
chcp 65001 >nul
setlocal DisableDelayedExpansion
title Build Ryujinx Motion
pushd "%~dp0" || exit /b 1
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "tools\ryujinx-build\build-windows.ps1"
if errorlevel 1 (
  pause
  popd
  exit /b 1
)
pause
popd
