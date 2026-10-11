#!/usr/bin/env python3
"""
Checks the generated data/view tree (parents/indices/ids) and renders an
approximation of the layout XML that a.a.a.Ox will emit.
"""
import json
import sys
from collections import defaultdict
from Crypto.Cipher import AES

KEY = b"sketchwaresecure"


def dec(b):
    c = AES.new(KEY, AES.MODE_CBC, KEY)
    d = c.decrypt(b)
    return d[:-d[-1]].decode("utf-8")


def parse(text):
    sections = {}
    cur = None
    for line in text.split("\n"):
        if line.startswith("@"):
            cur = line[1:].strip()
            sections.setdefault(cur, [])
        elif cur is not None and line.strip():
            sections[cur].append(line.strip())
    return sections


def hexcolor(v):
    return "#%08X" % (v & 0xFFFFFFFF)


def dp(v):
    return {-1: "match_parent", -2: "wrap_content"}.get(v, "%ddp" % v)


def attrs(vb, indent):
    a = []
    lay = vb["layout"]
    a.append(('android:layout_width', dp(lay["width"])))
    a.append(('android:layout_height', dp(lay["height"])))
    if vb["convert"] in ("LinearLayout",) and lay["orientation"] in (0, 1):
        a.append(('android:orientation',
                  "vertical" if lay["orientation"] == 1 else "horizontal"))
    if lay["gravity"]:
        a.append(('android:gravity', str(lay["gravity"])))
    if lay["layoutGravity"]:
        a.append(('android:layout_gravity', str(lay["layoutGravity"])))
    if lay["weight"]:
        a.append(('android:layout_weight', str(lay["weight"])))
    for side in ("Left", "Right", "Top", "Bottom"):
        mv = lay["margin" + side]
        if mv:
            a.append(('android:layout_margin' + side, "%ddp" % mv))
        pv = lay["padding" + side]
        if pv:
            a.append(('android:padding' + side, "%ddp" % pv))
    if lay.get("backgroundResource"):
        a.append(('android:background', "@drawable/" + lay["backgroundResource"]))
    elif lay["backgroundColor"] not in (16777215, 0):
        a.append(('android:background', hexcolor(lay["backgroundColor"])))
    t = vb["text"]
    if vb["type"] in (3, 4, 5):
        a.append(('android:text', t["text"]))
        a.append(('android:textSize', "%dsp" % t["textSize"]))
        a.append(('android:textColor', hexcolor(t["textColor"])))
        if t["textType"]:
            a.append(('android:textStyle', {1: "bold", 2: "italic",
                                            3: "bold|italic"}[t["textType"]]))
    if vb["type"] == 5:
        a.append(('android:hint', t["hint"]))
        a.append(('android:textColorHint', hexcolor(t["hintColor"])))
        a.append(('android:inputType', str(t["inputType"])))
        if t["singleLine"]:
            a.append(('android:singleLine', "true"))
    if vb["type"] == 6 and vb["image"]["resName"] != "default_image":
        a.append(('android:src', "@drawable/" + vb["image"]["resName"]))
        a.append(('android:scaleType', vb["image"]["scaleType"]))
    return a


def render(beans, out, depth=0):
    kids = defaultdict(list)
    roots = []
    for b in beans:
        if b["parent"] in (None, "", "root"):
            roots.append(b)
        else:
            kids[b["parent"]].append(b)
    for b in roots:
        emit(b, kids, out, depth)


def emit(b, kids, out, depth):
    pad = "  " * depth
    out.append("%s<%s" % (pad, b["convert"]))
    out.append('%s    android:id="@+id/%s"' % (pad, b["id"]))
    for k, v in attrs(b, depth):
        out.append('%s    %s="%s"' % (pad, k, v))
    children = sorted(kids.get(b["id"], []), key=lambda x: x["index"])
    if children:
        out[-1] += " >"
        for c in children:
            emit(c, kids, out, depth + 1)
        out.append("%s</%s>" % (pad, b["convert"]))
    else:
        out[-1] += " />"


def main(path, view_root_path):
    sections = parse(dec(open(path, "rb").read()))
    roots = json.load(open(view_root_path))
    ok = True
    for name in sorted(sections):
        if name.endswith("_fab"):
            continue
        beans = [json.loads(l) for l in sections[name]]
        ids = [b["id"] for b in beans]
        dupes = {i for i in ids if ids.count(i) > 1}
        per_parent = defaultdict(list)
        for b in beans:
            per_parent[b["parent"]].append(b["index"])
        problems = []
        if dupes:
            problems.append("duplicate ids: %s" % dupes)
        known = set(ids) | {"root"}
        for b in beans:
            if b["parent"] not in known:
                problems.append("unknown parent %r for %s" % (b["parent"], b["id"]))
        for parent, idxs in per_parent.items():
            if len(set(idxs)) != len(idxs):
                problems.append("duplicate index under %s" % parent)
        if name not in roots:
            problems.append("missing entry in view_root")

        print("=" * 72)
        print(name, "->", "%d views" % len(beans),
              "OK" if not problems else "PROBLEMS")
        print("=" * 72)
        for p in problems:
            ok = False
            print("  !!", p)
        out = []
        render(beans, out)
        print("\n".join(out))
        print()
    print("RESULT:", "OK" if ok else "FAILED")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1], sys.argv[2]))
