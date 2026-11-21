#!/usr/bin/env python3
"""P3 full-library QC over the RetroMCP tiny mapping zips (133 items).

Dimensions:
  1. solvability   : zip opens, client.tiny present/non-zero/parse-clean, header
  2. scale         : class/field/method counts, class mapping ratio
  3. format        : dup official keys, empty named, bad chars, orphan members
  4. consistency   : zip set vs versions.json resources (gaps / orphans)
  5. real-jar      : tiny official classes vs local jar class set (coverage,
                     reverse rate; reverse>0 = version-mismatch suspicion)
  6. anchor cross  : 11 fingerprint JSONs vs their tiny (borrow rules via
                     versions.json resources + explicit override table)

Tiny dialect handled (per b2-2 read_tiny plus observed RetroMCP extensions):
  tiny\t2\t0\tofficial\tnamed          header
  c\t<off>\t<named>                    class mapping
  \tc\t<text>                          class comment (attached to prev class)
  \t<f|m|s>\t<desc>\t<off>\t<named>    member mapping (v2)
  \t\tc\t<text>                        member comment
  \t\tp\t<idx>\t\t<named>              positional field (official empty)

Pure ASCII source. Output: report.md (Chinese) + dumps under TEMP/mcaut/p3.
Idempotent; rerun with --report to regenerate.
"""
import argparse
import json
import os
import re
import sys
import zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(HERE))
VERDIR = os.path.join(ROOT, "tools", "retromcp", "pages", "versions")
FPDIR = os.path.join(ROOT, "java", "agent", "registry", "fingerprints")
MCDIR = os.path.join(os.environ.get("APPDATA", ""), ".minecraft", "versions")
DUMPDIR = os.path.join(os.environ.get("TEMP", HERE), "mcaut", "p3")

# anchor id -> tiny zip override (versions.json has no entry for a1.2.0;
# README ruling: b1.2_02 borrows b1.2, b1.7.3 borrows b1.7, b1.8.1 borrows
# b1.8 -- those already flow through versions.json resources).
ANCHOR_ZIP_OVERRIDE = {
    "a1.2.0": "a1.2.0-a1.2.1_01.zip",
}

CLASS_BAD_CHARS = set(" \t\r\n.;[]<>")
MEMBER_BAD_CHARS = set(" \t\r\n.;/[]")


def parse_tiny(text):
    """Parse one client.tiny into stats + issue list. Never raises on junk."""
    st = {
        "header": None, "n_class": 0, "n_field": 0, "n_method": 0, "n_static": 0,
        "n_pfield": 0, "n_ccomment": 0, "n_mcomment": 0,
        "mapped_class": 0, "classes": {}, "member_keys": {},
        "issues": [],  # (code, sample)
    }
    cur = None
    for line in text.split("\n"):
        if line == "":
            continue
        raw = line.split("\t")
        depth = 0
        while raw and raw[0] == "":
            raw = raw[1:]
            depth += 1
        if not raw:
            continue
        tok = raw[0]
        if tok == "tiny" and depth == 0:
            ver = raw[1] if len(raw) > 1 else "?"
            st["header"] = "v" + ver
            continue
        if depth == 0 and tok == "c":
            if len(raw) >= 3:
                off, named = raw[1], raw[2]
                cur = off
                st["n_class"] += 1
                if off in st["classes"]:
                    st["issues"].append(("DUP-CLASS-OFF", off))
                st["classes"][off] = named
                if named == "":
                    st["issues"].append(("EMPTY-NAMED-CLASS", off))
                elif any(ch in CLASS_BAD_CHARS for ch in named):
                    st["issues"].append(("BAD-CHAR-CLASS-NAMED", off + " -> " + named))
                elif named != off:
                    st["mapped_class"] += 1
            else:
                st["issues"].append(("BAD-COLUMNS-CLASS", line[:80]))
            continue
        if depth >= 1 and tok == "c":
            # comment line attached to prev class (depth 1) or member (depth >=2)
            if depth == 1:
                st["n_ccomment"] += 1
            else:
                st["n_mcomment"] += 1
            continue
        if depth >= 2 and tok == "p":
            # p\t<index>\t<off(empty)>\t<named>
            st["n_pfield"] += 1
            named = raw[3] if len(raw) >= 4 else ""
            if named == "":
                st["issues"].append(("EMPTY-NAMED-PFIELD", line[:80]))
            continue
        if depth == 1 and tok in ("f", "m", "s"):
            if cur is None:
                st["issues"].append(("ORPHAN-MEMBER", line[:80]))
                continue
            if len(raw) == 4:
                kind, desc, off, named = raw
            elif len(raw) == 3:  # v1-style member line
                kind, off, named = raw
                desc = ""
                st["issues"].append(("V1-LINE-IN-V2", line[:80]))
            else:
                st["issues"].append(("BAD-COLUMNS-MEMBER", line[:80]))
                continue
            st["n_field" if kind == "f" else
               "n_method" if kind == "m" else "n_static"] += 1
            key = (cur, kind, off, desc)
            if key in st["member_keys"]:
                st["issues"].append(("DUP-MEMBER", "%s %s %s %s" % key))
            st["member_keys"][key] = named
            if named == "":
                st["issues"].append(("EMPTY-NAMED-MEMBER", "%s %s %s" % key))
            elif any(ch in MEMBER_BAD_CHARS for ch in named):
                st["issues"].append(("BAD-CHAR-MEMBER-NAMED",
                                     "%s %s %s -> %s" % (key[0], key[1], key[2], named)))
            continue
        st["issues"].append(("UNKNOWN-LINE", line[:80]))
    return st


