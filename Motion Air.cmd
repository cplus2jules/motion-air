@echo off
chcp 65001 >nul
setlocal DisableDelayedExpansion
title Motion Air
pushd "%~dp0" || exit /b 1
where node >nul 2>&1
if errorlevel 1 goto missing_node
node -e "process.exit(Number(process.versions.node.split('.')[0]) >= 20 ? 0 : 1)"
if errorlevel 1 goto missing_node
node -e "Promise.all(['express','ws','qrcode','selfsigned','@nut-tree-fork/nut-js'].map(m=>import(m))).catch(()=>process.exit(1))" >nul 2>&1
if errorlevel 1 (
  echo Installing Motion Air. This only happens on first use or after an update.
  call npm.cmd ci --no-audit --no-fund
  if errorlevel 1 goto failed
)
node tools/start-pairing.mjs --launch %*
if errorlevel 1 goto failed
popd
exit /b 0
:missing_node
echo Install Node.js 22 or newer from https://nodejs.org, then open Motion Air again.
goto failed
:failed
echo.
echo Motion Air could not start. Read the message above, then try again.
pause
popd
exit /b 1
