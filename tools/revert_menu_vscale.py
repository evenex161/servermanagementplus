"""Revert vScale/getScaled* helpers from MineBayMenu/MineStacksMenu in all loaders."""
import re
import sys
from pathlib import Path

ROOT = Path(r"C:\VS Workspace\Projects\Minecraft Mods\Servermanagement-Forge")


def revert_minebay(text: str) -> str:
    # Replace constructor preamble
    text = re.sub(
        r"// Calculate scaled panel width AND uniform vertical scale for slot positioning\.\s*\n"
        r"\s*// Both axes must use the same factor so slots stay inside the \(uniformly scaled\)\s*\n"
        r"\s*// panel rectangle drawn by the screen\.\s*\n"
        r"\s*int panelWidth = getScaledPanelWidth\(playerInventory\);\s*\n"
        r"\s*float vScale = getScaledVerticalFactor\(playerInventory\);",
        "// All slot positions are in design space (panel width 600).\n"
        "        // The matching ScalableContainerScreen renders everything through a uniform\n"
        "        // pose-matrix scale, so slots are visually scaled with the panel even when\n"
        "        // the workspace is smaller than the design size.\n"
        "        int panelWidth = 600;",
        text)
    text = text.replace("int offeringY = Math.round(85 * vScale);", "int offeringY = 85;")
    text = text.replace("int offerSlotY = Math.round(148 * vScale);", "int offerSlotY = 148;")
    text = re.sub(
        r"// Position inventory centered at bottom of panel \(Y scaled to match panel height\)",
        "// Position inventory centered at bottom of panel (design-space Y)", text)
    text = text.replace("int inventoryY = Math.round(230 * vScale);", "int inventoryY = 230;")
    # Strip helper methods block
    text = re.sub(
        r"\n\s*/\*\*\s*\n\s*\* Get the scaled panel width.*?private static float getScaledVerticalFactor\(Inventory playerInventory\)\s*\{[^}]*\{[^}]*\}\s*catch[^}]*\{[^}]*\}\s*\}\s*\n",
        "\n", text, flags=re.DOTALL)
    return text


def revert_minestacks(text: str) -> str:
    text = re.sub(
        r"// Calculate scaled panel width AND uniform vertical scale for slot positioning\.\s*\n"
        r"\s*// Both axes must use the same factor so slots stay inside the \(uniformly scaled\)\s*\n"
        r"\s*// panel rectangle drawn by the screen\.\s*\n"
        r"\s*int panelWidth = getScaledPanelWidth\(playerInventory\);\s*\n"
        r"\s*float vScale = getScaledVerticalFactor\(playerInventory\);",
        "// All slot positions are in design space (panel width 400).\n"
        "        // The matching ScalableContainerScreen renders everything through a uniform\n"
        "        // pose-matrix scale, so slots are visually scaled with the panel even when\n"
        "        // the workspace is smaller than the design size.\n"
        "        int panelWidth = 400;",
        text)
    text = text.replace("int betSlotY = Math.round(30 * vScale);", "int betSlotY = 30;")
    text = re.sub(
        r"// Position inventory centered at bottom of panel \(Y scaled to match panel height\)",
        "// Position inventory centered at bottom of panel (design-space Y)", text)
    text = text.replace("int inventoryY = Math.round(140 * vScale);", "int inventoryY = 140;")
    text = re.sub(
        r"\n\s*/\*\*\s*\n\s*\* Get the scaled panel width.*?private static float getScaledVerticalFactor\(Inventory playerInventory\)\s*\{[^}]*\{[^}]*\}\s*catch[^}]*\{[^}]*\}\s*\}\s*\n",
        "\n", text, flags=re.DOTALL)
    return text


def main():
    targets = [
        ("neoforge", "minebay/MineBayMenu.java", revert_minebay, 600),
        ("neoforge", "gambling/MineStacksMenu.java", revert_minestacks, 400),
        ("fabric", "minebay/MineBayMenu.java", revert_minebay, 600),
        ("fabric", "gambling/MineStacksMenu.java", revert_minestacks, 400),
    ]
    for loader, rel, fn, _w in targets:
        path = ROOT / loader / "src" / "main" / "java" / "com" / "servermanagement" / "gui" / rel
        if not path.exists():
            print(f"  {loader}/{rel}: MISSING")
            continue
        original = path.read_text(encoding="utf-8")
        if "vScale" not in original and "getScaledPanelWidth" not in original:
            print(f"  {loader}/{rel}: already reverted")
            continue
        new = fn(original)
        if new == original:
            print(f"  {loader}/{rel}: NO CHANGE")
            continue
        path.write_text(new, encoding="utf-8")
        # sanity check
        leftover = []
        if "vScale" in new:
            leftover.append("vScale")
        if "getScaledPanelWidth" in new:
            leftover.append("getScaledPanelWidth")
        if "getScaledVerticalFactor" in new:
            leftover.append("getScaledVerticalFactor")
        if leftover:
            print(f"  {loader}/{rel}: WROTE but leftover tokens: {leftover}")
        else:
            print(f"  {loader}/{rel}: reverted clean")


if __name__ == "__main__":
    main()
