"""
Migrate all AbstractContainerScreen subclasses to ScalableContainerScreen.

For each screen file:
  1. extends AbstractContainerScreen<X>  -> extends ScalableContainerScreen<X>
  2. add import com.servermanagement.gui.ScalableContainerScreen
  3. remove import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
  4. constructor: super(menu, inv, title);  -> super(menu, inv, title, DESIGN_W, DESIGN_H);
     where DESIGN_W and DESIGN_H come from this.imageWidth = N; this.imageHeight = M;
     constants in the constructor (or from the ScreenScaler.scale(W, H, ...) call in init()).
  5. init() body: remove the `int[] dim = ScreenScaler.scale(W, H, ...);
                              this.imageWidth = dim[0];
                              this.imageHeight = dim[1];` block
  6. render override: change signature `public void render(` -> `protected void renderContent(`
     and change super.render( -> super.renderContent( inside the method body.
"""
import re
import sys
from pathlib import Path

ROOT = Path(r"C:\VS Workspace\Projects\Minecraft Mods\Servermanagement-Forge")

# Screens to migrate (ItemPickerScreen extends Screen, not AbstractContainerScreen, skip)
SCREENS = [
    "gui/screen/DashboardScreen.java",
    "gui/screen/ConfigScreen.java",
    "gui/screen/ConsoleScreen.java",
    "gui/screen/PerformanceSettingsScreen.java",
    "gui/screen/PlayerManagerScreen.java",
    "gui/screen/PortalTimerScreen.java",
    "gui/screen/MotdEditorScreen.java",
    "gui/screen/WorldDetailScreen.java",
    "gui/screen/WorldListScreen.java",
    "gui/screen/GlobalSettingsScreen.java",
    "gui/screen/ServerManagementScreen.java",
    "gui/economy/AchievementsScreen.java",
    "gui/economy/BankScreen.java",
    "gui/economy/DailyTasksScreen.java",
    "gui/economy/EconomyManagementScreen.java",
    "gui/gambling/MineStacksScreen.java",
    "gui/minebay/MineBayScreen.java",
]


