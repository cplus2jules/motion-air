@echo off
chcp 65001 >nul
setlocal DisableDelayedExpansion
title Motion Air
pushd "%~dp0" || exit /b 1
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "tools\bootstrap\windows.ps1" %*
if errorlevel 1 goto failed
popd
exit /b 0
:failed
echo.
echo Motion Air could not start. Read the message above, then try again.
pause
popd
exit /b 1
