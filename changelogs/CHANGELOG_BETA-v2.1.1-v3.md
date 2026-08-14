# ServerManagement+ BETA-v2.1.1-v3

## 🐛 Notable Bug Fixes & Polish
- **Network Stability (EncoderException):** Fixed a critical disconnect issue (`String too big`) when sending custom payloads by removing restrictive arbitrary string length bounds across all network packets. Network payload variables now utilize the standard Minecraft length limit, fully accommodating dynamically long string representations (such as 73+ character UUIDs or descriptions) without crashing the server connection.