def migrate(text: str, filepath: Path) -> tuple[str, list[str]]:
    notes = []
    orig = text

    # 1. Extract design dimensions from `int[] dim = ScreenScaler.scale(W, H, ...)`
    m = re.search(r"int\[\]\s*dim\s*=\s*ScreenScaler\.scale\(\s*([A-Za-z_0-9]+)\s*,\s*([A-Za-z_0-9]+)\s*,",
                  text)
    if not m:
        notes.append("WARN: no ScreenScaler.scale(W,H,...) call found in init()")
        # Fall back to extracting from constructor `this.imageWidth = N` etc.
        return text, notes

    design_w = m.group(1)
    design_h = m.group(2)
    notes.append(f"design = {design_w} x {design_h}")

    # 2. Replace extends AbstractContainerScreen<X> -> extends ScalableContainerScreen<X>
    new_text, n = re.subn(r"\bextends\s+AbstractContainerScreen\s*<",
                          "extends ScalableContainerScreen<", text)
    if n != 1:
        notes.append(f"WARN: {n} extends-replacements (expected 1)")
    text = new_text

    # 3. Remove AbstractContainerScreen import
    text, n = re.subn(
        r"^import\s+net\.minecraft\.client\.gui\.screens\.inventory\.AbstractContainerScreen;\s*\n",
        "", text, flags=re.MULTILINE)
    if n != 1:
        notes.append(f"WARN: {n} AbstractContainerScreen-import removals (expected 1)")

    # 4. Add ScalableContainerScreen import (placed just before any existing
    # com.servermanagement.gui.* import or after the package line).
    if "com.servermanagement.gui.ScalableContainerScreen" not in text:
        # insert before first com.servermanagement import, or after package line
        m2 = re.search(r"^(package\s+[^\n]+;\s*\n)", text)
        if m2:
            insert_at = m2.end()
            text = (text[:insert_at]
                    + "\nimport com.servermanagement.gui.ScalableContainerScreen;\n"
                    + text[insert_at:])

    # 5. Constructor super(menu, inv, title) -> super(menu, inv, title, DESIGN_W, DESIGN_H)
    # Match: super(<3 args>);  - take the first super() call
    text, n = re.subn(
        r"super\(\s*([A-Za-z_0-9]+)\s*,\s*([A-Za-z_0-9]+)\s*,\s*([A-Za-z_0-9]+)\s*\)\s*;",
        lambda m: f"super({m.group(1)}, {m.group(2)}, {m.group(3)}, {design_w}, {design_h});",
        text, count=1)
    if n != 1:
        notes.append(f"WARN: {n} super(menu,inv,title) replacements (expected 1)")

    # 6. Remove `int[] dim = ScreenScaler.scale(W, H, this.width, this.height);
    #            this.imageWidth = dim[0];
    #            this.imageHeight = dim[1];`
    text, n = re.subn(
        r"\n[ \t]*int\[\]\s*dim\s*=\s*ScreenScaler\.scale\([^;]+\);\s*\n[ \t]*this\.imageWidth\s*=\s*dim\[0\];\s*\n[ \t]*this\.imageHeight\s*=\s*dim\[1\];\s*\n",
        "\n", text, count=1)
    if n != 1:
        notes.append(f"WARN: {n} ScreenScaler.scale block removals (expected 1)")

    # 7. Rename render override: `public void render(GuiGraphics ..., int mouseX, int mouseY, float partialTick)`
    # -> `protected void renderContent(GuiGraphics ..., int mouseX, int mouseY, float partialTick)`
    text, n = re.subn(
        r"public\s+void\s+render\(\s*GuiGraphics",
        "protected void renderContent(GuiGraphics",
        text)
    notes.append(f"render->renderContent: {n} rename(s)")

    # 8. Inside renderContent: `super.render(<args>)` -> `super.renderContent(<args>)`
    # Only those calls that pass `mouseX, mouseY, partialTick` style
    text, n = re.subn(
        r"super\.render\(\s*([A-Za-z_0-9]+)\s*,\s*([A-Za-z_0-9]+)\s*,\s*([A-Za-z_0-9]+)\s*,\s*([A-Za-z_0-9]+)\s*\)",
        r"super.renderContent(\1, \2, \3, \4)",
        text)
    notes.append(f"super.render->super.renderContent: {n} rename(s)")

    # 9. Remove ScreenScaler import if no longer used
    if "ScreenScaler" not in re.sub(r"^import\s+[^;\n]*ScreenScaler[^;\n]*;\s*\n", "", text,
                                     flags=re.MULTILINE):
        text, n = re.subn(r"^import\s+com\.servermanagement\.gui\.ScreenScaler;\s*\n",
                          "", text, flags=re.MULTILINE)
        if n:
            notes.append("removed unused ScreenScaler import")

    return text, notes


def main(loader: str):
    base = ROOT / loader / "src" / "main" / "java" / "com" / "servermanagement"
    for rel in SCREENS:
        path = base / rel
        if not path.exists():
            print(f"  {rel}: MISSING")
            continue
        original = path.read_text(encoding="utf-8")
        if "extends ScalableContainerScreen" in original:
            print(f"  {rel}: already migrated, skipping")
            continue
        migrated, notes = migrate(original, path)
        if migrated == original:
            print(f"  {rel}: NO CHANGE  ({'; '.join(notes)})")
            continue
        path.write_text(migrated, encoding="utf-8")
        print(f"  {rel}: migrated  ({'; '.join(notes)})")


if __name__ == "__main__":
    loader = sys.argv[1] if len(sys.argv) > 1 else "forge"
    print(f"=== migrating loader: {loader} ===")
    main(loader)
