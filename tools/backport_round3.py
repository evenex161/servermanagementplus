#!/usr/bin/env python3
"""Round 3 — fix residual narrow patterns."""
import re
import sys
from pathlib import Path

RULES = [
    # Fix double-qualified types from earlier broken substitutions
    # `net.minecraft.resources.new ResourceLocation(X)` → `net.minecraft.resources.ResourceLocation.tryParse(X)`
    # Wait — semantically we want a NEW ResourceLocation. The original was
    # `net.minecraft.resources.ResourceLocation.parse(X)` which became broken.
    # Fix to `new net.minecraft.resources.ResourceLocation(X)`.
    (re.compile(r"net\.minecraft\.resources\.new ResourceLocation\("),
     "new net.minecraft.resources.ResourceLocation("),

    # Same for advancements package
    (re.compile(r"net\.minecraft\.advancements\.net\.minecraft\.advancements\."),
     "net.minecraft.advancements."),

    # Achievement.display() returns DisplayInfo directly in 1.20.1 (no Optional)
    # callers do `.display().orElse(null)` → `.getDisplay()`
    (re.compile(r"\bholder\.display\(\)\.orElse\(null\)"), "holder.getDisplay()"),
    (re.compile(r"\bholder\.display\(\)"), "java.util.Optional.ofNullable(holder.getDisplay())"),
    (re.compile(r"\bholder\.criteria\(\)"), "holder.getCriteria()"),
    (re.compile(r"\bholder\.parent\(\)"), "java.util.Optional.ofNullable(holder.getParent())"),

    # DisplayInfo.getType() → getFrame() in 1.20.1
    (re.compile(r"\bdisplayInfo\.getType\(\)"), "displayInfo.getFrame()"),

    # File.resolve("X") chained → wrap in toPath()
    # Pattern: `something.resolve("a").resolve("b").resolve("c")`
    # The first .resolve must be on a Path; if it's after toPath()/Paths.get, fine.
    # For chained calls on getServerDirectory(), our round2 rule already produced toPath().
    # Remaining cases use `dir.toPath()...resolve("file")` already, but a common idiom is:
    #   server.getServerDirectory().toPath().resolve("X")
    # which we already handle. The remaining 4 are likely on raw File still — easiest is
    # generic getServerDirectory().toPath() injection wherever followed immediately by .resolve.
    # (already in round 2). Could be other Files. Skip.

    # GameProfile owner — 1.21 uses ResolvableProfile with .name() returning Optional<String>;
    # 1.20.1 uses raw GameProfile with .getName() returning String.
    (re.compile(r"\bowner\.name\(\)\.isPresent\(\)\s*&&\s*owner\.name\(\)\.get\(\)\.equals\("),
     'owner.getName() != null && owner.getName().equals('),

    # Mouse scroll event — 1.21 splits into getDeltaX/getDeltaY; 1.20.1 has getScrollDelta()
    (re.compile(r"\bevent\.getDeltaY\(\)"), "event.getScrollDelta()"),

    # 1.21 introduced super.renderBackground(g, mx, my, partial) — 1.20.1 only takes (g)
    (re.compile(r"super\.renderBackground\(([^,]+),\s*\w+\s*,\s*\w+\s*,\s*\w+\s*\)"),
     r"super.renderBackground(\1)"),
    # renderMenuBackground(g) → renderBackground(g)  [Screen 1.20.1 has no renderMenuBackground]
    (re.compile(r"this\.renderMenuBackground\(([^)]+)\)"), r"this.renderBackground(\1)"),
    # renderPanorama / renderBlurredBackground — drop entirely (1.20.1 has no equivalents)
    (re.compile(r"^\s*this\.renderPanorama\([^)]+\);\s*\n", re.MULTILINE), ""),
    (re.compile(r"^\s*this\.renderBlurredBackground\([^)]+\);\s*\n", re.MULTILINE), ""),

    # mouseScrolled override sig changed: 1.21 (double, double, double, double); 1.20.1 (double, double, double)
    # Convert ONLY the override declaration lines (with `double` types).
    (re.compile(r"\bmouseScrolled\(\s*double\s+(\w+),\s*double\s+(\w+),\s*double\s+\w+,\s*double\s+(\w+)\s*\)"),
     r"mouseScrolled(double \1, double \2, double \3)"),
    # And `super.mouseScrolled(a, b, c, d)` → `super.mouseScrolled(a, b, d)` — restrict to single line
    (re.compile(r"super\.mouseScrolled\(([^,\n]+),\s*([^,\n]+),\s*[^,\n]+,\s*([^)\n]+)\)"),
     r"super.mouseScrolled(\1, \2, \3)"),

    # ItemStack.saveOptional with full qualified ServerLifecycleHooks — broader regex
    (re.compile(
        r"\.saveOptional\(\s*[\w.]*ServerLifecycleHooks\.getCurrentServer\(\)\.registryAccess\(\)\s*\)"
    ), ".save(new CompoundTag())"),
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
