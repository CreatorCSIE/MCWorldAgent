#!/usr/bin/env python3
"""B2-2 generic anchor-vs-tiny reverse recon (fingerprint landing, batch 1).

Direction is inverted vs b2-1: instead of per-slot SEMANTICS named triples,
consume the shipped anchor JSON and look each official triple up in the
version's client.tiny. Output = coverage + named semantics + gap list.

Schemas handled:
  - legacy: targets {slot: {class,name,descriptor}}  (0327/a1.2.0/b1.2_02)
  - runtime: hooks {NAME: {class,method,desc}} + ops bare strings
             (b1.7.3/b1.8.1) -- ops slots carry NO class/desc context,
             so they are counted as UNRECONCILABLE-OFFLINE (finding F-2).

Tiny source map (README ruling: b1.2_02 borrows b1.2 tiny):
  inf-20100327 -> inf-20100327.zip   a1.2.0 -> a1.2.0-a1.2.1_01.zip
  b1.2_02      -> b1.2.zip           b1.7.3 -> b1.7.zip   b1.8.1 -> b1.8.zip
"""
import io
import json
import os
import sys
import zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(HERE))
FPDIR = os.path.join(ROOT, "java", "agent", "registry", "fingerprints")
VERDIR = os.path.join(ROOT, "tools", "retromcp", "pages", "versions")

VERSIONS = [
    ("inf-20100327", "inf-20100327.zip"),
    ("a1.2.0", "a1.2.0-a1.2.1_01.zip"),
    ("b1.2_02", "b1.2.zip"),
    ("b1.7.3", "b1.7.zip"),
    ("b1.8.1", "b1.8.zip"),
]


def read_tiny(zname):
    zp = os.path.join(VERDIR, zname)
    with zipfile.ZipFile(zp) as z:
        with z.open("client.tiny") as f:
            text = io.TextIOWrapper(f, encoding="utf-8").read()
    classes = {}  # official -> named
    members = {}  # (official class, kind, official name) -> (desc, named)
    cur = None
    for line in text.split("\n"):
        if not line:
            continue
        parts = line.split("\t")
        if parts[0] == "":
            parts = parts[1:]  # member lines are indented with a leading tab
        if parts[0] == "c":
            if len(parts) >= 3:
                classes[parts[1]] = parts[2]
                cur = parts[1]
            else:
                cur = None
        elif parts[0] in ("f", "m", "s") and cur is not None:
            # tiny v2: kind, desc, official, named  (v1: kind, official, named)
            if len(parts) == 4:
                kind, desc, off, named = parts
            elif len(parts) == 3:
                kind, off, named = parts
                desc = ""
            else:
                continue
            members[(cur, kind, off, desc)] = named
    return classes, members


def legacy_slots(path):
    with open(path, encoding="utf-8") as f:
        j = json.load(f)
    out = []
    for slot, t in j.get("targets", {}).items():
        if t is None:
            continue
        out.append((slot, t["class"], t["name"], t["descriptor"]))
    nulls = sum(1 for t in j.get("targets", {}).values() if t is None)
    return out, nulls


def runtime_slots(path):
    with open(path, encoding="utf-8") as f:
        j = json.load(f)
    out = []
    for hook, h in j.get("hooks", {}).items():
        out.append((hook, h["class"], h["method"], h["desc"]))
    ops = j.get("ops", {})
    return out, ops, j


def norm_class(cls):
    # legacy anchors use dot form (net.minecraft.a.a.f); tiny uses slashes.
    if "/" not in cls and "." in cls:
        return cls.replace(".", "/")
    return cls


def recon(triples, classes, members):
    rows = []
    for slot, cls, name, desc in triples:
        cls = norm_class(cls)
        if cls not in classes:
            rows.append((slot, cls, name, desc, "CLASS-GAP", ""))
            continue
        if not name or name == "None":
            rows.append((slot, cls, name, desc, "MATCH", "class->" + classes[cls]))
            continue
        named = None
        for kind in ("m", "f", "s"):
            if (cls, kind, name, desc) in members:
                named = members[(cls, kind, name, desc)]
                break
        if named is not None:
            rows.append((slot, cls, name, desc, "MATCH", named))
            continue
        cands = [(k[1], k[3], v) for k, v in members.items() if k[0] == cls and k[2] == name]
        if cands:
            rows.append((slot, cls, name, desc, "DESC-DIFF",
                         " ".join("%s%s->%s" % c for c in cands)))
        else:
            rows.append((slot, cls, name, desc, "NAME-GAP", ""))
    return rows


def main():
    lines = []
    for vid, zname in VERSIONS:
        jpath = os.path.join(FPDIR, vid + ".json")
        classes, members = read_tiny(zname)
        with open(jpath, encoding="utf-8") as f:
            head = json.load(f)
        schema = "runtime" if "hooks" in head else "legacy"
        lines.append("=== %s (schema=%s, tiny=%s, classes=%d members=%d) ===" % (
            vid, schema, zname, len(classes), len(members)))
        if schema == "legacy":
            triples, nulls = legacy_slots(jpath)
            if nulls:
                lines.append("anchor-null slots (intentional absence): %d" % nulls)
        else:
            triples, ops, _ = runtime_slots(jpath)
            bare = [k for k, v in ops.items() if isinstance(v, str)]
            lines.append("ops bare-name slots (unreconcilable offline, F-2): %d" % len(bare))
        rows = recon(triples, classes, members)
        counts = {}
        for r in rows:
            counts[r[4]] = counts.get(r[4], 0) + 1
        lines.append("reconciled slots: %d  %s" % (len(rows), counts))
        for slot, cls, name, desc, state, named in rows:
            if state != "MATCH":
                lines.append("  [%s] %s = %s.%s%s  tiny:%s" % (state, slot, cls, name, desc, named))
            else:
                lines.append("  [MATCH] %s = %s.%s%s -> %s" % (slot, cls, name, desc, named))
        lines.append("")
    txt = "\n".join(lines)
    out = os.path.join(HERE, "report.md")
    with open(out, "w", encoding="utf-8") as f:
        f.write("# B2-2 anchor-vs-tiny reverse recon (batch 1, 2026-11-20)\n\n```text\n" + txt + "\n```\n")
    # console: gaps only
    for l in lines:
        if l.startswith("===") or "NAME-GAP" in l or "CLASS-GAP" in l or "DESC-DIFF" in l or "unreconcilable" in l or "reconciled" in l:
            print(l)
    print("report -> " + out)


if __name__ == "__main__":
    main()