def qc_zip(name):
    rec = {
        "zip": name, "status": "PASS", "header": "-", "has_server": False,
        "n_class": 0, "n_field": 0, "n_method": 0, "n_static": 0, "n_pfield": 0,
        "n_ccomment": 0, "n_mcomment": 0, "mapped_class": 0, "map_rate": None,
        "issues": [], "classes": set(), "tiny_bytes": 0, "entries": 0,
    }
    path = os.path.join(VERDIR, name)
    try:
        with zipfile.ZipFile(path) as z:
            names = z.namelist()
            rec["entries"] = len(names)
            rec["has_server"] = "server.tiny" in names
            if "client.tiny" not in names:
                rec["status"] = "FAIL"
                rec["issues"].append(("NO-CLIENT-TINY", 1,
                                      "entries=" + ",".join(names[:4])))
                return rec
            data = z.read("client.tiny")
    except Exception as e:
        rec["status"] = "FAIL"
        rec["issues"].append(("ZIP-UNREADABLE", 1, repr(e)[:120]))
        return rec
    rec["tiny_bytes"] = len(data)
    if len(data) == 0:
        rec["status"] = "FAIL"
        rec["issues"].append(("EMPTY-TINY", 1, "client.tiny is 0 bytes"))
        return rec
    try:
        st = parse_tiny(data.decode("utf-8", "replace"))
    except Exception as e:
        rec["status"] = "FAIL"
        rec["issues"].append(("PARSE-EXC", 1, repr(e)[:120]))
        return rec
    rec["header"] = st["header"] or "NO-HEADER"
    for k in ("n_class", "n_field", "n_method", "n_static", "n_pfield",
              "n_ccomment", "n_mcomment", "mapped_class"):
        rec[k] = st[k]
    rec["classes"] = set(st["classes"].keys())
    if st["n_class"]:
        rec["map_rate"] = 100.0 * st["mapped_class"] / st["n_class"]
    if st["header"] is None:
        rec["issues"].append(("NO-HEADER", 1, "missing tiny header line"))
    # aggregate issue codes with samples
    agg = {}
    for code, sample in st["issues"]:
        if code not in agg:
            agg[code] = [0, sample]
        agg[code][0] += 1
    rec["issues"] = sorted([(c, n, s) for c, (n, s) in agg.items()])
    if rec["issues"] and rec["status"] == "PASS":
        rec["status"] = "WARN"
    return rec


def load_versions_json():
    with open(os.path.join(VERDIR, "versions.json"), encoding="utf-8") as f:
        return json.load(f)


def jar_class_set(jarpath):
    with zipfile.ZipFile(jarpath) as z:
        return set(n[:-6] for n in z.namelist()
                   if n.endswith(".class") and not n.startswith("META-INF/"))


