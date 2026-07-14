#!/bin/bash
# ======================================================================
#  ServerManagement+ Smart Start Script (Linux)
#
#  This script starts your Minecraft server and only restarts it 
#  automatically when an OTA Update is triggered. Normal "/stop" 
#  commands will exit normally.
# ======================================================================

while true; do
    echo "[ServerManagement+] Starting Server..."
    
    # --- REPLACE THIS LINE WITH YOUR ACTUAL SERVER START COMMAND ---
    java -Xmx4G -jar server.jar nogui
    # ---------------------------------------------------------------
    
    echo "[ServerManagement+] Server stopped. Checking for updates..."
    
    # Check if the server dropped an "update_in_progress.flag" before stopping
    if [ -f "update_in_progress.flag" ]; then
        echo "[ServerManagement+] Update detected! Waiting for updater to finish replacing JAR..."
        
        while true; do
            # The updater will drop "update_finished.flag" when it's done copying the file
            if [ -f "update_finished.flag" ]; then
                echo "[ServerManagement+] Update finished! Restarting the server now..."
                rm "update_in_progress.flag"
                rm "update_finished.flag"
                break # Break out of the inner wait loop, going back to the main loop
            fi
            
            # Wait 1 second before checking again
            sleep 1
        done
    else
        echo "[ServerManagement+] Normal shutdown detected. Exiting."
        exit 0
    fi
done
