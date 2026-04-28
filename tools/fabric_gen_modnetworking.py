"""Generate the 1.20.1-compatible fabric ModNetworking.java from packet-list.txt."""
from pathlib import Path

lines = Path('tools/packet-list.txt').read_text(encoding='utf-8').splitlines()
c2s, s2c = [], []
target = c2s
for line in lines:
    line = line.strip()
    if not line:
        continue
    if line.startswith('#'):
        target = s2c if 'S2C' in line else c2s
        continue
    target.append(line)

print(f'C2S: {len(c2s)} | S2C: {len(s2c)}')

def short(fq: str) -> str:
    return fq.rsplit('.', 1)[1]

# Build sections
imports = ['import ' + fq + ';' for fq in c2s + s2c]
imports_block = '\n'.join(sorted(set(imports)))

c2s_register_lines = []
for fq in c2s:
    n = short(fq)
    c2s_register_lines.append(
        f'        ServerPlayNetworking.registerGlobalReceiver({n}.ID, '
        f'(server, player, handler, buf, sender) -> '
        f'{{ {n} pkt = new {n}(buf); server.execute(() -> pkt.handle(player)); }});'
    )

s2c_register_lines = []
for fq in s2c:
    n = short(fq)
    s2c_register_lines.append(
        f'        ClientPlayNetworking.registerGlobalReceiver({n}.ID, '
        f'(client, handler, buf, sender) -> '
        f'{{ {n} pkt = new {n}(buf); client.execute(() -> pkt.handle(null)); }});'
    )

content = f'''package com.servermanagement.network;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import com.servermanagement.ServerManagementModFabric;

{imports_block}

/**
 * 1.20.1 fabric networking facade.
 *
 * <p>Each {{@link IPacket}} is registered under a {{@link net.minecraft.resources.ResourceLocation}}
 * channel using the legacy {{@code ResourceLocation + FriendlyByteBuf}} fabric API
 * (1.21+ {{@code CustomPacketPayload}} / {{@code PayloadTypeRegistry}} are not available
 * on Fabric API 0.92.x for Minecraft 1.20.1).</p>
 */
public final class ModNetworking {{

    private ModNetworking() {{ }}

    public static void registerServerPackets() {{
{chr(10).join(c2s_register_lines)}
    }}

    public static void registerClientPackets() {{
{chr(10).join(s2c_register_lines)}
    }}

    // ---------------- Send helpers -----------------

    public static void sendToServer(IPacket packet) {{
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        packet.encode(buf);
        ClientPlayNetworking.send(packet.id(), buf);
    }}

    public static void sendToPlayer(ServerPlayer player, IPacket packet) {{
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        packet.encode(buf);
        ServerPlayNetworking.send(player, packet.id(), buf);
    }}

    /** Forge-style argument order convenience overload. */
    public static void sendToPlayer(IPacket packet, ServerPlayer player) {{
        sendToPlayer(player, packet);
    }}

    public static void sendToAllPlayers(IPacket packet) {{
        MinecraftServer server = ServerManagementModFabric.getServer();
        if (server == null) return;
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        packet.encode(buf);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {{
            ServerPlayNetworking.send(p, packet.id(), buf.copy());
        }}
    }}
}}
'''

Path('fabric/src/main/java/com/servermanagement/network/ModNetworking.java').write_text(content, encoding='utf-8')
print('wrote ModNetworking.java')
