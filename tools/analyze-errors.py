#!/usr/bin/env python3
"""Categorize compile errors from build-errors.txt."""
import re
from pathlib import Path
from collections import Counter

text = Path("build-errors.txt").read_text(encoding="utf-8", errors="replace")

# Find every "symbol:   class X" / "variable X" / "method X" reference
syms = re.findall(r"symbol:\s+(\w+(?:\s+\S+)?)", text)
print("=== Top missing symbols ===")
for sym, cnt in Counter(syms).most_common(40):
    print(f"  {cnt:4d}  {sym}")

# Find missing packages
pkgs = re.findall(r"package (\S+) does not exist", text)
print("\n=== Missing packages ===")
for pkg, cnt in Counter(pkgs).most_common(20):
    print(f"  {cnt:4d}  {pkg}")

# Find files with most errors
files = re.findall(r"servermanagement\\([^:]+):\d+:\s+error:", text)
print("\n=== Files with most errors ===")
for f, cnt in Counter(files).most_common(20):
    print(f"  {cnt:4d}  {f}")
