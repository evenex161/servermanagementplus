import re
from pathlib import Path
files = [
  r'neoforge\src\main\java\com\servermanagement\gui\minebay\MineBayMenu.java',
  r'neoforge\src\main\java\com\servermanagement\gui\gambling\MineStacksMenu.java',
  r'fabric\src\main\java\com\servermanagement\gui\minebay\MineBayMenu.java',
  r'fabric\src\main\java\com\servermanagement\gui\gambling\MineStacksMenu.java',
]
# Match: blank line + javadoc + getScaledPanelWidth method + getClientPanelWidth method + javadoc + getScaledVerticalFactor method
pat = re.compile(
  r'\n\s*/\*\*\s*\n\s*\* Get the scaled panel width.*?'
  r'private static float getScaledVerticalFactor\(Inventory playerInventory\)\s*\{.*?\n\s*\}\s*\n',
  re.DOTALL)
for f in files:
    p = Path(f)
    t = p.read_text(encoding='utf-8')
    new = pat.sub('\n', t)
    if new == t:
        print(f'NO CHANGE: {f}')
    else:
        p.write_text(new, encoding='utf-8')
        leftover = [tok for tok in ('vScale','getScaledPanelWidth','getScaledVerticalFactor','getClientPanelWidth') if tok in new]
        print(f'CLEANED: {f}  leftover={leftover}')
