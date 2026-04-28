"""Add `implements com.servermanagement.network.IPacket` and an `id()` method
to every packet record in fabric/src/main/java/com/servermanagement/network/packet."""
import re
from pathlib import Path

ID_METHOD = (
    '\n    @Override\n'
    '    public net.minecraft.resources.ResourceLocation id() { return ID; }\n'
)

# Match record header. There may already be `implements` from prior runs; if so skip.
HEADER_RE = re.compile(
    r'^public record (\w+)\(([^)]*)\)\s*(implements [^{]+)?\{',
    re.MULTILINE
)

n = 0
for p in Path('fabric/src/main/java/com/servermanagement/network/packet').rglob('*.java'):
    text = p.read_text(encoding='utf-8')
    orig = text

    # Skip if already has IPacket
    if 'com.servermanagement.network.IPacket' in text or 'implements IPacket' in text:
        continue

    m = HEADER_RE.search(text)
    if not m:
        print('skip (no record header):', p)
        continue

    name, params, impls = m.group(1), m.group(2), m.group(3)
    new_impls = 'implements com.servermanagement.network.IPacket '
    if impls:
        # already has another implements - merge (rare)
        existing = impls.replace('implements ', '').strip().rstrip()
        new_impls = f'implements com.servermanagement.network.IPacket, {existing} '
    new_header = f'public record {name}({params}) {new_impls}{{'

    text = text[:m.start()] + new_header + text[m.end():]

    # Insert id() method right after the ID constant declaration line.
    id_line_re = re.compile(
        r'(    public static final net\.minecraft\.resources\.ResourceLocation ID = new net\.minecraft\.resources\.ResourceLocation\("servermanagement", "[^"]+"\);\n)'
    )
    text, count = id_line_re.subn(lambda mm: mm.group(1) + ID_METHOD, text, count=1)
    if count == 0:
        print('warn (no ID line):', p)

    if text != orig:
        p.write_text(text, encoding='utf-8')
        n += 1

print('files:', n)
