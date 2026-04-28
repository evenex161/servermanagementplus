import re
from pathlib import Path

n = 0
pat = re.compile(r'tag\.put\("([^"]+)",\s*([^)]+)\.save\(new CompoundTag\(\)\)\)\);')
for f in Path('fabric/src/main/java').rglob('*.java'):
    t = f.read_text(encoding='utf-8')
    o = t
    t = pat.sub(r'tag.put("\1", \2.save(new CompoundTag()));', t)
    if t != o:
        f.write_text(t, encoding='utf-8')
        n += 1
        print(f)
print('files:', n)
