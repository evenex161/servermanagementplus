@echo off
REM ============================================
REM  ServerManagement+ Debug Test Launcher
REM  Multi-version (MC 1.20.1 + 1.21.1) launcher
REM
REM  Usage: test.bat [client|server|both|debug] [forge|neoforge|fabric]
REM
REM  Modes:
REM    client  - Build and launch the client only
REM    server  - Build and launch a dedicated server (nogui)
REM    both    - Build, start server, then start client (connects to localhost)
REM    debug   - Same as both but with 2 clients, debug logging, auto-op
REM
REM  Loader (optional, default: forge):
REM    forge    - Forge loader (always supported)
REM    neoforge - NeoForge loader (only on mc/1.21.1 branch)
REM    fabric   - Fabric loader
REM
REM  The Minecraft version is auto-detected from gradle.properties
REM  (`minecraft_version=`) and the loader list is validated against
  REM  what the current branch actually ships.
REM ============================================

setlocal EnableDelayedExpansion

set MODE=%1
if "%MODE%"=="" set MODE=client

set LOADER=%2
if "%LOADER%"=="" set LOADER=forge

REM --- Auto-detect Minecraft version from gradle.properties ---------------
set MC_VERSION=
for /f "usebackq tokens=1,2 delims==" %%A in ("%~dp0gradle.properties") do (
    if /I "%%A"=="minecraft_version" set MC_VERSION=%%B
)
if "%MC_VERSION%"=="" (
    echo [TEST] Could not detect minecraft_version from gradle.properties.
    exit /b 1
)
echo [TEST] Detected Minecraft version: %MC_VERSION%

REM --- Validate loader against branch capabilities ------------------------
set LOADER_OK=0
if /I "%LOADER%"=="forge"  set LOADER_OK=1
if /I "%LOADER%"=="fabric" set LOADER_OK=1
if /I "%LOADER%"=="neoforge" (
    if "%MC_VERSION%"=="1.21.1" (
        set LOADER_OK=1
    ) else (
        echo [TEST] NeoForge is not available on Minecraft %MC_VERSION%.
        echo [TEST] Use the mc/1.21.1-forge branch for NeoForge support.
        exit /b 1
    )
)
if "%LOADER_OK%"=="0" (
    echo [TEST] Unsupported loader: %LOADER%
    if "%MC_VERSION%"=="1.20.1" (
        echo [TEST] Supported on MC 1.20.1: forge ^| fabric
    ) else (
        echo [TEST] Supported on MC %MC_VERSION%: forge ^| neoforge ^| fabric
    )
    exit /b 1
)

cd /d "%~dp0"

REM Verify the loader subproject actually exists in this checkout
if not exist "%LOADER%\build.gradle" (
    echo [TEST] Loader subproject not found: %LOADER%\build.gradle
    echo [TEST] This branch does not contain the %LOADER% module.
    exit /b 1
)

REM Resolve Gradle task prefix and run directories
set TASK_PREFIX=:%LOADER%
set SERVER_DIR=%LOADER%\runs\server
set CLIENT_DIR=%LOADER%\runs\client

REM ForgeGradle 6 names its run tasks "Client"/"Server" (no "run" prefix);
REM fabric-loom and neoforge moddev use "runClient"/"runServer".
if /I "%LOADER%"=="forge" (
    set CLIENT_TASK=Client
    set SERVER_TASK=Server
) else (
    set CLIENT_TASK=runClient
    set SERVER_TASK=runServer
)

echo [TEST] MC=%MC_VERSION% loader=%LOADER% mode=%MODE%

if "%MODE%"=="server" goto :server
if "%MODE%"=="both" goto :both
if "%MODE%"=="debug" goto :debug
if "%MODE%"=="client" goto :client

echo Unknown mode: %MODE%
echo Usage: test.bat [client^|server^|both^|debug] [forge^|neoforge^|fabric]
exit /b 1

:client
echo [TEST] Building and launching CLIENT (%LOADER%)...
call gradlew.bat %TASK_PREFIX%:%CLIENT_TASK%
goto :eof

:server
echo [TEST] Building and launching SERVER (%LOADER%, nogui)...
call :setup_server
call gradlew.bat %TASK_PREFIX%:%SERVER_TASK%
goto :eof

