package com.servermanagement.security;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.util.PerformanceMetrics;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Handles AES-256-GCM encryption/decryption for secure data storage and transmission.
 * Uses authenticated encryption to prevent tampering.
 * Optimized with ThreadLocal cipher pooling to reduce GC pressure.
 */
public class EncryptionManager {
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int KEY_SIZE = 256;
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    
    private static EncryptionManager instance;
    private SecretKey serverKey;
    private final SecureRandom secureRandom;
    
    // ThreadLocal cipher pooling to avoid repeated Cipher.getInstance() calls
    private final ThreadLocal<Cipher> encryptCipher = ThreadLocal.withInitial(() -> {
        try {
            return Cipher.getInstance(ALGORITHM);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize cipher", e);
        }
    });
    
    private final ThreadLocal<Cipher> decryptCipher = ThreadLocal.withInitial(() -> {
        try {
            return Cipher.getInstance(ALGORITHM);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize cipher", e);
        }
    });

    private EncryptionManager() {
        this.secureRandom = new SecureRandom();
    }

    public static EncryptionManager getInstance() {
        if (instance == null) {
            instance = new EncryptionManager();
        }
        return instance;
    }

    /**
     * Initialize or load the server encryption key
     */
    public void initialize(java.io.File serverDir) {
        try {
            java.io.File keyFile = new java.io.File(serverDir, "data/servermanagement/.key");
            
            if (keyFile.exists()) {
                // Load existing key
                byte[] keyBytes = java.nio.file.Files.readAllBytes(keyFile.toPath());
                this.serverKey = new SecretKeySpec(keyBytes, "AES");
                ServerManagementMod.LOGGER.info("Loaded encryption key");
            } else {
                // Generate new key
                KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                keyGen.init(KEY_SIZE, secureRandom);
                this.serverKey = keyGen.generateKey();
                
                // Save key securely
                keyFile.getParentFile().mkdirs();
                java.nio.file.Files.write(keyFile.toPath(), serverKey.getEncoded());
                
                // Set restrictive permissions (owner read/write only)
                try {
                    java.nio.file.Files.setPosixFilePermissions(
                        keyFile.toPath(),
                        java.nio.file.attribute.PosixFilePermissions.fromString("rw-------")
                    );
                } catch (UnsupportedOperationException e) {
                    // Windows doesn't support POSIX permissions
                    keyFile.setReadable(false, false);
                    keyFile.setReadable(true, true);
                    keyFile.setWritable(false, false);
                    keyFile.setWritable(true, true);
                }
                
                ServerManagementMod.LOGGER.info("Generated new encryption key");
            }
        } catch (Exception e) {
            ServerManagementMod.LOGGER.error("Failed to initialize encryption", e);
            throw new RuntimeException("Encryption initialization failed", e);
        }
    }

    /**
     * Encrypt data using AES-256-GCM
     * Uses ThreadLocal cipher pool for performance
     */
    public byte[] encrypt(byte[] data) throws Exception {
        if (serverKey == null) {
            throw new IllegalStateException("Encryption not initialized");
        }

        PerformanceMetrics.getInstance().recordEncrypt();
        
        // Generate random IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        secureRandom.nextBytes(iv);

        // Get pooled cipher and initialize
        Cipher cipher = encryptCipher.get();
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, serverKey, parameterSpec);

        // Encrypt data
        byte[] encryptedData = cipher.doFinal(data);

        // Combine IV and encrypted data
        ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encryptedData.length);
        byteBuffer.put(iv);
        byteBuffer.put(encryptedData);

        return byteBuffer.array();
    }

    /**
     * Decrypt data using AES-256-GCM
     * Uses ThreadLocal cipher pool for performance
     */
    public byte[] decrypt(byte[] encryptedData) throws Exception {
        if (serverKey == null) {
            throw new IllegalStateException("Encryption not initialized");
        }

        PerformanceMetrics.getInstance().recordDecrypt();
        
        // Extract IV and encrypted data
        ByteBuffer byteBuffer = ByteBuffer.wrap(encryptedData);
        byte[] iv = new byte[GCM_IV_LENGTH];
        byteBuffer.get(iv);
        byte[] cipherText = new byte[byteBuffer.remaining()];
        byteBuffer.get(cipherText);

        // Get pooled cipher and initialize
        Cipher cipher = decryptCipher.get();
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, serverKey, parameterSpec);

        // Decrypt data
        return cipher.doFinal(cipherText);
    }

    /**
     * Encrypt string data
     */
    public String encryptString(String data) {
        try {
            byte[] encrypted = encrypt(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            ServerManagementMod.LOGGER.error("Encryption failed", e);
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * Decrypt string data
     */
    public String decryptString(String encryptedData) {
        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedData);
            byte[] decrypted = decrypt(decoded);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            ServerManagementMod.LOGGER.error("Decryption failed", e);
            throw new RuntimeException("Decryption failed", e);
        }
    }

    /**
     * Check if data is encrypted (has valid Base64 + encryption header)
     */
    public boolean isEncrypted(String data) {
        if (data == null || data.isEmpty()) {
            return false;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(data);
            // Encrypted data should be at least IV length + tag length
            return decoded.length >= GCM_IV_LENGTH + (GCM_TAG_LENGTH / 8);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Generate a secure random token for session authentication
     */
    public String generateSecureToken() {
        byte[] token = new byte[32];
        secureRandom.nextBytes(token);
        return Base64.getEncoder().encodeToString(token);
    }

    /**
     * Generate HMAC for packet authentication
     */
    public String generateHMAC(String data, String secret) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hmac);
        } catch (Exception e) {
            ServerManagementMod.LOGGER.error("HMAC generation failed", e);
            throw new RuntimeException("HMAC generation failed", e);
        }
    }

    /**
     * Verify HMAC for packet authentication
     */
    public boolean verifyHMAC(String data, String hmac, String secret) {
        String calculatedHMAC = generateHMAC(data, secret);
        return calculatedHMAC.equals(hmac);
    }
}
