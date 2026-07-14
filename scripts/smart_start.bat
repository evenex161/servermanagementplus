@echo off
REM ======================================================================
REM  ServerManagement+ Smart Start Script (Windows)
REM  
REM  This script starts your Minecraft server and only restarts it 
REM  automatically when an OTA Update is triggered. Normal "/stop" 
REM  commands will exit normally.
REM ======================================================================

:loop
echo [ServerManagement+] Starting Server...

REM --- REPLACE THIS LINE WITH YOUR ACTUAL SERVER START COMMAND ---
java -Xmx4G -jar server.jar nogui
REM ---------------------------------------------------------------

echo [ServerManagement+] Server stopped. Checking for updates...

REM Check if the server dropped an "update_in_progress.flag" before stopping
if exist "update_in_progress.flag" (
    echo [ServerManagement+] Update detected! Waiting for updater to finish replacing JAR...
    
    :wait_update
    REM The updater will drop "update_finished.flag" when it's done copying the file
    if exist "update_finished.flag" (
        echo [ServerManagement+] Update finished! Restarting the server now...
        del "update_in_progress.flag"
        del "update_finished.flag"
        goto loop
    )
    
    REM Wait 1 second before checking again
    timeout /t 1 /nobreak >nul
    goto wait_update
)

echo [ServerManagement+] Normal shutdown detected. Exiting.
