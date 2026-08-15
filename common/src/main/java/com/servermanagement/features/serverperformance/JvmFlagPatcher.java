package com.servermanagement.features.serverperformance;

import com.servermanagement.Constants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Patches server run scripts (run.bat/run.sh) to inject recommended JVM GC flags.
 * Creates .bak backups before modifying any file.
 */
public class JvmFlagPatcher {

    private static final String[] BAT_SCRIPTS = {"run.bat", "start.bat", "launch.bat", "server_start.bat"};
    private static final String[] SH_SCRIPTS = {"run.sh", "start.sh", "launch.sh", "server_start.sh"};

    /**
     * Checks if any detected run script already contains the recommended GC flags.
     */
    public static boolean isAlreadyOptimal(Path serverRoot) {
        String recommended = GCAdvisor.getRecommendedFlags(); // e.g. "-XX:+UseZGC -XX:+ZGenerational"
        String primaryFlag = "-XX:+UseZGC";

        for (String name : BAT_SCRIPTS) {
            Path script = serverRoot.resolve(name);
            if (Files.exists(script)) {
                try {
                    String content = Files.readString(script);
                    if (content.contains(primaryFlag)) return true;
                } catch (IOException ignored) {}
            }
        }
        for (String name : SH_SCRIPTS) {
            Path script = serverRoot.resolve(name);
            if (Files.exists(script)) {
                try {
                    String content = Files.readString(script);
                    if (content.contains(primaryFlag)) return true;
                } catch (IOException ignored) {}
            }
        }
        return false;
    }

    /**
     * Returns the names of detected run scripts that would be patched.
     */
    public static List<String> getDetectedScriptNames(Path serverRoot) {
        List<String> found = new ArrayList<>();
        for (String name : BAT_SCRIPTS) {
            if (Files.exists(serverRoot.resolve(name))) { found.add(name); break; }
        }
        for (String name : SH_SCRIPTS) {
            if (Files.exists(serverRoot.resolve(name))) { found.add(name); break; }
        }
        return found;
    }

    /**
     * Patches all detected run scripts with the recommended GC flags.
     * Creates .bak backups before modifying.
     * @return list of patched script names, empty if none found or on failure
     */
    public static List<String> patchRunScripts(Path serverRoot) {
        List<String> patched = new ArrayList<>();
        String recommendedFlags = GCAdvisor.getRecommendedFlags();
        Set<String> removalFlags = GCAdvisor.getRemovalFlags();

        // Patch first found .bat
        for (String name : BAT_SCRIPTS) {
            Path script = serverRoot.resolve(name);
            if (Files.exists(script)) {
                if (patchScript(script, recommendedFlags, removalFlags)) {
                    patched.add(name);
                }
                break;
            }
        }

        // Patch first found .sh
        for (String name : SH_SCRIPTS) {
            Path script = serverRoot.resolve(name);
            if (Files.exists(script)) {
                if (patchScript(script, recommendedFlags, removalFlags)) {
                    patched.add(name);
                }
                break;
            }
        }

        if (!patched.isEmpty()) {
            GCAdvisor.setScriptPatched(true);
            Constants.LOG.info("JvmFlagPatcher: Successfully patched {} script(s): {}", patched.size(), patched);
        } else {
            Constants.LOG.warn("JvmFlagPatcher: No run scripts found to patch in {}", serverRoot);
        }

        return patched;
    }

    private static boolean patchScript(Path script, String recommendedFlags, Set<String> removalFlags) {
        try {
            // Create .bak backup
            Path backup = script.resolveSibling(script.getFileName().toString() + ".bak");
            Files.copy(script, backup, StandardCopyOption.REPLACE_EXISTING);
            Constants.LOG.info("JvmFlagPatcher: Created backup {}", backup.getFileName());

            // Read and patch lines
            List<String> lines = Files.readAllLines(script);
            List<String> patched = new ArrayList<>();
            boolean modified = false;

            for (String line : lines) {
                String trimmed = line.trim();
                // Skip comments
                if (trimmed.startsWith("REM") || trimmed.startsWith("::") || trimmed.startsWith("#") || trimmed.isEmpty()) {
                    patched.add(line);
                    continue;
                }
                // Detect java launch lines
                if (isJavaLaunchLine(trimmed)) {
                    String patchedLine = injectGCFlags(line, recommendedFlags, removalFlags);
                    patched.add(patchedLine);
                    if (!patchedLine.equals(line)) modified = true;
                } else {
                    patched.add(line);
                }
            }

            if (modified) {
                Files.write(script, patched);
                return true;
            } else {
                // Already has the flags, no modification needed
                Constants.LOG.info("JvmFlagPatcher: {} already contains recommended flags, no changes made", script.getFileName());
                return true;
            }
        } catch (IOException e) {
            Constants.LOG.error("JvmFlagPatcher: Failed to patch {}", script.getFileName(), e);
            return false;
        }
    }

    private static boolean isJavaLaunchLine(String trimmed) {
        String lower = trimmed.toLowerCase();
        if (lower.startsWith("echo ")) return false;
        return lower.startsWith("java ") || lower.contains("bin\\java") ||
               lower.contains("bin/java") || lower.contains("%java_home%");
    }

    /**
     * Injects recommended GC flags into a Java launch command line.
     * Removes any conflicting GC flags first.
     */
    static String injectGCFlags(String line, String recommendedFlags, Set<String> removalFlags) {
        // If already has the recommended flags, skip
        if (line.contains("-XX:+UseZGC")) return line;

        // Remove conflicting GC flags
        String result = line;
        for (String flag : removalFlags) {
            result = result.replace(" " + flag, "");
            result = result.replace(flag + " ", "");
            result = result.replace(flag, "");
        }

        // Find insertion point: after "java" command (possibly quoted path) and before other args
        // Strategy: insert after the first space following "java"
        String trimmed = result.trim();
        String leading = result.substring(0, result.indexOf(trimmed.charAt(0)));
        
        int insertIdx = findInsertionPoint(trimmed);
        if (insertIdx > 0 && insertIdx < trimmed.length()) {
            result = leading + trimmed.substring(0, insertIdx) + " " + recommendedFlags + trimmed.substring(insertIdx);
        } else {
            // Fallback: append before the last token (usually the jar or nogui)
            result = leading + trimmed + " " + recommendedFlags;
        }

        return result;
    }

    private static int findInsertionPoint(String cmd) {
        // Handle quoted java paths like: "C:\Program Files\Java\bin\java.exe" -Xmx4G ...
        if (cmd.startsWith("\"")) {
            int closingQuote = cmd.indexOf('"', 1);
            if (closingQuote > 0) return closingQuote + 1;
        }
        // Handle unquoted: java -Xmx4G ...
        int firstSpace = cmd.indexOf(' ');
        return firstSpace > 0 ? firstSpace : cmd.length();
    }
}