:both
echo [TEST] Building mod (%LOADER%)...
call gradlew.bat %TASK_PREFIX%:classes
if errorlevel 1 (
    echo [TEST] Build failed!
    exit /b 1
)
call :setup_server
call :create_servers_dat "%CLIENT_DIR%\servers.dat"
echo [TEST] Starting SERVER in background...
REM --console=plain forces Gradle to forward stdin to the server JVM so you can type
REM commands like "op Player1", "stop", etc. directly into the server window.
start "SM+ Test Server" cmd /c "title SM+ Test Server && gradlew.bat %TASK_PREFIX%:%SERVER_TASK% --no-daemon --console=plain 2>&1"
echo [TEST] Waiting 20s for server startup...
timeout /t 20 /nobreak >nul
echo [TEST] Starting CLIENT...
call gradlew.bat %TASK_PREFIX%:%CLIENT_TASK% --no-daemon
echo [TEST] Shutting down server...
call :stop_server
goto :eof

:debug
echo ============================================
echo  ServerManagement+ DEBUG TEST (%LOADER%)
echo  - Local server + 2 clients (multiplayer)
echo  - Debug logging enabled
echo  - Auto-op for Player1 and Player2
echo  - online-mode=false for local testing
echo ============================================
echo.
echo [DEBUG] Compiling mod...
call gradlew.bat %TASK_PREFIX%:classes
if errorlevel 1 (
    echo [DEBUG] Build failed! Fix errors and retry.
    exit /b 1
)
echo [DEBUG] Build OK.
echo.

REM Setup server and client environments
call :setup_server

REM Generate servers.dat for both clients (localhost entry)
call :create_servers_dat "%CLIENT_DIR%\servers.dat"

REM Create separate game dir for client 2
set CLIENT2_DIR=%LOADER%\runs\client2
if not exist "%CLIENT2_DIR%" mkdir "%CLIENT2_DIR%"
if not exist "%CLIENT2_DIR%\config" mkdir "%CLIENT2_DIR%\config"
if exist "%CLIENT_DIR%\config\servermanagement-common.toml" (
    copy /Y "%CLIENT_DIR%\config\servermanagement-common.toml" "%CLIENT2_DIR%\config\servermanagement-common.toml" >nul 2>&1
)
call :create_servers_dat "%CLIENT2_DIR%\servers.dat"

REM Generate ops.json with correct offline-mode UUIDs for both players
powershell -NoProfile -ExecutionPolicy Bypass -File "tools\generate-ops-json.ps1" -OutputFile "%SERVER_DIR%\ops.json" -PlayerNames "Player1,Player2"

REM Enable debug logging for the mod
if not exist "%CLIENT_DIR%\config" mkdir "%CLIENT_DIR%\config"

echo [DEBUG] Starting SERVER in background...
REM --console=plain forces Gradle to forward stdin to the server JVM so you can type
REM commands like "op Player1", "stop", etc. directly into the server window.
start "SM+ Debug Server" cmd /c "title SM+ Debug Server && gradlew.bat %TASK_PREFIX%:%SERVER_TASK% --no-daemon --console=plain -x compileJava -x processResources -x classes 2>&1"

echo [DEBUG] Waiting 25s for server startup...
echo          (Server logs: %SERVER_DIR%\logs\latest.log)
timeout /t 25 /nobreak >nul

REM Verify server is up by checking for the log file update
if exist "%SERVER_DIR%\logs\latest.log" (
    findstr /C:"Done" "%SERVER_DIR%\logs\latest.log" >nul 2>&1
    if errorlevel 1 (
        echo [DEBUG] Server may still be loading, waiting 10 more seconds...
        timeout /t 10 /nobreak >nul
    )
)

echo [DEBUG] ------------------------------------------------
echo [DEBUG]   Player1 = Client 1, Player2 = Client 2
echo [DEBUG]   Press F3+M for GUI debug overlay
echo [DEBUG]   Press F3+L for debug logging
echo [DEBUG]   Use /sm or right-click for mod menu
echo [DEBUG]   Server: localhost in Multiplayer tab
echo [DEBUG] ------------------------------------------------
echo.
echo [DEBUG] Starting CLIENT 1 (Player1) in background...
start "SM+ Client 1" cmd /c "title SM+ Client 1 && gradlew.bat %TASK_PREFIX%:%CLIENT_TASK% --no-daemon -x compileJava -x processResources -x classes -PmcUsername=Player1 2>&1"

