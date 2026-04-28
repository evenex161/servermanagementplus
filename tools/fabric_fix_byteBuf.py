import pathlib, re, sys
p = pathlib.Path(sys.argv[1])
t = p.read_text(encoding='utf-8')
t = t.replace('import net.minecraft.network.RegistryFriendlyByteBuf;', 'import net.minecraft.network.FriendlyByteBuf;')
t = re.sub(r'RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf\(\s*Unpooled\.buffer\(\),[^;]*\);', 'FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());', t)
t = t.replace('ClientboundPlayerInfoUpdatePacket.STREAM_CODEC.decode(buf)', 'new ClientboundPlayerInfoUpdatePacket(buf)')
p.write_text(t, encoding='utf-8')
print('patched', p)
