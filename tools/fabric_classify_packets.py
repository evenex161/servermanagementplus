import re
from pathlib import Path

t = Path('fabric/src/main/java/com/servermanagement/network/ModNetworking.java').read_text(encoding='utf-8')
c2s = re.findall(r'PayloadTypeRegistry\.playC2S\(\)\.register\((\w+)\.TYPE', t)
s2c = re.findall(r'PayloadTypeRegistry\.playS2C\(\)\.register\((\w+)\.TYPE', t)

# Look up the packet's import path (sub-package) to know whether it's `packet` or `packet.minebay`
imports = {}
for line in t.splitlines():
    m = re.match(r'import com\.servermanagement\.network\.(packet[\w.]*)\.\*;', line)
    # We use star imports — keep all packet sub-packages
all_pkgs = set(['packet', 'packet.minebay'])

def find_pkg(name: str) -> str:
    for pkg in ['packet', 'packet.minebay']:
        p = Path(f'fabric/src/main/java/com/servermanagement/network/{pkg.replace(".", "/")}/{name}.java')
        if p.exists():
            return f'com.servermanagement.network.{pkg}.{name}'
    return f'com.servermanagement.network.packet.{name}'

with open('tools/packet-list.txt', 'w', encoding='utf-8') as f:
    f.write('# C2S\n')
    for n in c2s:
        f.write(find_pkg(n) + '\n')
    f.write('# S2C\n')
    for n in s2c:
        f.write(find_pkg(n) + '\n')

print('C2S:', len(c2s), 'S2C:', len(s2c))