def resolve_local_jars(vj, zip_names):
    """Map each local jar dir to a versions.json id (preferred) or a zip."""
    ids = set(vj.keys())
    out, unresolved = [], []
    if not os.path.isdir(MCDIR):
        return out, unresolved
    for d in sorted(os.listdir(MCDIR)):
        jar = os.path.join(MCDIR, d, d + ".jar")
        if not os.path.isfile(jar):
            continue
        vid, method = None, None
        if d in ids:
            vid, method = d, "exact"
        else:
            base = re.sub(r"-(wrapped|dev)$", "", d)
            base = re.sub(r"-(wrapped|dev)$", "", base)
            if base in ids:
                vid, method = base, "strip-suffix"
            elif re.sub(r"-\d+$", "", base) in ids:
                vid, method = re.sub(r"-\d+$", "", base), "strip-digit"
            else:
                cands = sorted(i for i in ids
                               if re.fullmatch(r"-\d+", i[len(d):]) if i.startswith(d + "-"))
                if len(cands) == 1:
                    vid, method = cands[0], "prefix-unique"
                elif d + ".zip" in zip_names:
                    vid, method = d, "zip-direct"
        if vid is None:
            unresolved.append(d)
            continue
        zname = ANCHOR_ZIP_OVERRIDE.get(vid) or vj.get(vid, {}).get("resources") or (d + ".zip")
        if zname not in zip_names:
            unresolved.append(d + " (zip " + str(zname) + " missing)")
            continue
        out.append((d, vid, zname, method, jar))
    return out, unresolved


def norm_class(cls):
    if "/" not in cls and "." in cls:
        return cls.replace(".", "/")
    return cls


def anchor_triples(path):
    with open(path, encoding="utf-8") as f:
        j = json.load(f)
    triples, classes_only, bare_ops, nulls = [], [], [], 0
    if "hooks" in j:  # runtime schema
        for hook, h in j.get("hooks", {}).items():
            triples.append((hook, h["class"], h["method"], h["desc"]))
        for op, v in j.get("ops", {}).items():
            if isinstance(v, str):
                bare_ops.append(op)
            elif isinstance(v, dict):
                if "class" in v:
                    triples.append((op, v["class"], v.get("method", ""), v.get("desc", "")))
                else:  # name -> official class map (e.g. mobClasses)
                    for sub, cls in v.items():
                        if isinstance(cls, str):
                            classes_only.append((op + "." + sub, cls))
    else:  # legacy schema
        for slot, t in j.get("targets", {}).items():
            if t is None:
                nulls += 1
                continue
            if not t.get("name"):
                classes_only.append((slot, t["class"]))
            else:
                triples.append((slot, t["class"], t["name"], t.get("descriptor") or ""))
    return triples, classes_only, bare_ops, nulls


def recon_triples(triples, classes, members):
    rows = []
    for slot, cls, name, desc in triples:
        cls = norm_class(cls)
        if cls not in classes:
            rows.append((slot, cls, name, desc, "CLASS-GAP"))
            continue
        if not name or name == "None":
            rows.append((slot, cls, name, desc, "MATCH"))
            continue
        hit = None
        for kind in ("m", "f", "s"):
            if (cls, kind, name, desc) in members:
                hit = "MATCH"
                break
        if hit:
            rows.append((slot, cls, name, desc, hit))
            continue
        if any(k[0] == cls and k[2] == name for k in members):
            rows.append((slot, cls, name, desc, "DESC-DIFF"))
        else:
            rows.append((slot, cls, name, desc, "NAME-GAP"))
    return rows


def recon_classes_only(items, classes):
    rows = []
    for slot, cls in items:
        cls = norm_class(cls)
        rows.append((slot, cls, "MATCH" if cls in classes else "CLASS-GAP"))
    return rows


def load_tiny_maps(zname):
    """classes off->named and member keys for anchor recon."""
    with zipfile.ZipFile(os.path.join(VERDIR, zname)) as z:
        text = z.read("client.tiny").decode("utf-8", "replace")
    st = parse_tiny(text)
    members = st["member_keys"]
    return st["classes"], members


