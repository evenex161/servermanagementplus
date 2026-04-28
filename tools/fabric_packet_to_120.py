"""
Convert 1.21.1-style fabric packet records (CustomPacketPayload-based)
to 1.20.1 fabric packet records using FabricPacket / ResourceLocation.

For each packet record:
  - Strip `implements net.minecraft.network.protocol.common.custom.CustomPacketPayload`
  - Strip the static TYPE field declaration (3 lines)
  - Strip the static STREAM_CODEC field declaration (2-3 lines)
  - Strip the `type()` override method (3-4 lines)
  - Extract the packet ID string from the original TYPE definition
  - Insert `public static final net.minecraft.resources.ResourceLocation ID = new net.minecraft.resources.ResourceLocation("servermanagement", "<id>");`
"""
import re
import sys
from pathlib import Path

# Match the TYPE field block - pulls out the packet id string
TYPE_RE = re.compile(
    r'    public static final net\.minecraft\.network\.protocol\.common\.custom\.CustomPacketPayload\.Type<\w+> TYPE\s*=\s*\n'
    r'\s*new net\.minecraft\.network\.protocol\.common\.custom\.CustomPacketPayload\.Type<>\(new net\.minecraft\.resources\.ResourceLocation\("servermanagement",\s*"([^"]+)"\)\);\n',
    re.MULTILINE
)

# Match the STREAM_CODEC field block (multi-line). Grab everything up to ';\n' lazily.
STREAM_CODEC_RE = re.compile(
    r'    public static final net\.minecraft\.network\.codec\.StreamCodec<[^,]+,\s*\w+>\s*STREAM_CODEC\s*=\s*\n'
    r'\s*net\.minecraft\.network\.codec\.StreamCodec\.of\(.*?\);\n',
    re.MULTILINE | re.DOTALL
)

# Match the type() override (handles both single and multi-line annotation forms)
TYPE_METHOD_RE = re.compile(
    r'    @Override\n'
    r'    public net\.minecraft\.network\.protocol\.common\.custom\.CustomPacketPayload\.Type<\?\s*extends net\.minecraft\.network\.protocol\.common\.custom\.CustomPacketPayload>\s*type\(\)\s*\{\n'
    r'\s*return TYPE;\n'
    r'\s*\}\n',
    re.MULTILINE
)

# implements clause (with leading space)
IMPLEMENTS_RE = re.compile(
    r'\s+implements\s+net\.minecraft\.network\.protocol\.common\.custom\.CustomPacketPayload'
)

def convert_file(p: Path) -> bool:
    text = p.read_text(encoding='utf-8')
    orig = text

    m = TYPE_RE.search(text)
    if m:
        packet_id = m.group(1)
        text = TYPE_RE.sub('', text)
        # Insert ID constant right after the record header line (only if TYPE was present)
        id_line = (
            f'    public static final net.minecraft.resources.ResourceLocation ID = '
            f'new net.minecraft.resources.ResourceLocation("servermanagement", "{packet_id}");\n\n'
        )
        text = re.sub(
            r'(public record \w+\([^)]*\)\s*\{\n)',
            lambda mm: mm.group(1) + id_line,
            text,
            count=1
        )

    text = STREAM_CODEC_RE.sub('', text)
    text = TYPE_METHOD_RE.sub('', text)
    text = IMPLEMENTS_RE.sub('', text)

    # Collapse 3+ consecutive blank lines into 2
    text = re.sub(r'\n{4,}', '\n\n\n', text)

    if text != orig:
        p.write_text(text, encoding='utf-8')
        return True
    return False


def main():
    base = Path(sys.argv[1]) if len(sys.argv) > 1 else Path('fabric/src/main/java/com/servermanagement/network/packet')
    n = 0
    for f in base.rglob('*.java'):
        if convert_file(f):
            n += 1
            print('converted', f)
    print('files:', n)


if __name__ == '__main__':
    main()
