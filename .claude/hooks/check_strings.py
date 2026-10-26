"""English / Arabic string parity check.

Hook mode (PostToolUse Write|Edit): reads the hook JSON on stdin and only runs when the edited
file is res/values/strings.xml or res/values-ar/strings.xml. Problems are reported to Claude
(exit 2 + stderr) without blocking anything.

CLI mode: `python .claude/hooks/check_strings.py --all` checks unconditionally (used by /verify);
exit 1 when the files disagree.
"""
import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
RES = os.path.join(ROOT, "app", "src", "main", "res")
EN = os.path.join(RES, "values", "strings.xml")
AR = os.path.join(RES, "values-ar", "strings.xml")

NAME = re.compile(r'<(?:string|plurals)\s+name="([^"]+)"')
STRING = re.compile(r'<string\s+name="([^"]+)"[^>]*>(.*?)</string>', re.S)
PLACEHOLDER = re.compile(r"%(?:\d+\$)?[sdf]")


def read(path):
    with open(path, encoding="utf-8") as f:
        return f.read()


def problems():
    en, ar = read(EN), read(AR)
    en_keys, ar_keys = set(NAME.findall(en)), set(NAME.findall(ar))
    out = []
    missing = sorted(en_keys - ar_keys)
    extra = sorted(ar_keys - en_keys)
    if missing:
        out.append("missing in values-ar/strings.xml: " + ", ".join(missing))
    if extra:
        out.append("only in values-ar (not in values/strings.xml): " + ", ".join(extra))
    en_ph = {k: sorted(PLACEHOLDER.findall(v)) for k, v in STRING.findall(en)}
    ar_ph = {k: sorted(PLACEHOLDER.findall(v)) for k, v in STRING.findall(ar)}
    bad = [k for k in en_ph if k in ar_ph and en_ph[k] != ar_ph[k]]
    if bad:
        out.append("placeholders differ (%s / %d): " + ", ".join(bad))
    return out


def main():
    if "--all" in sys.argv:
        found = problems()
        if found:
            print("String parity check FAILED:\n  " + "\n  ".join(found))
            return 1
        print("String parity OK: values/ and values-ar/ have the same keys and placeholders.")
        return 0

    try:
        payload = json.load(sys.stdin)
    except ValueError:
        return 0
    tool_input = payload.get("tool_input") or {}
    path = (tool_input.get("file_path") or (payload.get("tool_response") or {}).get("filePath") or "")
    norm = path.replace("\\", "/")
    if not (norm.endswith("/res/values/strings.xml") or norm.endswith("/res/values-ar/strings.xml")):
        return 0
    found = problems()
    if not found:
        return 0
    sys.stderr.write("English/Arabic strings are out of sync (every UI string must exist in both "
                     "res/values/strings.xml and res/values-ar/strings.xml):\n  " + "\n  ".join(found) + "\n")
    return 2


if __name__ == "__main__":
    sys.exit(main())
