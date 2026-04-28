import re
from pathlib import Path
files = [
  r'neoforge\src\main\java\com\servermanagement\gui\minebay\MineBayMenu.java',
  r'neoforge\src\main\java\com\servermanagement\gui\gambling\MineStacksMenu.java',
  r'fabric\src\main\java\com\servermanagement\gui\minebay\MineBayMenu.java',
  r'fabric\src\main\java\com\servermanagement\gui\gambling\MineStacksMenu.java',
]
# Match orphaned: '    }\n        try {  ... } catch (Throwable t) {\n            return 1.0f;\n        }\n    }\n'
pat = re.compile(
  r'(\n    \})\n        try \{\s*\n            var mc = net\.minecraft\.client\.Minecraft\.getInstance\(\);\s*\n'
  r'            int guiW = mc\.getWindow\(\)\.getGuiScaledWidth\(\);\s*\n'
  r'            int guiH = mc\.getWindow\(\)\.getGuiScaledHeight\(\);\s*\n'
  r'            return com\.servermanagement\.gui\.ScreenScaler\.scaleFactor\([^)]+\);\s*\n'
  r'        \} catch \(Throwable t\) \{\s*\n'
  r'            return 1\.0f;\s*\n'
  r'        \}\s*\n'
  r'    \}\s*\n')
for f in files:
    p = Path(f); t = p.read_text(encoding='utf-8')
    new, n = pat.subn(r'\1\n', t)
    print(f'{f}: {n} cleanups')
    if n: p.write_text(new, encoding='utf-8')