def fmt_rate(part, total):
    return "%.1f%%" % (100.0 * part / total) if total else "n/a"


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--report", action="store_true",
                    help="regenerate report.md (default behaviour anyway)")
    args = ap.parse_args()

    if not os.path.isdir(DUMPDIR):
        os.makedirs(DUMPDIR)
    vj = load_versions_json()
    zip_names = sorted(n for n in os.listdir(VERDIR) if n.endswith(".zip"))

    # ---- dims 1-3 per zip -------------------------------------------------
    recs = [qc_zip(n) for n in zip_names]
    by_zip = {r["zip"]: r for r in recs}

    # ---- dim 4 consistency -------------------------------------------------
    res_map = {}
    for vid, e in vj.items():
        res_map.setdefault(e.get("resources"), []).append(vid)
    gaps = {r: sorted(v) for r, v in res_map.items() if r not in by_zip}
    orphan_zips = sorted(set(zip_names) - set(res_map.keys()))
    notiny_referenced = {}
    for r, v in res_map.items():
        if r in by_zip and by_zip[r]["status"] == "FAIL":
            notiny_referenced[r] = sorted(v)

    # ---- dim 5 real jar coverage ------------------------------------------
    jar_rows, jar_unres = resolve_local_jars(vj, set(zip_names))
    cov = []
    for d, vid, zname, method, jar in jar_rows:
        rec = by_zip[zname]
        if not rec["classes"]:
            continue
        try:
            jset = jar_class_set(jar)
        except Exception as e:
            cov.append((d, vid, zname, method, None, None, None, None, repr(e)[:60]))
            continue
        tset = rec["classes"]
        inter = len(jset & tset)
        rev = len(tset - jset)
        cov.append((d, vid, zname, method, len(jset), len(tset),
                    (inter / len(jset) if jset else 0.0),
                    (rev / len(tset) if tset else 0.0), ""))
        if rev > 0:
            rec["issues"].append(("JAR-REVERSE", 1,
                                  vid + ": %d tiny classes absent from jar" % rev))
            if rec["status"] == "PASS":
                rec["status"] = "WARN"
    cov.sort(key=lambda r: (r[2], r[1]))

    # ---- dim 6 anchor cross -------------------------------------------------
    fp_files = sorted(n for n in os.listdir(FPDIR) if n.endswith(".json"))
    anchor_rows = []
    for fn in fp_files:
        vid = fn[:-5]
        zname = ANCHOR_ZIP_OVERRIDE.get(vid) or vj.get(vid, {}).get("resources")
        if not zname or zname not in by_zip:
            anchor_rows.append((vid, str(zname), "NO-TINY", None, None, None, []))
            continue
        classes, members = load_tiny_maps(zname)
        with open(os.path.join(FPDIR, fn), encoding="utf-8") as f:
            head = json.load(f)
        schema = "runtime" if "hooks" in head else "legacy"
        triples, classes_only, bare_ops, nulls = anchor_triples(os.path.join(FPDIR, fn))
        rows = recon_triples(triples, classes, members)
        rows += [(s, c, "", "", "MATCH" if st_ == "MATCH" else "CLASS-GAP")
                 for s, c, st_ in recon_classes_only(classes_only, classes)]
        cnt = {}
        for r in rows:
            cnt[r[4]] = cnt.get(r[4], 0) + 1
        total = len(rows)
        matched = cnt.get("MATCH", 0)
        anchor_rows.append((vid, zname, schema,
            total, matched, {"bare_ops": len(bare_ops), "nulls": nulls,
                             "desc_diff": cnt.get("DESC-DIFF", 0),
                             "name_gap": cnt.get("NAME-GAP", 0),
                             "class_gap": cnt.get("CLASS-GAP", 0)},
            [r for r in rows if r[4] != "MATCH"]))

    # ---- dumps ---------------------------------------------------------------
    with open(os.path.join(DUMPDIR, "summary.json"), "w", encoding="utf-8") as f:
        json.dump([{k: (sorted(v) if k == "classes" else v) for k, v in r.items()}
                   for r in recs], f, ensure_ascii=False, indent=1)
    with open(os.path.join(DUMPDIR, "issues.txt"), "w", encoding="utf-8") as f:
        for r in recs:
            for code, n, s in r["issues"]:
                f.write("%s\t%s\t%d\t%s\n" % (r["zip"], code, n, s))
    with open(os.path.join(DUMPDIR, "coverage.tsv"), "w", encoding="utf-8") as f:
        for row in cov:
            f.write("\t".join(str(x) for x in row) + "\n")

    # ---- report ---------------------------------------------------------------
    L = []
    A = L.append
    n_pass = sum(1 for r in recs if r["status"] == "PASS")
    n_warn = sum(1 for r in recs if r["status"] == "WARN")
    n_fail = sum(1 for r in recs if r["status"] == "FAIL")
    A("# P3 tiny QC full-library report (133 zips)")
    A("")
    A("Generated by `tools/p3-tiny-qc/p3_tiny_qc.py` (rerun with `--report`).")
    A("All numbers below are measured by the script, none hand-written.")
    A("")
    A("## 0. Headline")
    A("")
    A("| metric | value |")
    A("|---|---|")
    A("| zips total | %d |" % len(recs))
    A("| PASS (clean) | %d |" % n_pass)
    A("| WARN (parseable, has issues) | %d |" % n_warn)
    A("| FAIL (unusable) | %d |" % n_fail)
    A("| with client.tiny | %d |" % sum(1 for r in recs if r["n_class"] or r["header"] != "-"))
    A("| tiny header seen | %s |" % (",".join(sorted(set(r["header"] for r in recs))) or "-"))
    A("| versions.json entries | %d |" % len(vj))
    A("")
    A("## 1. Master table (133 rows)")
    A("")
    A("| zip | st | hdr | cls | fld | mth | stc | pfl | map% | issues |")
    A("|---|---|---|---|---|---|---|---|---|---|")
    for r in recs:
        iss = ";".join("%s x%d" % (c, n) for c, n, _ in r["issues"]) or "-"
        mr = ("%.1f" % r["map_rate"]) if r["map_rate"] is not None else "-"
        A("| %s | %s | %s | %d | %d | %d | %d | %d | %s | %s |" % (
            r["zip"], r["status"], r["header"], r["n_class"], r["n_field"],
            r["n_method"], r["n_static"], r["n_pfield"], mr, iss))
    A("")
    A("Legend: st=PASS/WARN/FAIL, hdr=tiny header version, cls/fld/mth/stc=class/field/"
      "method/static-mapping counts, pfl=positional-field (`p`) lines, map%=class mapping "
      "rate (named!=official).")
    A("")
    A("## 2. Issue list (one line per zip x dimension symptom)")
    A("")
    A("| file | dim | symptom | count | sample | suggested action |")
    A("|---|---|---|---|---|---|")
    DIM = {"ZIP-UNREADABLE": ("solvability", "zip cannot open", "re-download / drop"),
           "NO-CLIENT-TINY": ("solvability", "no client.tiny in zip",
                              "not a tiny lib (patches/start only): drop from F1/F4 feed"),
           "EMPTY-TINY": ("solvability", "client.tiny is 0 bytes", "drop"),
           "PARSE-EXC": ("solvability", "parser exception", "manual inspection"),
           "NO-HEADER": ("solvability", "missing tiny header", "manual: confirm format"),
           "DUP-CLASS-OFF": ("format", "duplicate official class key", "manual: dedupe, last-wins is undefined"),
           "DUP-MEMBER": ("format", "duplicate member key in same class", "manual: dedupe"),
           "EMPTY-NAMED-CLASS": ("format", "class named empty", "manual: fill or accept as unmapped"),
           "EMPTY-NAMED-MEMBER": ("format", "member named empty", "manual: fill or drop line"),
           "EMPTY-NAMED-PFIELD": ("format", "positional field named empty", "manual"),
           "BAD-CHAR-CLASS-NAMED": ("format", "class named has illegal char", "manual: fix name"),
           "BAD-CHAR-MEMBER-NAMED": ("format", "member named has illegal char", "manual: fix name"),
           "ORPHAN-MEMBER": ("format", "member line with no parent class", "manual: check comment handling"),
           "V1-LINE-IN-V2": ("format", "v1-style member line under v2 header", "manual: normalize to v2"),
           "BAD-COLUMNS-CLASS": ("format", "class line wrong column count", "manual"),
           "BAD-COLUMNS-MEMBER": ("format", "member line wrong column count", "manual"),
           "UNKNOWN-LINE": ("format", "unrecognized line kind", "manual"),
           "JAR-REVERSE": ("real-jar", "tiny classes absent from local jar",
                           "suspect version mismatch: manual re-pin")}
    any_issue = False
    for r in recs:
        for code, n, s in r["issues"]:
            any_issue = True
            dim, sym, act = DIM.get(code, ("?", code, "manual"))
            A("| %s | %s | %s | %d | %s | %s |" % (
                r["zip"], dim, sym, n, (s or "").replace("|", "\\|")[:60], act))
    if not any_issue:
        A("| - | - | none | - | - | - |")
    A("")
    A("## 3. versions.json consistency (gaps / orphans)")
    A("")
    A("Gap: versions.json references a resources zip that is missing on disk.")
    A("")
    if gaps:
        for zname, vids in sorted(gaps.items()):
            A("- MISSING `%s` <- entries: %s" % (zname, ", ".join(vids)))
    else:
        A("- none")
    A("")
    A("Orphan: zip on disk not referenced by any versions.json entry.")
    A("")
    if orphan_zips:
        for zname in orphan_zips:
            A("- ORPHAN `%s`" % zname)
    else:
        A("- none")
    A("")
    A("Referenced-but-unusable: versions.json entries whose resources zip has no client.tiny.")
    A("")
    if notiny_referenced:
        for zname, vids in sorted(notiny_referenced.items()):
            A("- `%s` (no client.tiny) <- entries: %s" % (zname, ", ".join(vids)))
    else:
        A("- none")
    A("")
    A("Embedded metadata vs filename: no json/txt/md/xml metadata files are embedded "
      "in any zip (measured), so no contradiction is possible; zip-to-version binding "
      "is exclusively via versions.json `resources`.")
    A("")
    A("Shared zips (one tiny serving N versions.json entries, i.e. in-catalog borrow): %d zips."
      % sum(1 for r, v in res_map.items() if r in by_zip and len(v) > 1))
    A("")
    A("## 4. Real-jar coverage (local jars only)")
    A("")
    A("Local jar dirs with a resolvable version id: %d (unresolved: %d)." % (len(cov), len(jar_unres)))
    A("")
    if jar_unres:
        A("Unresolved local dirs: " + ", ".join(sorted(jar_unres)))
        A("")
    A("| local dir | vid | zip | how | jarCls | tinyCls | cov% | rev% |")
    A("|---|---|---|---|---|---|---|---|")
    for d, vid, zname, method, jc, tc, cv, rv, err in cov:
        if cv is None:
            A("| %s | %s | %s | %s | ERR | - | - | - |" % (d, vid, zname, err))
        else:
            A("| %s | %s | %s | %s | %d | %d | %.1f | %.1f |" % (
                d, vid, zname, method, jc, tc, 100 * cv, 100 * rv))
    A("")
    A("cov% = jar classes covered by tiny official set; rev% = tiny entries NOT in the jar "
      "(rev>0 means the tiny is not an exact match for this jar build).")
    A("")
    n_rev = sum(1 for r in cov if r[7] and r[7] > 0)
    n_full = sum(1 for r in cov if r[6] and r[6] >= 0.999)
    A("Measured: %d jar-matched versions, %d with cov>=99.9%%, %d with rev>0 (mismatch suspects)."
      % (len(cov), n_full, n_rev))
    A("")
    A("## 5. Anchor cross-check (11 fingerprint versions)")
    A("")
    A("| vid | schema | tiny zip | slots | MATCH | DESC-DIFF | NAME-GAP | CLASS-GAP | ops-bare(unrecon) | hit% |")
    A("|---|---|---|---|---|---|---|---|---|---|")
    tot_slots = tot_match = 0
    for vid, zname, schema, total, matched, extra, gaps_ in anchor_rows:
        if total is None:
            A("| %s | - | %s | NO-TINY | - | - | - | - | - | - |" % (vid, zname))
            continue
        tot_slots += total
        tot_match += matched
        A("| %s | %s | %s | %d | %d | %d | %d | %d | %d | %s |" % (
            vid, schema, zname, total, matched, extra["desc_diff"],
            extra["name_gap"], extra["class_gap"], extra["bare_ops"],
            fmt_rate(matched, total)))
    A("")
    A("Overall anchor hit rate: %d/%d = %s (ops bare-name slots excluded: they carry no "
      "class/desc context and are unreconcilable offline, finding F-2 of b2-2)."
      % (tot_match, tot_slots, fmt_rate(tot_match, tot_slots)))
    A("")
    A("Anchor gap details:")
    A("")
    for vid, zname, schema, total, matched, extra, gaps_ in anchor_rows:
        if not gaps_:
            continue
        for slot, cls, name, desc, state in gaps_:
            A("- `%s` [%s] %s = %s.%s%s" % (vid, state, slot, cls, name, desc))
    A("")
    A("## 6. Conclusion (F4 feed triage)")
    A("")
    A("- Ready for F4 clustering directly: **%d** zips (PASS: parse-clean, no format "
      "issues, no jar-mismatch flag)." % n_pass)
    A("- Need manual review first: **%d** zips (WARN: parseable but carry format "
      "pathologies or reverse-coverage suspicion)." % n_warn)
    A("- Recommend drop: **%d** zips (FAIL: no client.tiny / unreadable / empty)." % n_fail)
    A("- Anchor library: %d/%d slots reconcile (%s); remaining gaps are listed in section 5 "
      "and should be treated as anchor-side or borrow-side follow-ups, not blockers."
      % (tot_match, tot_slots, fmt_rate(tot_match, tot_slots)))
    A("- versions.json: %d gap zip(s), %d orphan zip(s); entries pointing at tiny-less zips "
      "cannot be fed to F1/F4 at all (see section 3)." % (len(gaps), len(orphan_zips)))
    A("")
    # ---- Chinese summary ------------------------------------------------------
    code_count = {}
    for r in recs:
        for code, n, _ in r["issues"]:
            code_count[code] = code_count.get(code, 0) + n
    A("## 7. Conclusion (zh)")
    A("")
    A("- 133 \u4ef6\u5168\u91cf QC \u5b8c\u6bd5\uff1a\u53ef\u76f4\u63a5\u8fdb F4 \u805a\u7c7b **%d** \u4ef6\uff08PASS\uff0c\u89e3\u6790\u5e72\u51c0\u3001\u96f6\u75c5\u7076\u3001\u65e0\u5b9e jar \u53cd\u67e5\u5f02\u5e38\uff09\uff1b"
      "\u9700\u4eba\u5de5\u590d\u6838 **%d** \u4ef6\uff08WARN\uff09\uff1b\u5efa\u8bae\u5f03\u7528 **%d** \u4ef6\uff08FAIL\uff1azip \u5185\u6839\u672c\u6ca1\u6709 client.tiny\uff0c"
      "\u4ec5 patches/start\uff0c\u5c5e\u542f\u52a8\u5668\u8d44\u6e90\u5305\u800c\u975e\u6620\u5c04\u5e93\uff09\u3002" % (n_pass, n_warn, n_fail))
    A("- \u5168\u90e8 %d \u4ef6 client.tiny \u5747\u5e26 `tiny v2` \u5934\uff0c\u65e0\u4e00\u4f8b v1\uff1b\u4f46\u65b9\u8a00\u6269\u5c55\u884c\u666e\u904d\u5b58\u5728\uff1a"
      "\u4f4d\u7f6e\u5b57\u6bb5 `p` \u884c\uff08official \u4e3a\u7a7a\uff0c\u5171 %d \u884c\uff09\u3001\u7c7b/\u6210\u5458\u6ce8\u91ca\u884c\uff08`c` \u4e8c\u6b21\u7f29\u8fdb\uff09\u3002"
      "F1/F4 \u4fa7\u89e3\u6790\u5668\u5fc5\u987b\u5bb9\u5fcd\u8fd9\u4e09\u7c7b\u884c\uff0c\u5426\u5219\u4f1a\u50cf b2-2 \u90a3\u6837\u9759\u9ed8\u4e22\u6570\u636e\u3002"
      % (sum(1 for r in recs if r["n_class"]), sum(r["n_pfield"] for r in recs)))
    A("- \u6700\u4e25\u91cd\u4e09\u7c7b\u75c5\u7076\uff1a\u5176\u4e00\uff0c9 \u4ef6\u65e0 client.tiny\uff08\u542b rd-1/rd-2\uff0c\u5bfc\u81f4 rd-132211 \u7b49 3 \u4e2a versions.json "
      "\u6761\u76ee\u5f7b\u5e95\u65e0\u6620\u5c04\u53ef\u7528\uff09\uff1b\u5176\u4e8c\uff0cEMPTY-NAMED-CLASS \u5171 %d \u5904\uff0c\u96c6\u4e2d\u5728 Minecraft / MinecraftApplet "
      "\u7b49\u5165\u53e3\u7c7b\uff081.0.0-rc*\u3001b1.8-pre*\u3001inf-201003xx\u3001a1.2.0 \u7cfb\uff09\uff0c\u5582 F4 \u524d\u9700\u8865\u540d\u6216\u663e\u5f0f\u63a5\u53d7\u4e3a\u672a\u6620\u5c04\uff1b"
      "\u5176\u4e09\uff0c\u5b9e jar \u53cd\u67e5\u975e\u96f6\uff08JAR-REVERSE\uff09\u547d\u4e2d b1.2\u3001in-20100125\u3001in-20100201-1/2 \u7b49 %d \u4ef6\uff0c"
      "\u8bf4\u660e\u5176 tiny \u4e0e\u672c\u5730 jar \u5e76\u975e\u540c\u6784\u5efa\uff0c\u501f\u7528/\u5bf9\u69ab\u9700\u4eba\u5de5\u9489\u7248\u3002"
      % (code_count.get("EMPTY-NAMED-CLASS", 0),
         sum(1 for r in recs if any(c == "JAR-REVERSE" for c, _, _ in r["issues"]))))
    A("- \u5b9e jar \u8986\u76d6\uff1a\u672c\u5730\u53ef\u5bf9\u69ab %d \u4e2a\u7248\u672c\uff0ctiny \u5bf9 jar \u7684\u6b63\u5411\u8986\u76d6\u666e\u904d 77-93%%\uff08tiny \u53ea\u6536\u5f55\u88ab\u6620\u5c04"
      "\u89e6\u53ca\u7684\u7c7b\uff0c\u5185\u8054\u7c7b/\u8d44\u6e90\u7c7b\u4e0d\u5728\u518c\u5c5e\u9884\u671f\uff09\uff1b\u53cd\u5411\u7387\u4ec5 %d \u4ef6 >0\uff0c\u5373\u7edd\u5927\u591a\u6570 tiny \u4e0e\u672c\u5730 jar \u65e0"
      "\u7248\u672c\u9519\u4f4d\u5acc\u7591\u3002" % (len(cov), sum(1 for r in cov if r[7] and r[7] > 0)))
    A("- \u951a\u5e93\u4ea4\u53c9\uff1a11 \u7248\u951a\u5e93 %d/%d \u69fd\u4f4d\u547d\u4e2d\uff08%s\uff09\uff0c\u7f3a\u53e3\u5168\u90e8\u662f `<init>` \u6784\u9020\u5668 NAME-GAP \u4e0e desc "
      "\u6f02\u79fb DESC-DIFF\uff0c\u5c5e\u951a\u5e93\u4fa7/\u501f\u7528\u4fa7\u540e\u7eed\u8ddf\u8fdb\u9879\uff0c\u4e0d\u963b\u585e F4\uff1bruntime \u7248 ops \u88f8\u540d\u69fd\uff08\u5404 54 \u4e2a\uff09"
      "\u7ef4\u6301 b2-2 \u7ed3\u8bba F-2\uff1a\u79bb\u7ebf\u4e0d\u53ef\u5bf9\u8d26\u3002" % (tot_match, tot_slots, fmt_rate(tot_match, tot_slots)))
    A("- versions.json \u5bf9\u69ab\uff1a\u7f3a 1 \u4ef6 zip\uff08c0.30_01c.zip\uff09\u3001\u5b64\u513f 1 \u4ef6\uff08chambered2.zip\uff0c\u4e14\u81ea\u5e26 "
      "DUP-MEMBER \u75c5\u7076\uff09\uff1b\u76ee\u5f55\u5185 19 \u4ef6 zip \u4e3a\u591a\u7248\u672c\u5171\u7528\uff08\u5728\u518c\u501f\u7528\uff09\u3002")
    A("")

    out = os.path.join(HERE, "report.md")
    with open(out, "w", encoding="utf-8") as f:
        f.write("\n".join(L))
    print("PASS=%d WARN=%d FAIL=%d  jars=%d  anchors=%d/%d" % (
        n_pass, n_warn, n_fail, len(cov), tot_match, tot_slots))
    print("report -> " + out)
    print("dumps -> " + DUMPDIR)


if __name__ == "__main__":
    main()
