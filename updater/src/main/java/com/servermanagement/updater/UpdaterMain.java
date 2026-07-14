package com.servermanagement.updater;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Optional;

public class UpdaterMain {
    public static void main(String[] args) {
        if (args.length < 4) {
            System.err.println("Usage: java -jar updater.jar <PID> <OLD_JAR> <NEW_JAR> <IS_CLIENT> [LAUNCH_ARGS...]");
            System.exit(1);
        }

        long pid = Long.parseLong(args[0]);
        Path oldJar = Paths.get(args[1]);
        Path newJar = Paths.get(args[2]);
        boolean isClient = Boolean.parseBoolean(args[3]);

        Path logFile = oldJar.getParent().resolve("updater_log.txt");
        try (java.io.PrintWriter log = new java.io.PrintWriter(new java.io.FileWriter(logFile.toFile(), true))) {
            log.println("--- Updater Started ---");
            log.println("PID: " + pid);
            log.println("Old Jar: " + oldJar);
            log.println("New Jar: " + newJar);

            Optional<ProcessHandle> handle = ProcessHandle.of(pid);
            if (handle.isPresent()) {
                log.println("Waiting for PID " + pid + " to terminate...");
                handle.get().onExit().join();
                log.println("Process terminated.");
            } else {
                log.println("Process not found, waiting 1s anyway...");
                Thread.sleep(1000); 
            }

            boolean success = false;
            for (int i = 0; i < 20; i++) {
                try {
                    Files.move(newJar, oldJar, StandardCopyOption.REPLACE_EXISTING);
                    success = true;
                    log.println("Successfully replaced jar.");
                    
                    if (!isClient) {
                        Path serverRoot = oldJar.getParent().getParent();
                        if (serverRoot != null) {
                            try {
                                Files.writeString(serverRoot.resolve("update_finished.flag"), "update finished");
                            } catch (Exception e2) {
                                log.println("Failed to write update_finished.flag: " + e2.getMessage());
                            }
                        }
                    }

                    break;
                } catch (Exception e) {
                    log.println("Failed to move, attempt " + (i+1) + ": " + e.getMessage());
                    Thread.sleep(500);
                }
            }

            if (!success) {
                log.println("Failed to replace jar after 20 attempts.");
            }

            if (isClient && !java.awt.GraphicsEnvironment.isHeadless()) {
                if (success) {
                    javax.swing.JOptionPane.showMessageDialog(null, 
                        "The update was successfully installed.\nYou can now restart Minecraft!", 
                        "ServerManagement+ Update", 
                        javax.swing.JOptionPane.INFORMATION_MESSAGE);
                } else {
                    javax.swing.JOptionPane.showMessageDialog(null, 
                        "Failed to replace the mod JAR.\nPlease check the updater_log.txt in your mods folder.", 
                        "ServerManagement+ Update Error", 
                        javax.swing.JOptionPane.ERROR_MESSAGE);
                }
            }

            log.println("Updater finished.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
