package com.servermanagement.security;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.servermanagement.ServerManagementMod;
import net.minecraft.world.item.ItemStack;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Optional;

/**
 * Handles secure storage of data with encryption.
 * Automatically migrates unencrypted data to encrypted format.
 * 
 * IMPORTANT: Classes to be saved must:
 * - Mark non-serializable fields as 'transient' (e.g., Random, Thread, etc.)
 * - Re-initialize transient fields after loading
 */
public class SecureDataStorage {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            // Use registerTypeHierarchyAdapter for reliable matching under Forge's classloader
            .registerTypeHierarchyAdapter(ItemStack.class, new ItemStackTypeAdapter())
            // Safety net: Optional cannot be serialized via reflection in Java 17+ (module access)
            .registerTypeAdapter(Optional.class, new TypeAdapter<Optional<?>>() {
                @Override
                public void write(JsonWriter out, Optional<?> value) throws IOException {
                    if (value == null || value.isEmpty()) {
                        out.nullValue();
                    } else {
                        GSON.toJson(value.get(), value.get().getClass(), out);
                    }
                }
                @Override
                public Optional<?> read(JsonReader in) throws IOException {
                    if (in.peek() == JsonToken.NULL) {
                        in.nextNull();
                        return Optional.empty();
                    }
                    // Read as generic object — best effort
                    Object value = GSON.getAdapter(Object.class).read(in);
                    return Optional.ofNullable(value);
                }
            })
            .create();
    private static final String ENCRYPTED_MARKER = "ENCRYPTED_V1:";

    /**
     * Save data with encryption
     */
    public static <T> void save(T data, File file, Class<T> clazz) {
        try {
            file.getParentFile().mkdirs();
            
            // Serialize to JSON
            String json;
            try {
                json = GSON.toJson(data);
            } catch (JsonIOException e) {
                // Improved error message for module access issues
                if (e.getCause() instanceof java.lang.reflect.InaccessibleObjectException) {
                    ServerManagementMod.LOGGER.error(
                        "CRITICAL: Failed to serialize {}. This class contains non-serializable fields " +
                        "(like Random, Thread, etc.) that must be marked as 'transient'. " +
                        "Check the class definition and mark problematic fields with 'transient'.",
                        clazz.getSimpleName()
                    );
                }
                throw e;
            }
            
            // Encrypt
            String encrypted = EncryptionManager.getInstance().encryptString(json);
            
            // Add marker
            String output = ENCRYPTED_MARKER + encrypted;
            
            // Write to file
            try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
                writer.write(output);
            }
            
            ServerManagementMod.LOGGER.debug("Saved encrypted data to: {}", file.getName());
        } catch (Exception e) {
            ServerManagementMod.LOGGER.error("Failed to save encrypted data to: {}", file.getName(), e);
            throw new RuntimeException("Failed to save encrypted data: " + file.getName(), e);
        }
    }

    /**
     * Load data with automatic decryption and migration
     */
    public static <T> T load(File file, Class<T> clazz, T defaultValue) {
        if (!file.exists()) {
            return defaultValue;
        }

        try {
            // Read file content
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            
            if (content.isEmpty()) {
                ServerManagementMod.LOGGER.warn("File is empty: {}", file.getName());
                return defaultValue;
            }

            // Check if encrypted
            if (content.startsWith(ENCRYPTED_MARKER)) {
                // Decrypt
                String encrypted = content.substring(ENCRYPTED_MARKER.length());
                String decrypted = EncryptionManager.getInstance().decryptString(encrypted);
                
                // Deserialize
                T data = GSON.fromJson(decrypted, clazz);
                ServerManagementMod.LOGGER.debug("Loaded encrypted data from: {}", file.getName());
                return data != null ? data : defaultValue;
            } else {
                // Legacy unencrypted data - migrate
                ServerManagementMod.LOGGER.warn("Found unencrypted data in {}, migrating...", file.getName());
                
                // Parse unencrypted data
                T data = GSON.fromJson(content, clazz);
                
                if (data != null) {
                    // Create backup
                    File backup = new File(file.getParent(), file.getName() + ".backup");
                    Files.copy(file.toPath(), backup.toPath(), 
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    ServerManagementMod.LOGGER.info("Created backup: {}", backup.getName());
                    
                    // Save as encrypted
                    save(data, file, clazz);
                    ServerManagementMod.LOGGER.info("Migrated {} to encrypted format", file.getName());
                    
                    return data;
                } else {
                    ServerManagementMod.LOGGER.warn("Failed to parse unencrypted data");
                    return defaultValue;
                }
            }
        } catch (Exception e) {
            ServerManagementMod.LOGGER.error("Failed to load data from {}", file.getName(), e);
            
            // Try to restore from backup
            File backup = new File(file.getParent(), file.getName() + ".backup");
            if (backup.exists()) {
                ServerManagementMod.LOGGER.info("Attempting to restore from backup...");
                try {
                    Files.copy(backup.toPath(), file.toPath(), 
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    return load(file, clazz, defaultValue);
                } catch (Exception ex) {
                    ServerManagementMod.LOGGER.error("Backup restore failed", ex);
                }
            }
            
            return defaultValue;
        }
    }

    /**
     * Check if a file contains encrypted data
     */
    public static boolean isEncrypted(File file) {
        if (!file.exists()) {
            return false;
        }

        try {
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            return content.startsWith(ENCRYPTED_MARKER);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Migrate all data files in a directory to encrypted format
     */
    public static void migrateDirectory(File directory) {
        if (!directory.exists() || !directory.isDirectory()) {
            return;
        }

        File[] files = directory.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) {
            return;
        }

        int migrated = 0;
        for (File file : files) {
            if (!isEncrypted(file)) {
                try {
                    // Read as generic JSON and re-save encrypted
                    String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                    Object data = GSON.fromJson(content, Object.class);
                    
                    if (data != null) {
                        // Create backup
                        File backup = new File(file.getParent(), file.getName() + ".backup");
                        Files.copy(file.toPath(), backup.toPath(), 
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        
                        // Encrypt
                        String json = GSON.toJson(data);
                        String encrypted = EncryptionManager.getInstance().encryptString(json);
                        String output = ENCRYPTED_MARKER + encrypted;
                        
                        // Save
                        Files.writeString(file.toPath(), output, StandardCharsets.UTF_8);
                        
                        migrated++;
                        ServerManagementMod.LOGGER.info("Migrated: {}", file.getName());
                    }
                } catch (Exception e) {
                    ServerManagementMod.LOGGER.error("Failed to migrate {}", file.getName(), e);
                }
            }
        }

        if (migrated > 0) {
            ServerManagementMod.LOGGER.info("Migration complete: {} files encrypted", migrated);
        }
    }
}
