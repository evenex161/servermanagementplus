# ServerManagement+ Security & Integrity Architecture

This document provides a standardized technical overview of the security mechanisms implemented within the ServerManagement+ ecosystem. It is intended for platform security moderators, open-source contributors, and system administrators to understand how sensitive configuration data is managed and protected both at rest and during runtime.

## 1. Overview
ServerManagement+ interacts with remote Modrinth and CurseForge APIs to provide server administrators with real-time update notifications and automated over-the-air (OTA) updates.
To securely communicate with these platforms without exposing plaintext developer authentication tokens or API keys within the distributed `.jar` files, the mod employs a robust **Runtime Bytecode Integrity Verification** system combined with **Data-at-Rest Encryption (AES-128)**.

## 2. Cryptographic Implementation Details
The protection of the telemetry API tokens relies on a cryptographic synergy between the mod's compiled bytecode and an embedded data payload (`metrics.dat`).

### 2.1 Build-Time Encryption (Data-at-Rest)
During the Gradle build process, sensitive API keys provided by the developer via local environments are never compiled into the Java classes as plaintext strings. Instead, the build system performs the following sequence:
1. **Target Compilation**: The `CurseForgeUpdateChecker.class` (the designated network handler) is compiled into bytecode.
2. **Cryptographic Hashing**: The build script calculates the **SHA-256** hash of this exact compiled `.class` file.
3. **Key Derivation**: The resulting SHA-256 hash is safely truncated/derived into a symmetric **AES-128** encryption key.
4. **Payload Generation**: The API key is encrypted using the derived AES key, resulting in an unintelligible binary payload. This payload is stored in the mod resources as `assets/servermanagement/metrics.dat`.

### 2.2 Runtime Decryption & Integrity Verification
At runtime, when the server initiates a secure query to the update API, the network handler must decrypt `metrics.dat` dynamically. It achieves this through a strict self-verification process:
1. **Self-Reflection**: The `CurseForgeUpdateChecker` class requests its own raw bytecode from the active JVM classloader.
2. **Runtime Hashing**: The class calculates the **SHA-256** hash of its loaded bytecode.
3. **Symmetric Decryption**: It derives the AES-128 key from this hash and attempts to decrypt the `metrics.dat` payload.
4. **Key Recovery**: If successful, the API key is temporarily held in volatile memory to construct the HTTP Header and is immediately discarded.

## 3. Security Benefits & Fail-Safes
This architecture serves a dual purpose:
- **Token Protection**: Automated scrapers and archive analyzers cannot trivially extract plaintext keys from the `.jar` file, as the key exists solely as an encrypted binary blob.
- **Tamper Evident (Integrity Lock)**: Because the decryption key is mathematically tied to the exact bytecode sequence of the class file, any modification to the compiled `.jar` (e.g., decompilation, byte-code manipulation, or injection of malicious network hooks) will alter the resulting SHA-256 hash. 
- **Graceful Failure**: If the bytecode hash changes, the resulting AES key will be invalid. The decryption will safely fail (`javax.crypto.BadPaddingException`), and the mod will gracefully fall back to a public, unauthenticated query rate-limit or prompt the user to provide their own configuration key.

## 4. Transparency
ServerManagement+ is fully open-source. The build scripts responsible for the AES encryption, as well as the runtime decryption logic, are fully visible within the public repository. This mechanism relies entirely on standard `javax.crypto` and `java.security.MessageDigest` libraries and does not employ any non-standard or proprietary security modules.
