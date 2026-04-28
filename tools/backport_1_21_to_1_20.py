#!/usr/bin/env python3
"""
Backport translator: Forge 1.21.1 → Forge 1.20.1 source patterns.

Run on a single .java file in-place:
    python tools/backport_1_21_to_1_20.py <file.java>

Handles common API renames; residual errors must be hand-fixed.
"""
import re
import sys
from pathlib import Path

# (pattern, replacement) — order matters
RULES = [
    # ResourceLocation factory methods → constructors
    (re.compile(r"ResourceLocation\.fromNamespaceAndPath\("), "new ResourceLocation("),
    (re.compile(r"ResourceLocation\.parse\("), "new ResourceLocation("),
    (re.compile(r"ResourceLocation\.withDefaultNamespace\(([^)]+)\)"),
     r'new ResourceLocation("minecraft", \1)'),
    # ItemStack stream codec → buf helpers (handle optional cast on buf arg)
    # encode((Cast) buf, stack)  →  buf.writeItem(stack)
    (re.compile(r"ItemStack\.OPTIONAL_STREAM_CODEC\.encode\(\s*(?:\([^)]*\)\s*)?(\w+)\s*,\s*([^)]+)\)"),
     r"\1.writeItem(\2)"),
    (re.compile(r"ItemStack\.STREAM_CODEC\.encode\(\s*(?:\([^)]*\)\s*)?(\w+)\s*,\s*([^)]+)\)"),
     r"\1.writeItem(\2)"),
    # decode((Cast) buf)  →  buf.readItem()
    (re.compile(r"ItemStack\.OPTIONAL_STREAM_CODEC\.decode\(\s*(?:\([^)]*\)\s*)?(\w+)\s*\)"),
     r"\1.readItem()"),
    (re.compile(r"ItemStack\.STREAM_CODEC\.decode\(\s*(?:\([^)]*\)\s*)?(\w+)\s*\)"),
     r"\1.readItem()"),
    # Repair earlier mistranslations from buggy v1 of these rules:
    #   "(net.minecraft.network.FriendlyByteBuf) buf.writeItem(...)" → "buf.writeItem(...)"
    #   "(net.minecraft.network.FriendlyByteBuf.readItem() buf)" → "buf.readItem()"
    (re.compile(r"\((?:net\.minecraft\.network\.)?(?:Registry)?FriendlyByteBuf\)\s*(\w+)\.writeItem\("),
     r"\1.writeItem("),
    (re.compile(r"\(net\.minecraft\.network\.(?:Registry)?FriendlyByteBuf\.readItem\(\)\s*(\w+)\)"),
     r"\1.readItem()"),
    # RegistryFriendlyByteBuf → FriendlyByteBuf (1.20.1 has no registry-aware variant)
    (re.compile(r"\bRegistryFriendlyByteBuf\b"), "FriendlyByteBuf"),
    (re.compile(r"import net\.minecraft\.network\.RegistryFriendlyByteBuf;\s*\n"), ""),
    # Custom name DataComponents → setHoverName/getHoverName
    (re.compile(r"\.set\(DataComponents\.CUSTOM_NAME,\s*([^)]+)\)"),
     r".setHoverName(\1)"),
    (re.compile(r"\.get\(DataComponents\.CUSTOM_NAME\)"), ".getHoverName()"),
    (re.compile(r"\.has\(DataComponents\.CUSTOM_NAME\)"), ".hasCustomHoverName()"),
    # Networking event types
    (re.compile(r"import net\.minecraftforge\.event\.network\.CustomPayloadEvent;"),
     "import net.minecraftforge.network.NetworkEvent;\nimport java.util.function.Supplier;"),
    (re.compile(r"\bCustomPayloadEvent\.Context\b"), "Supplier<NetworkEvent.Context>"),
    # Inside packet handlers — convert ctx.foo() to ctx.get().foo()
    (re.compile(r"\bctx\.enqueueWork\("), "ctx.get().enqueueWork("),
    (re.compile(r"\bctx\.setPacketHandled\("), "ctx.get().setPacketHandled("),
    (re.compile(r"\bctx\.getSender\(\)"), "ctx.get().getSender()"),
    (re.compile(r"\bctx\.getDirection\(\)"), "ctx.get().getDirection()"),

    # ItemStack equivalence (1.21 distinguishes components, 1.20.1 only tags)
    (re.compile(r"\bItemStack\.isSameItemSameComponents\("), "ItemStack.isSameItemSameTags("),
    (re.compile(r"\.isSameItemSameComponents\("), ".isSameItemSameTags("),

    # AdvancementHolder<X> → Advancement (1.20.1 has no holder wrapper)
    # holder.id() → holder.getId(), holder.value() → holder
    (re.compile(r"\bAdvancementHolder\b"), "Advancement"),
    (re.compile(r"import net\.minecraft\.advancements\.AdvancementHolder;\s*\n"), ""),

    # RecipeHolder<X> → X (strip wrapper); .value() → ; (caller still works)
    (re.compile(r"\bRecipeHolder<([^>]+)>"), r"\1"),
    (re.compile(r"import net\.minecraft\.world\.item\.crafting\.RecipeHolder;\s*\n"), ""),

    # Strip 1.21 component package imports (force fall-through to NBT)
    (re.compile(r"import net\.minecraft\.core\.component\.[^;]+;\s*\n"), ""),
    (re.compile(r"import net\.minecraft\.world\.item\.component\.[^;]+;\s*\n"), ""),

    # ItemStack save/parse with component lookup
    # stack.saveOptional(registries) → stack.save(new CompoundTag())
    (re.compile(r"(\w+)\.saveOptional\(\s*[^)]+\)"),
     r"\1.save(new CompoundTag())"),
    # ItemStack.parseOptional(registries, tag) → ItemStack.of(tag)
    (re.compile(r"ItemStack\.parseOptional\(\s*[^,]+,\s*([^)]+)\)"),
     r"ItemStack.of(\1)"),

    # NbtIo Path overloads → File overloads
    (re.compile(r"NbtIo\.writeCompressed\(\s*([^,]+),\s*([\w.]+)\.toFile\(\)\)"),
     r"NbtIo.writeCompressed(\1, \2.toFile())"),  # idempotent
    (re.compile(r"NbtIo\.writeCompressed\(\s*([^,]+),\s*([\w]+)\)"),
     r"NbtIo.writeCompressed(\1, \2.toFile())"),
    (re.compile(r"NbtIo\.readCompressed\(\s*([\w]+)\.toFile\(\)\)"),
     r"NbtIo.readCompressed(\1.toFile())"),  # idempotent
    (re.compile(r"NbtIo\.readCompressed\(\s*([\w]+)\)(?!\.toFile)"),
     r"NbtIo.readCompressed(\1.toFile())"),
]

def translate(text: str) -> str:
    for pat, repl in RULES:
        text = pat.sub(repl, text)
    return text

def main():
    if len(sys.argv) < 2:
        print(__doc__, file=sys.stderr)
        sys.exit(2)
    paths = []
    for arg in sys.argv[1:]:
        p = Path(arg)
        if p.is_dir():
            paths.extend(p.rglob("*.java"))
        else:
            paths.append(p)
    for p in paths:
        src = p.read_text(encoding="utf-8")
        dst = translate(src)
        if dst != src:
            p.write_text(dst, encoding="utf-8")
            print(f"translated: {p}")
        else:
            print(f"unchanged:  {p}")

if __name__ == "__main__":
    main()
