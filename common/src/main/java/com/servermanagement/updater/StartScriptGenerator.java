package com.servermanagement.updater;

import com.servermanagement.Constants;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class StartScriptGenerator {
    
    private static final String DEFAULT_JAVA_CMD = "java -Xmx4G -jar server.jar nogui";

    public static void generateIfNeeded() {
        Path serverRoot = Paths.get("").toAbsolutePath();
        Path batPath = serverRoot.resolve("smart_start.bat");
        Path shPath = serverRoot.resolve("smart_start.sh");

        boolean hasSmartStartIntegrated = false;
        String[] possibleBatScripts = {"run.bat", "start.bat", "launch.bat", "server_start.bat"};
        String[] possibleShScripts = {"run.sh", "start.sh", "launch.sh", "server_start.sh"};

        // Check if any existing script already has the smart start flag
        for (String scriptName : possibleBatScripts) {
            Path script = serverRoot.resolve(scriptName);
            if (Files.exists(script)) {
                try {
                    String content = Files.readString(script);
                    if (content.contains("-Dservermanagement.smartstart=true")) {
                        hasSmartStartIntegrated = true;
                        break;
                    }
                } catch (IOException ignored) {}
            }
        }
        
        if (!hasSmartStartIntegrated) {
            for (String scriptName : possibleShScripts) {
                Path script = serverRoot.resolve(scriptName);
                if (Files.exists(script)) {
                    try {
                        String content = Files.readString(script);
                        if (content.contains("-Dservermanagement.smartstart=true")) {
                            hasSmartStartIntegrated = true;
                            break;
                        }
                    } catch (IOException ignored) {}
                }
            }
        }

        if (hasSmartStartIntegrated) {
            // Clean up standalone smart start scripts if they exist to keep root clean
            try {
                Files.deleteIfExists(batPath);
                Files.deleteIfExists(shPath);
            } catch (IOException ignored) {}
            return;
        }

        if (!Files.exists(batPath)) {
            generateBat(serverRoot, batPath);
        }
        
        if (!Files.exists(shPath)) {
            generateSh(serverRoot, shPath);
        }
    }

    public static void overrideRunScripts(Path serverRoot) {
        String[] possibleBatScripts = {"run.bat", "start.bat", "launch.bat", "server_start.bat"};
        String[] possibleShScripts = {"run.sh", "start.sh", "launch.sh", "server_start.sh"};
        
        for (String batName : possibleBatScripts) {
            Path script = serverRoot.resolve(batName);
            if (Files.exists(script)) {
                try {
                    Files.copy(script, serverRoot.resolve(batName + ".bak"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    Constants.LOG.error("Failed to backup " + batName, e);
                }
                generateBat(serverRoot, script);
                Constants.LOG.info("Overrode " + batName + " with smart start script.");
                break;
            }
        }
        
        for (String shName : possibleShScripts) {
            Path script = serverRoot.resolve(shName);
            if (Files.exists(script)) {
                try {
                    Files.copy(script, serverRoot.resolve(shName + ".bak"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    Constants.LOG.error("Failed to backup " + shName, e);
                }
                generateSh(serverRoot, script);
                Constants.LOG.info("Overrode " + shName + " with smart start script.");
                break;
            }
        }
    }

    private static String injectSmartStartFlag(String javaCmd) {
        if (javaCmd.contains("-Dservermanagement.smartstart=true")) {
            return javaCmd;
        }
        int spaceIdx = javaCmd.indexOf(" ");
        if (javaCmd.startsWith("\"")) {
            spaceIdx = javaCmd.indexOf("\" ") + 1;
            if (spaceIdx == 0) spaceIdx = javaCmd.length();
        }
        
        if (spaceIdx > 0 && spaceIdx < javaCmd.length()) {
            return javaCmd.substring(0, spaceIdx) + " -Dservermanagement.smartstart=true" + javaCmd.substring(spaceIdx);
        } else {
            return javaCmd + " -Dservermanagement.smartstart=true";
        }
    }

    private static String findJavaCommand(Path serverRoot, String[] possibleFiles) {
        for (String fileName : possibleFiles) {
            Path script = serverRoot.resolve(fileName);
            if (Files.exists(script)) {
                try {
                    List<String> lines = Files.readAllLines(script);
                    for (String line : lines) {
                        String trimmed = line.trim();
                        // Ignore comments
                        if (trimmed.startsWith("REM") || trimmed.startsWith("::") || trimmed.startsWith("#")) {
                            continue;
                        }
                        // Find lines starting with java or "path/to/java"
                        if (trimmed.startsWith("java ") || trimmed.contains("bin\\java") || trimmed.contains("bin/java") || trimmed.contains("\"%JAVA_HOME%\\bin\\java\"")) {
                            // If it's not echoing the java string, assume it's the launch command
                            if (!trimmed.toLowerCase().startsWith("echo ")) {
                                return trimmed;
                            }
                        }
                    }
                } catch (IOException e) {
                    Constants.LOG.error("Failed to read script " + fileName + " for parsing", e);
                }
            }
        }
        return DEFAULT_JAVA_CMD;
    }

    private static void generateBat(Path serverRoot, Path outPath) {
        String[] possibleScripts = {"run.bat", "start.bat", "launch.bat", "server_start.bat"};
        String javaCmd = injectSmartStartFlag(findJavaCommand(serverRoot, possibleScripts));
        
        String content = "@echo off\n" +
            "REM ======================================================================\n" +
            "REM  ServerManagement+ Smart Start Script (Windows)\n" +
            "REM  Automatically generated by ServerManagement+\n" +
            "REM ======================================================================\n\n" +
            ":loop\n" +
            "echo [ServerManagement+] Starting Server...\n\n" +
            "REM --- Extracted Server Launch Command ---\n" +
            javaCmd + "\n" +
            "REM ---------------------------------------\n\n" +
            "echo [ServerManagement+] Server stopped. Checking for updates...\n\n" +
            "if exist \"update_in_progress.flag\" (\n" +
            "    echo [ServerManagement+] Update detected! Waiting for updater to finish...\n" +
            "    \n" +
            "    :wait_update\n" +
            "    if exist \"update_finished.flag\" (\n" +
            "        echo [ServerManagement+] Update finished! Restarting the server now...\n" +
            "        del \"update_in_progress.flag\"\n" +
            "        del \"update_finished.flag\"\n" +
            "        goto loop\n" +
            "    )\n" +
            "    \n" +
            "    timeout /t 1 /nobreak >nul\n" +
            "    goto wait_update\n" +
            ")\n\n" +
            "echo [ServerManagement+] Normal shutdown detected. Exiting.\n";

        try {
            Files.writeString(outPath, content);
            Constants.LOG.info("Generated smart_start.bat in server root.");
        } catch (IOException e) {
            Constants.LOG.error("Failed to generate smart_start.bat", e);
        }
    }

    private static void generateSh(Path serverRoot, Path outPath) {
        String[] possibleScripts = {"run.sh", "start.sh", "launch.sh", "server_start.sh"};
        String javaCmd = injectSmartStartFlag(findJavaCommand(serverRoot, possibleScripts));
        
        String content = "#!/bin/bash\n" +
            "# ======================================================================\n" +
            "#  ServerManagement+ Smart Start Script (Linux)\n" +
            "#  Automatically generated by ServerManagement+\n" +
            "# ======================================================================\n\n" +
            "while true; do\n" +
            "    echo \"[ServerManagement+] Starting Server...\"\n" +
            "    \n" +
            "    # --- Extracted Server Launch Command ---\n" +
            "    " + javaCmd + "\n" +
            "    # ---------------------------------------\n" +
            "    \n" +
            "    echo \"[ServerManagement+] Server stopped. Checking for updates...\"\n" +
            "    \n" +
            "    if [ -f \"update_in_progress.flag\" ]; then\n" +
            "        echo \"[ServerManagement+] Update detected! Waiting for updater to finish...\"\n" +
            "        \n" +
            "        while true; do\n" +
            "            if [ -f \"update_finished.flag\" ]; then\n" +
            "                echo \"[ServerManagement+] Update finished! Restarting the server now...\"\n" +
            "                rm \"update_in_progress.flag\"\n" +
            "                rm \"update_finished.flag\"\n" +
            "                break\n" +
            "            fi\n" +
            "            sleep 1\n" +
            "        done\n" +
            "    else\n" +
            "        echo \"[ServerManagement+] Normal shutdown detected. Exiting.\"\n" +
            "        exit 0\n" +
            "    fi\n" +
            "done\n";

        try {
            Files.writeString(outPath, content);
            Constants.LOG.info("Generated smart_start.sh in server root.");
            
            // Try to make it executable
            outPath.toFile().setExecutable(true, false);
        } catch (IOException e) {
            Constants.LOG.error("Failed to generate smart_start.sh", e);
        }
    }
}
