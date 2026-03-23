#!/usr/bin/env bash
set -euo pipefail

FILE="app/src/main/java/com/sonara/app/ui/screens/insights/InsightsScreen.kt"

if [[ ! -f "$FILE" ]]; then
  echo "Hata: Dosya bulunamadı:"
  echo "  $FILE"
  exit 1
fi

cp "$FILE" "$FILE.bak"

python3 - <<'PY' "$FILE"
from pathlib import Path
import sys

path = Path(sys.argv[1])
text = path.read_text(encoding="utf-8")

text = text.replace("Icons.Rounded.Bolt", "Icons.Rounded.AutoAwesome")
lines = text.splitlines()
lines = [ln for ln in lines if ln.strip() != "import androidx.compose.material.icons.rounded.Bolt"]

auto_import = "import androidx.compose.material.icons.rounded.AutoAwesome"
if auto_import not in lines:
    out = []
    inserted = False
    for ln in lines:
        out.append(ln)
        if ln.strip() == "import androidx.compose.material.icons.Icons":
            out.append(auto_import)
            inserted = True
    if not inserted:
        newer = []
        done = False
        for ln in out:
            newer.append(ln)
            if not done and ln.startswith("package "):
                newer.append("")
                newer.append(auto_import)
                done = True
        out = newer
    lines = out

path.write_text("\n".join(lines) + "\n", encoding="utf-8")
print("Patch uygulandı:", path)
PY

chmod +x "$FILE" 2>/dev/null || true
echo "Tamam."
