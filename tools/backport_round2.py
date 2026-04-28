#!/usr/bin/env python3
"""Round 2 fixes — narrow patterns that the main translator missed."""
import re
import sys
from pathlib import Path

RULES = [
    # Packet handlers using `Supplier<NetworkEvent.Context> contextSupplier`
    # with body `Supplier<NetworkEvent.Context> context = contextSupplier;` and
    # then context.enqueueWork(...). Convert to use `ctx`.
    (re.compile(
        r"public void handle\(Supplier<NetworkEvent\.Context>\s+contextSupplier\)\s*\{\s*\n"
        r"\s*Supplier<NetworkEvent\.Context>\s+context\s*=\s*contextSupplier;\s*\n"
    ), "public void handle(Supplier<NetworkEvent.Context> ctx) {\n"),
    # Then convert the remaining context.X calls to ctx.get().X
    (re.compile(r"\bcontext\.enqueueWork\("), "ctx.get().enqueueWork("),
    (re.compile(r"\bcontext\.setPacketHandled\("), "ctx.get().setPacketHandled("),
    (re.compile(r"\bcontext\.getSender\(\)"), "ctx.get().getSender()"),
    (re.compile(r"\bcontext\.getDirection\(\)"), "ctx.get().getDirection()"),

    # Generic advancement.id() → advancement.getId()
    (re.compile(r"(advancement|adv|holder)\.id\(\)"), r"\1.getId()"),
    # AdvancementHolder.value() → just the holder; in 1.20.1 we already typed it as Advancement
    (re.compile(r"\bholder\.value\(\)"), "holder"),

    # FieldFile chunk packets etc. use `(net.minecraft.network.FriendlyByteBuf) buf.X`
    (re.compile(r"\(\s*(?:net\.minecraft\.network\.)?(?:Registry)?FriendlyByteBuf\s*\)\s*(\w+)\.(read|write)"),
     r"\1.\2"),

    # 1.20.1: server.getServerDirectory() returns File, not Path. .resolve(...) doesn't exist.
    # Convert getServerDirectory().resolve("a/b").toFile() → new File(server.getServerDirectory(), "a/b")
    (re.compile(
        r"server\.getServerDirectory\(\)\.resolve\(\s*\"([^\"]+)\"\s*\)\.toFile\(\)"
    ), r'new java.io.File(server.getServerDirectory(), "\1")'),
    # And the bare .resolve("...").toPath() → .toPath().resolve("...")  (then maybe needs more)
    (re.compile(
        r"server\.getServerDirectory\(\)\.resolve\(\s*\"([^\"]+)\"\s*\)"
    ), r'server.getServerDirectory().toPath().resolve("\1")'),

    # NbtIo Path → File overloads (any remaining file.toPath() being passed → just file)
    (re.compile(r"NbtIo\.writeCompressed\(\s*([^,]+),\s*([\w]+)\.toPath\(\)\)"),
     r"NbtIo.writeCompressed(\1, \2)"),
    (re.compile(r"NbtIo\.readCompressed\(\s*([\w]+)\.toPath\(\)\s*,\s*[^)]+\)"),
     r"NbtIo.readCompressed(\1)"),
    (re.compile(r"NbtIo\.readCompressed\(\s*([\w]+)\.toPath\(\)\)"),
     r"NbtIo.readCompressed(\1)"),

    # ItemStack.saveOptional(<long.expression>) → save(new CompoundTag())
    (re.compile(r"\.saveOptional\([^)]*registryAccess\(\)[^)]*\)"),
     ".save(new CompoundTag())"),

    # FrameType enum (1.20.1 has FrameType.TASK/GOAL/CHALLENGE; 1.21 has AdvancementType)
    (re.compile(r"\bAdvancementType\b"), "net.minecraft.advancements.FrameType"),
    # displayInfo.getType() in 1.21 returns AdvancementType; in 1.20.1 it returns FrameType
    # — same call name; nothing needed beyond the type swap above. But callers may have
    # imported AdvancementType — already handled.

    # RecipeInput is a 1.21+ marker interface. 1.20.1 uses Container.
    (re.compile(r"\bnet\.minecraft\.world\.item\.crafting\.RecipeInput\b"),
     "net.minecraft.world.Container"),
    (re.compile(r"\bRecipeInput\b"), "net.minecraft.world.Container"),

    # NbtAccounter.create(N) → new NbtAccounter(N) (1.20.1)
    (re.compile(r"\bNbtAccounter\.create\(\s*([^)]+)\s*\)"),
     r"new net.minecraft.nbt.NbtAccounter(\1)"),
    # NbtAccounter.unlimitedHeap() → NbtAccounter.UNLIMITED
    (re.compile(r"\bNbtAccounter\.unlimitedHeap\(\)"), "net.minecraft.nbt.NbtAccounter.UNLIMITED"),

    # ItemEnchantments (1.21 component) → EnchantmentHelper-based 1.20.1
    # Inline component lookups in market pricing — replace with empty map fallback to keep semantic
    (re.compile(
        r"\w+\.getOrDefault\(\s*\n?\s*net\.minecraft\.core\.component\.DataComponents\.ENCHANTMENTS\s*,\s*\n?\s*net\.minecraft\.world\.item\.enchantment\.ItemEnchantments\.EMPTY\s*\)"
    ), "net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantments(stack)"),
]

def main():
    files = []
    for arg in sys.argv[1:]:
        p = Path(arg)
        if p.is_dir():
            files.extend(p.rglob("*.java"))
        else:
            files.append(p)
    for p in files:
        src = p.read_text(encoding="utf-8")
        dst = src
        for pat, repl in RULES:
            dst = pat.sub(repl, dst)
        if dst != src:
            p.write_text(dst, encoding="utf-8")
            print(f"translated: {p}")

if __name__ == "__main__":
    main()