echo [DEBUG] Waiting 20s for Client 1 to load...
timeout /t 20 /nobreak >nul

echo [DEBUG] Starting CLIENT 2 (Player2)...
echo [DEBUG]   (Uses separate game dir + Gradle cache + buildSrc dir to avoid lock conflicts)
set BUILDSRC_ALT_BUILD_DIR=build-c2
REM On NeoForge, two parallel runClient invocations stomp on the same artifact-lock
REM file under modlauncher's transformer cache, which produces FileSystemException.
REM Excluding extra prep tasks reduces the contention window.
set CLIENT2_EXTRA_EXCLUDES=
if /I "%LOADER%"=="neoforge" set CLIENT2_EXTRA_EXCLUDES=-x writeMinecraftClasspath -x writeMinecraftClasspathRunClient
call gradlew.bat %TASK_PREFIX%:%CLIENT_TASK% --no-daemon -x compileJava -x processResources -x classes %CLIENT2_EXTRA_EXCLUDES% --project-cache-dir=.gradle-c2 -PmcUsername=Player2 -PmcGameDir=%CLIENT2_DIR%
set BUILDSRC_ALT_BUILD_DIR=
set CLIENT2_EXTRA_EXCLUDES=

echo.
echo [DEBUG] Client 2 closed. Shutting down...
call :stop_clients
call :stop_server
echo [DEBUG] Done.
goto :eof

REM ============================================
REM  Helper subroutines
REM ============================================

:setup_server
REM Accept EULA automatically for dev testing
if not exist "%SERVER_DIR%" mkdir "%SERVER_DIR%"
echo eula=true> "%SERVER_DIR%\eula.txt"

REM Ensure server.properties has local testing defaults
if not exist "%SERVER_DIR%\server.properties" (
    echo #Minecraft server properties> "%SERVER_DIR%\server.properties"
    echo online-mode=false>> "%SERVER_DIR%\server.properties"
    echo server-port=25565>> "%SERVER_DIR%\server.properties"
    echo gamemode=creative>> "%SERVER_DIR%\server.properties"
    echo difficulty=peaceful>> "%SERVER_DIR%\server.properties"
    echo spawn-protection=0>> "%SERVER_DIR%\server.properties"
    echo max-players=4>> "%SERVER_DIR%\server.properties"
    echo level-name=testworld>> "%SERVER_DIR%\server.properties"
    echo motd=ServerManagement Dev Test>> "%SERVER_DIR%\server.properties"
    echo enable-command-block=true>> "%SERVER_DIR%\server.properties"
    echo allow-flight=true>> "%SERVER_DIR%\server.properties"
    echo view-distance=8>> "%SERVER_DIR%\server.properties"
    echo simulation-distance=5>> "%SERVER_DIR%\server.properties"
    echo spawn-npcs=false>> "%SERVER_DIR%\server.properties"
    echo spawn-animals=false>> "%SERVER_DIR%\server.properties"
    echo spawn-monsters=false>> "%SERVER_DIR%\server.properties"
    echo generate-structures=false>> "%SERVER_DIR%\server.properties"
    echo level-type=minecraft\:flat>> "%SERVER_DIR%\server.properties"
)

REM Auto-op is handled by tools\generate-ops-json.ps1 in debug mode
goto :eof

:stop_server
REM Gracefully stop the server by killing the window
taskkill /FI "WINDOWTITLE eq SM+ Debug Server*" >nul 2>&1
taskkill /FI "WINDOWTITLE eq SM+ Test Server*" >nul 2>&1
REM Wait briefly for cleanup
timeout /t 3 /nobreak >nul
goto :eof

:stop_clients
REM Kill any remaining client windows
taskkill /FI "WINDOWTITLE eq SM+ Client 1*" >nul 2>&1
goto :eof

:create_servers_dat
REM Generate a servers.dat (gzipped NBT) with a localhost entry
REM %~1 = output file path
set "_SDAT_DIR=%~dp1"
if not exist "%_SDAT_DIR%" mkdir "%_SDAT_DIR%"
powershell -NoProfile -ExecutionPolicy Bypass -File "tools\create-servers-dat.ps1" "%~1"
if errorlevel 1 (
    echo [WARN] Failed to create %~1 - add localhost server manually in Multiplayer
) else (
    echo [OK] Created %~1 with localhost server entry
)
goto :eof
