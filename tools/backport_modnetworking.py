#!/usr/bin/env python3
"""
Backport ModNetworking.java from 1.21.1 (ChannelBuilder/messageBuilder API) to
1.20.1 (NetworkRegistry.newSimpleChannel + registerMessage).

Idempotent for already-translated input (no-op).
"""
import re
import sys
from pathlib import Path

p = Path(sys.argv[1])
# Read as bytes and decode so we keep line endings exactly. Then normalize for matching.
raw = p.read_bytes().decode("utf-8")
t = raw.replace("\r\n", "\n")

# 1. Imports
t = re.sub(r"import net\.minecraftforge\.network\.ChannelBuilder;\s*\n", "", t)
t = t.replace(
    "import net.minecraftforge.network.SimpleChannel;",
    "import net.minecraftforge.network.NetworkRegistry;\n"
    "import net.minecraftforge.network.simple.SimpleChannel;",
)

# 2. Channel registration block
old_block = """    public static void register() {
        INSTANCE = ChannelBuilder.named(new ResourceLocation(ServerManagementMod.MOD_ID, "main"))
            .networkProtocolVersion(1)
            .clientAcceptedVersions((status, version) -> true)
            .serverAcceptedVersions((status, version) -> true)
            .simpleChannel();
"""
new_block = """    public static void register() {
        INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ServerManagementMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            s -> true,
            s -> true
        );

        ServerManagementMod.LOGGER.info("Registering network packets");
"""
t = t.replace(old_block, new_block)

# 3. Convert messageBuilder/encoder/decoder/consumer/add chains → registerMessage
chain_re = re.compile(
    r"INSTANCE\.messageBuilder\(([\w\.]+)\.class,\s*id\(\)\)\s*\n"
    r"\s*\.encoder\([^\n]+\)\s*\n"
    r"\s*\.decoder\([^\n]+\)\s*\n"
    r"\s*\.consumer\([^\n]+\)\s*\n"
    r"\s*\.add\(\);",
    re.MULTILINE,
)

def repl(m):
    cls = m.group(1)
    return f"INSTANCE.registerMessage(id(), {cls}.class, {cls}::encode, {cls}::new, {cls}::handle);"

t, n = chain_re.subn(repl, t)
print(f"converted {n} packet registrations")

# 4. Drop INSTANCE.build();
t = re.sub(r"\s*INSTANCE\.build\(\);\s*\n", "\n", t)

# 5. send() helpers
t = re.sub(
    r"INSTANCE\.send\(packet,\s*PacketDistributor\.SERVER\.noArg\(\)\)",
    "INSTANCE.sendToServer(packet)",
    t,
)
t = re.sub(
    r"INSTANCE\.send\(packet,\s*PacketDistributor\.PLAYER\.with\(player\)\)",
    "INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet)",
    t,
)
t = re.sub(
    r"INSTANCE\.send\(packet,\s*PacketDistributor\.ALL\.noArg\(\)\)",
    "INSTANCE.send(PacketDistributor.ALL.noArg(), packet)",
    t,
)

p.write_bytes(t.replace("\n", "\r\n").encode("utf-8"))
print(f"wrote {p}")
