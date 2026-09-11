#!/usr/bin/env python3
"""Package only bridge source and public docs; never bundle pairing keys or games."""
from pathlib import Path
import zipfile

root = Path(__file__).resolve().parent.parent
destination = root / "dist" / "Motion-Air-Windows.zip"
destination.parent.mkdir(exist_ok=True)
files = [root / name for name in (
    "Motion Air.cmd", "Build Ryujinx Motion.cmd", "Motion Air.command",
    "package.json", "package-lock.json", "README.md", "README.es.md",
    "LICENSE", "ACKNOWLEDGEMENTS.md", "RELEASING.md", "install.sh", "android/README.md",
)]
for folder in ("server", "public", "tools", "docs"):
    files += [p for p in (root / folder).rglob("*") if p.is_file()
              and not any(part in {"bin", "obj", "node_modules", "__pycache__", ".DS_Store"} for part in p.relative_to(root).parts)
              and p.suffix not in {".log", ".pyc"}]
with zipfile.ZipFile(destination, "w", zipfile.ZIP_DEFLATED) as archive:
    for source in sorted(set(files)):
        archive.write(source, "Motion Air/" + source.relative_to(root).as_posix())
print(destination)
