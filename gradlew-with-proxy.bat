@echo off
setlocal enabledelayedexpansion

rem ---------------------------------------------------------------------------
rem gradlew-with-proxy.bat
rem
rem This machine can only reach maven.neoforged.net through the SakuraCat proxy,
rem and Java's own DNS resolves that host to an unreachable address.
rem SakuraCat is toggled on and off, so this script:
rem   1) reads the Windows proxy setting,
rem   2) checks that the port is actually listening (KEEP IT SIMPLE: ASCII only),
rem   3) passes the proxy to Gradle as JVM arguments.
rem
rem Usage is the same as gradlew, e.g.:
rem   gradlew-with-proxy.bat compileJava
rem   gradlew-with-proxy.bat build
rem ---------------------------------------------------------------------------

set "PROXY_HOST="
set "PROXY_PORT="
set "PROXY_RAW="

for /f "usebackq tokens=*" %%P in (`powershell -NoProfile -Command "try { (Get-ItemProperty 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Internet Settings').ProxyServer } catch { '' }"`) do set "PROXY_RAW=%%P"

if defined PROXY_RAW (
  for /f "tokens=1,2 delims=:" %%A in ("!PROXY_RAW!") do (
    set "PROXY_HOST=%%A"
    set "PROXY_PORT=%%B"
  )
)

if defined PROXY_PORT (
  powershell -NoProfile -Command "try { $c=New-Object Net.Sockets.TcpClient; $c.Connect('%PROXY_HOST%', %PROXY_PORT%); $c.Close(); exit 0 } catch { exit 1 }" >nul 2>&1
  if !errorlevel! equ 0 (
    echo [network] using system proxy %PROXY_HOST%:%PROXY_PORT%
    set "GRADLE_OPTS=!GRADLE_OPTS! -Dhttp.proxyHost=%PROXY_HOST% -Dhttp.proxyPort=%PROXY_PORT% -Dhttps.proxyHost=%PROXY_HOST% -Dhttps.proxyPort=%PROXY_PORT%"
  ) else (
    echo [network] proxy %PROXY_HOST%:%PROXY_PORT% is not listening - connecting directly
  )
) else (
  echo [network] no system proxy configured - connecting directly
)

call "%~dp0gradlew.bat" %*
endlocal
