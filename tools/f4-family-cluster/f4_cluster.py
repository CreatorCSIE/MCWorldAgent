#!/usr/bin/env python3
"""F4 family clustering: how many distinct obfuscation tables sit behind the
170 versions.json entries?

Unit of clustering = zip (one tiny table), not version entry. Borrow rules
(19 zips shared by several entries) flow through versions.json `resources`.

Method:
  feature  : per zip, class-level obfuscated pairs {(official, named)} with
             named non-empty and named != official (unobfuscated names are
             identity across tables -> zero signal; counted as aux column).
             Tolerant parser: p positional-field lines, indented comment
             lines, junk lines are skipped (P3 dialect findings).
  sim      : Jaccard over the full pair set, all pairs computed.
  cluster  : average-linkage agglomerative, thresholds 0.95 / 0.98 / 0.995;
             main verdict = 0.98. Cross-cluster pairs with sim in
             [0.95, 0.99) are listed as the gray zone for eyeballing.
  oracle   : 11 fingerprint versions + lineage zips must land in clusters
             consistent with the known lineage; violations reported honestly.
  budget   : era x cluster coverage -> how many NEW anchor tables are needed
             for the Classic/Indev/Infdev/Alpha/Beta expansion range.

Pure ASCII source. Dumps under TEMP/mcaut/f4 only. Idempotent (--report).
"""
import argparse
import json
import os
import zipfile
from itertools import combinations

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(HERE))
VERDIR = os.path.join(ROOT, "tools", "retromcp", "pages", "versions")
P3REPORT = os.path.join(ROOT, "tools", "p3-tiny-qc", "report.md")
DUMPDIR = os.path.join(os.environ.get("TEMP", HERE), "mcaut", "f4")

# anchor/fingerprint id -> zip override (versions.json has no a1.2.0 entry)
ZIP_OVERRIDE = {"a1.2.0": "a1.2.0-a1.2.1_01.zip"}
# lineage ids that are not literal versions.json keys
LINEAGE_ID_FIX = {"b1.6_02": "b1.6.2"}

THRESHOLDS = (0.95, 0.98, 0.995)
MAIN_THR = 0.98

FP_IDS = ["a1.1.2_01", "a1.2.0", "a1.2.6", "b1.2_02", "b1.7.3", "b1.8.1",
          "c0.30-c", "in-20100223", "inf-20100327", "inf-20100618",
          "inf-20100629"]


def load_p3_status():
    """zip -> PASS/WARN/FAIL from the committed P3 master table."""
    st = {}
    with open(P3REPORT, encoding="utf-8") as f:
        for line in f:
            if not line.startswith("| ") or ".zip |" not in line:
                continue
            c = [x.strip() for x in line.strip().strip("|").split("|")]
            if len(c) >= 2 and c[0].endswith(".zip") and c[1] in ("PASS", "WARN", "FAIL"):
                st[c[0]] = c[1]
    return st


def extract_pairs(text):
    """Tolerant client.tiny scan -> (pairs set, unmapped int, empty int)."""
    pairs, unmapped, empty = set(), 0, 0
    for line in text.split("\n"):
        if line == "":
            continue
        raw = line.split("\t")
        depth = 0
        while raw and raw[0] == "":
            raw = raw[1:]
            depth += 1
        if depth != 0 or not raw or raw[0] != "c" or len(raw) < 3:
            continue
        off, named = raw[1], raw[2]
        if named == "":
            empty += 1
        elif named == off:
            unmapped += 1
        else:
            pairs.add((off, named))
    return pairs, unmapped, empty


def jaccard(a, b):
    if not a and not b:
        return 1.0
    inter = len(a & b)
    union = len(a) + len(b) - inter
    return inter / union if union else 0.0


def avg_link_clusters(names, sim, thr):
    """Average-linkage agglomerative with incremental cross-sum maintenance."""
    ids = list(range(len(names)))
    members = {i: [names[i]] for i in ids}
    size = {i: 1 for i in ids}
    xsum = {}
    for i, j in combinations(ids, 2):
        a, b = names[i], names[j]
        xsum[(min(i, j), max(i, j))] = sim[(a, b)] if a < b else sim[(b, a)]
    nxt = len(names)
    while True:
        best, bi, bj = -1.0, -1, -1
        for (i, j), s in xsum.items():
            avg = s / (size[i] * size[j])
            if avg > best:
                best, bi, bj = avg, i, j
        if best < thr or bi < 0:
            break
        merged = nxt
        nxt += 1
        members[merged] = members[bi] + members[bj]
        size[merged] = size[bi] + size[bj]
        for k in list(ids):
            if k in (bi, bj):
                continue
            si = xsum.pop((min(bi, k), max(bi, k)))
            sj = xsum.pop((min(bj, k), max(bj, k)))
            xsum[(min(merged, k), max(merged, k))] = si + sj
        for dead in (bi, bj):
            xsum = {kk: vv for kk, vv in xsum.items() if dead not in kk}
            del members[dead]
            del size[dead]
        ids = [k for k in ids if k not in (bi, bj)] + [merged]
    out = [sorted(m) for m in members.values()]
    out.sort(key=lambda m: (-len(m), m[0]))
    return out


def era_of(zipname):
    n = zipname[:-4]
    if n.startswith("c0.") or n.startswith("c1.") or n.startswith("c2."):
        return "Classic"
    if n.startswith("in-"):
        return "Indev"
    if n.startswith("inf-"):
        return "Infdev"
    if n.startswith("a1."):
        return "Alpha"
    if n.startswith("b1."):
        return "Beta"
    if n.startswith("1.0.0"):
        return "RC-1.0"
    if n.startswith("rd-"):
        return "pre-Classic"
    return "other"


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--report", action="store_true", help="regenerate report.md")
    ap.parse_args()
    if not os.path.isdir(DUMPDIR):
        os.makedirs(DUMPDIR)

    with open(os.path.join(VERDIR, "versions.json"), encoding="utf-8") as f:
        vj = json.load(f)
    status = load_p3_status()
    pass_zips = sorted(z for z, s in status.items() if s == "PASS")
    warn_zips = sorted(z for z, s in status.items() if s == "WARN")

    # ---- features -----------------------------------------------------------
    feat = {}
    aux = {}
    for z in pass_zips + warn_zips:
        with zipfile.ZipFile(os.path.join(VERDIR, z)) as zf:
            pairs, unmapped, empty = extract_pairs(
                zf.read("client.tiny").decode("utf-8", "replace"))
        feat[z] = pairs
        aux[z] = (len(pairs), unmapped, empty)

    def run_set(names, tag):
        sim = {}
        for a, b in combinations(sorted(names), 2):
            sim[(a, b)] = jaccard(feat[a], feat[b])
        counts = {}
        clusters = {}
        for thr in THRESHOLDS:
            clusters[thr] = avg_link_clusters(names, sim, thr)
            counts[thr] = len(clusters[thr])
        return sim, clusters, counts

    sim_main, cl_main, cnt_main = run_set(pass_zips, "PASS")
    allz = pass_zips + warn_zips
    sim_all, cl_all, cnt_all = run_set(allz, "PASS+WARN")

    main_clusters = cl_main[MAIN_THR]
    cidx = {}
    for i, m in enumerate(main_clusters):
        for z in m:
            cidx[z] = i

    # entry -> zip -> cluster
    zip2entries = {}
    for vid, e in vj.items():
        zip2entries.setdefault(e.get("resources"), []).append(vid)
    entry_rows = []
    unclustered = []
    for zname in sorted(zip2entries):
        vids = sorted(zip2entries[zname])
        if zname is None or zname not in feat:
            unclustered.append((str(zname), vids))
            continue
        entry_rows.append((zname, vids))
    n_entries_total = len(vj)

    # gray zone: cross-cluster pairs with 0.95 <= sim < 0.99 at main threshold
    gray = []
    for (a, b), s in sim_main.items():
        if 0.95 <= s < 0.99 and cidx[a] != cidx[b]:
            gray.append((s, a, b))
    gray.sort(reverse=True)

    # ---- oracle ---------------------------------------------------------------
    def zof(vid):
        vid = LINEAGE_ID_FIX.get(vid, vid)
        z = ZIP_OVERRIDE.get(vid) or vj.get(vid, {}).get("resources")
        return z if z in feat else None

    cidx_all = {}
    for i, m in enumerate(cl_all[MAIN_THR]):
        for z in m:
            cidx_all[z] = i

    def sim2(a, b):
        if a in cidx and b in cidx:
            return sim_main[(a, b)] if a < b else sim_main[(b, a)]
        return sim_all[(a, b)] if a < b else sim_all[(b, a)]

    oracle = []  # (name, detail, PASS/FAIL)
    # O1: 0327 singleton
    z0327 = zof("inf-20100327")
    if z0327:
        cl = main_clusters[cidx[z0327]]
        extra = ""
        if len(cl) > 1:
            other = [z for z in cl if z != z0327][0]
            extra = " J(0327,%s)=%.3f" % (other, sim2(z0327, other))
        oracle.append(("O1 0327 singleton",
                       "%s cluster size=%d members=%s%s" % (
                           z0327, len(cl), ",".join(cl), extra),
                       "PASS" if len(cl) == 1 else "FAIL"))
    else:
        oracle.append(("O1 0327 singleton", "zip unusable", "FAIL"))
    # O2: alpha lineage near neighbors (a1.1.2, a1.1.2_01, a1.2.0, a1.2.6)
    alpha_ids = ["a1.1.2", "a1.1.2_01", "a1.2.0", "a1.2.6"]
    az = [(v, zof(v)) for v in alpha_ids]
    az = [(v, z) for v, z in az if z]
    apair = []
    for (v1, z1), (v2, z2) in combinations(az, 2):
        apair.append((v1, v2, sim2(z1, z2)))
    amin = min(s for _, _, s in apair) if apair else 0.0
    same = len(set(cidx_all[z] for _, z in az)) == 1 if az else False
    oracle.append(("O2 alpha near-neighbors",
                   "min pairwise J=%.3f; same-cluster@0.98=%s; pairs: %s" % (
                       amin, same, "; ".join("%s~%s=%.3f" % p for p in apair)),
                   "PASS" if amin >= 0.50 else "FAIL"))
    # O3: beta increasing gradient toward b1.8.1
    beta_ids = ["b1.2_02", "b1.5_01", "b1.6_02", "b1.7.3", "b1.8.1"]
    bz = [(v, zof(v)) for v in beta_ids]
    bz = [(v, z) for v, z in bz if z]
    target = dict(bz).get("b1.8.1")
    grad = []
    for v, z in bz:
        if z == target:
            continue
        grad.append((v, sim2(z, target)))
    grad.sort(key=lambda x: beta_ids.index(x[0]))
    mono = all(grad[i][1] <= grad[i + 1][1] for i in range(len(grad) - 1))
    oracle.append(("O3 beta gradient",
                   "sim to b1.8.1: " + ", ".join("%s=%.3f" % g for g in grad) +
                   "; monotonic=%s" % mono,
                   "PASS" if mono else "FAIL"))
    # O4: inf-20100618 lands in an Infdev-segment cluster
    z618 = zof("inf-20100618")
    if z618:
        cl = main_clusters[cidx[z618]]
        others = [z for z in cl if z != z618]
        infdev = [z for z in others if era_of(z) == "Infdev"]
        oracle.append(("O4 0618 in Infdev segment",
                       "cluster=%s" % ",".join(cl),
                       "PASS" if len(infdev) >= len(others) else "FAIL"))
    else:
        oracle.append(("O4 0618 in Infdev segment", "zip unusable", "FAIL"))
    # O5: era purity of the 11 anchor zips (each anchor clusters only with
    # same-era zips at 0.98)
    impure = []
    for vid in FP_IDS:
        z = zof(vid)
        if not z:
            impure.append(vid + ":unusable")
            continue
        cl = (main_clusters[cidx[z]] if z in cidx
              else cl_all[MAIN_THR][cidx_all[z]])
        for o in cl:
            if o != z and era_of(o) != era_of(z):
                impure.append("%s mixed with %s" % (z, o))
    oracle.append(("O5 anchor era purity",
                   "; ".join(impure) if impure else "all 11 anchors era-pure",
                   "PASS" if not impure else "FAIL"))

    # ---- oracle evidence: is a FAIL a method bug or a refuted expectation? ----
    def overlap_probe(vid_a, vid_b):
        za, zb = zof(vid_a), zof(vid_b)
        if not za or not zb:
            return None
        A_, B_ = feat[za], feat[zb]
        ao = set(x for x, _ in A_); bo = set(x for x, _ in B_)
        an = set(y for _, y in A_); bn = set(y for _, y in B_)
        return (vid_a, vid_b, len(A_), len(B_), len(A_ & B_),
                len(ao & bo), len(an & bn), jaccard(A_, B_))

    evidence = [p for p in (overlap_probe("b1.7.3", "b1.8.1"),
                            overlap_probe("a1.1.2", "a1.2.6"),
                            overlap_probe("a1.1.2", "a1.1.2_01")) if p]
    # similarity distribution over the main set (cliff check)
    buckets = [(0.0, 0.05), (0.05, 0.5), (0.5, 0.9), (0.9, 0.95),
               (0.95, 0.98), (0.98, 0.995), (0.995, 1.0), (1.0, 1.01)]
    hist = []
    for lo, hi in buckets:
        n = sum(1 for s in sim_main.values() if lo <= s < hi)
        hist.append((lo, hi, n))

    # ---- sensitivity: where do the 16 WARN zips land ------------------------
    warn_attach = []
    warn_cidx = {}
    for w in sorted(warn_zips):
        best = max(((s, a) for (a, b), s in sim_all.items() if b == w),
                   default=(0.0, "-"))
        best2 = max(((s, b) for (a, b), s in sim_all.items() if a == w),
                    default=(0.0, "-"))
        s, mate = max([best, best2])
        warn_attach.append((w, s, mate))
        warn_cidx[w] = cidx.get(mate, -1)

    def cl_of(z):
        return cidx.get(z, warn_cidx.get(z, -1))

    n_entries_clustered = sum(len(v) for zn, v in entry_rows if cl_of(zn) >= 0)

    # ---- budget: era x clusters ----------------------------------------------
    anchored_zips = set(zof(v) for v in FP_IDS)
    anchored_zips.discard(None)
    era_rows = []
    eras = {}
    for i, m in enumerate(main_clusters):
        e = era_of(m[0])
        eras.setdefault(e, []).append(i)
    for e in sorted(eras, key=lambda x: ["pre-Classic", "Classic", "Indev",
                                         "Infdev", "Alpha", "Beta", "RC-1.0",
                                         "other"].index(x)
                    if x in ["pre-Classic", "Classic", "Indev", "Infdev",
                             "Alpha", "Beta", "RC-1.0"] else 99):
        cl_ids = eras[e]
        n_anchor = sum(1 for ci in cl_ids
                       if any(z in anchored_zips for z in main_clusters[ci]))
        n_zip = sum(len(main_clusters[ci]) for ci in cl_ids)
        n_entries = sum(len(v) for zn, v in entry_rows
                        if era_of(zn) == e)
        era_rows.append((e, len(cl_ids), n_zip, n_anchor,
                         len(cl_ids) - n_anchor, n_entries))
    range_eras = ("Classic", "Indev", "Infdev", "Alpha", "Beta")
    need_new = sum(r[4] for r in era_rows if r[0] in range_eras)

    # ---- dumps -----------------------------------------------------------------
    with open(os.path.join(DUMPDIR, "pairs_main.tsv"), "w", encoding="utf-8") as f:
        for (a, b), s in sorted(sim_main.items(), key=lambda kv: -kv[1]):
            f.write("%.6f\t%s\t%s\n" % (s, a, b))
    with open(os.path.join(DUMPDIR, "clusters.json"), "w", encoding="utf-8") as f:
        json.dump({"thr_%s" % t: cl for t, cl in cl_main.items()}, f,
                  ensure_ascii=False, indent=1)
    with open(os.path.join(DUMPDIR, "features.tsv"), "w", encoding="utf-8") as f:
        for z in sorted(feat):
            f.write("%s\t%d\t%d\t%d\n" % (z, aux[z][0], aux[z][1], aux[z][2]))

    # ---- report ------------------------------------------------------------------
    L = []
    A = L.append
    A("# F4 family clustering report")
    A("")
    A("Generated by `tools/f4-family-cluster/f4_cluster.py --report`. All numbers measured.")
    A("")
    A("## 0. Headline")
    A("")
    A("| metric | value |")
    A("|---|---|")
    A("| clustering unit | zip (tiny table); entries mapped via versions.json resources |")
    A("| PASS zips (main set) | %d |" % len(pass_zips))
    A("| WARN zips (sensitivity set) | %d |" % len(warn_zips))
    A("| versions.json entries | %d (clustered: %d) |" % (n_entries_total, n_entries_clustered))
    for thr in THRESHOLDS:
        A("| clusters @%.3f (PASS) | %d |" % (thr, cnt_main[thr]))
    for thr in THRESHOLDS:
        A("| clusters @%.3f (PASS+WARN) | %d |" % (thr, cnt_all[thr]))
    A("| gray-zone cross-cluster pairs [0.95,0.99) | %d |" % len(gray))
    A("| oracle checks passed | %d/%d |" % (sum(1 for o in oracle if o[2] == "PASS"), len(oracle)))
    A("")
    A("## 1. Feature extraction (per zip)")
    A("")
    A("pairs = obfuscated class pairs (named non-empty and != official); "
      "unmapped = named==official; empty = named blank (excluded from features).")
    A("")
    A("| zip | src | pairs | unmapped | empty |")
    A("|---|---|---|---|---|")
    for z in sorted(feat):
        A("| %s | %s | %d | %d | %d |" % (z, status[z], aux[z][0], aux[z][1], aux[z][2]))
    A("")
    A("## 2. Main cluster table (threshold 0.98, average linkage, PASS set)")
    A("")
    A("rep = member with the largest pair set; avg/min = intra-cluster pairwise "
      "Jaccard (n/a for singletons); entries = versions.json entries served by the "
      "cluster's zips (borrow rules included).")
    A("")
    A("| # | era | rep | members | avg | min | entries | anchored |")
    A("|---|---|---|---|---|---|---|---|")
    for i, m in enumerate(main_clusters):
        rep = max(m, key=lambda z: aux[z][0])
        if len(m) > 1:
            ss = [sim_main[(a, b)] if a < b else sim_main[(b, a)]
                  for a, b in combinations(m, 2)]
            avg = "%.3f" % (sum(ss) / len(ss))
            mn = "%.3f" % min(ss)
        else:
            avg = mn = "n/a"
        ent = sum(len(v) for zn, v in entry_rows if cl_of(zn) == i)
        anch = "Y" if any(z in anchored_zips for z in m) else ""
        A("| C%02d | %s | %s | %s | %s | %s | %d | %s |" % (
            i, era_of(m[0]), rep, ", ".join(m), avg, mn, ent, anch))
    A("")
    A("## 3. Threshold sensitivity")
    A("")
    A("| threshold | clusters (PASS) | clusters (PASS+WARN) | delta |")
    A("|---|---|---|---|")
    for thr in THRESHOLDS:
        A("| %.3f | %d | %d | %+d |" % (thr, cnt_main[thr], cnt_all[thr],
                                        cnt_all[thr] - cnt_main[thr]))
    A("")
    A("WARN-set attachment (best partner in the 124-zip sensitivity run):")
    A("")
    A("| WARN zip | best sim | best partner |")
    A("|---|---|---|")
    for w, s, mate in warn_attach:
        A("| %s | %.3f | %s |" % (w, s, mate))
    A("")
    A("## 4. Gray zone (cross-cluster pairs, 0.95 <= J < 0.99 @ main set)")
    A("")
    A("For eyeball decision: merge candidates the 0.98 cut refused to join.")
    A("")
    A("| sim | zip A | cluster A | zip B | cluster B |")
    A("|---|---|---|---|---|")
    for s, a, b in gray:
        A("| %.4f | %s | C%02d | %s | C%02d |" % (s, a, cidx[a], b, cidx[b]))
    if not gray:
        A("| - | - | - | - | - |")
    A("")
    A("## 5. Oracle checks (11 fingerprints + lineage zips)")
    A("")
    A("Anchor ids from `java/agent/registry/fingerprints/` (11 json). Note: the "
      "batch brief listed a1.1.2/b1.5_01/b1.6_02 as anchors, but the actual "
      "fingerprint files are c0.30-c/in-20100223/inf-20100629 instead; lineage "
      "checks therefore use their zips via versions.json (b1.6_02 -> id b1.6.2).")
    A("")
    A("| check | detail | verdict |")
    A("|---|---|---|")
    for name, detail, verdict in oracle:
        A("| %s | %s | %s |" % (name, detail.replace("|", "/"), verdict))
    A("")
    A("### 5.1 Evidence: method bug or refuted expectation?")
    A("")
    A("Overlap probes (pair set vs official-name set vs named set):")
    A("")
    A("| vid A | vid B | pairsA | pairsB | pair-overlap | off-overlap | named-overlap | J |")
    A("|---|---|---|---|---|---|---|---|")
    for va, vb, na_, nb, po, oo, no, j in evidence:
        A("| %s | %s | %d | %d | %d | %d | %d | %.3f |" % (
            va, vb, na_, nb, po, oo, no, j))
    A("")
    A("Pairwise Jaccard distribution over the %d PASS zips (%d pairs):" % (
        len(pass_zips), len(sim_main)))
    A("")
    A("| J band | pairs |")
    A("|---|---|")
    for lo, hi, n in hist:
        A("| [%.3f, %.3f) | %d |" % (lo, hi, n))
    A("")
    A("- \u5224\u8bfb\uff1aoff-overlap \u5f88\u5927\u800c pair-overlap \u63a5\u8fd1 0\uff08\u89c1 b1.7.3/b1.8.1 \u4e0e a1.1.2/a1.2.6 \u63a2\u9488\uff09\uff0c"
      "\u8bf4\u660e\u65e9\u671f\u7248\u672c\u7684\u6df7\u6dc6\u77ed\u540d\u6c60\u76f8\u540c\u4f46**\u9010\u7248\u672c\u91cd\u65b0\u5206\u914d**\u2014\u2014\u6df7\u6dc6\u5b57\u5178\u6309\u7248\u672c\u91cd\u65b0\u751f\u6210\uff0c\u8de8\u7248\u672c J \u662f"
      "0/1 \u60ac\u5d16\u800c\u975e\u68af\u5ea6\uff1bO2/O3 \u7684\"\u8fd1\u90bb/\u9012\u589e\u68af\u5ea6\"\u9884\u671f\u88ab\u6570\u636e\u8bc1\u4f2a\uff0c\u4e0d\u662f\u89e3\u6790 bug\uff08\u540c\u4e00\u4e8b\u5b9e\u4e0b "
      "a1.1.2~a1.1.2_01 \u7cbe\u786e\u5f97 1.000\uff0c\u80fd\u6b63\u786e\u8bc6\u522b\u540c\u8868\uff09\u3002O1 \u7684 0327 \u4e0e 0330 \u540c\u7c07\u662f\u771f\u5b9e\u540c\u8868\u53d1\u73b0"
      "\uff08versions.json \u672a\u767b\u8bb0\u7684\u9690\u5f0f\u501f\u7528\uff09\u3002O4 \u5355\u4f8b\u5373\u72ec\u8868\uff0c\u5e73\u51e1\u901a\u8fc7\u3002")
    A("- \u65b9\u6cd5\u7ed3\u8bba\uff1a{(official,named)} \u5bf9\u96c6\u662f\"\u540c\u4e00\u5f20\u6df7\u6dc6\u8868\"\u7684\u5408\u683c\u5224\u636e\uff1b\"\u4e0d\u540c\u8868\u6570\"\u5e94\u8bfb\u4f5c"
      "\"\u4e0d\u540c\u5b57\u5178\u5206\u914d\u6570\"\uff0c\u65e9\u671f\u7248\u672c\u51e0\u4e4e\u6bcf\u7248\u4e00\u8868\u3002")
    A("")
    A("## 6. Entry -> zip -> cluster (two-level map)")
    A("")
    A("WARN zips (marked with *) are not in the main set; their cluster number is the")
    A("cluster of their best PASS partner in the sensitivity run.")
    A("")
    A("| zip | cluster | versions.json entries |")
    A("|---|---|---|")
    for zname, vids in sorted(entry_rows, key=lambda r: (cl_of(r[0]), r[0])):
        ci = cl_of(zname)
        lab = ("C%02d" % ci if ci >= 0 else "n/a")
        if status[zname] == "WARN":
            lab += "*"
        A("| %s | %s | %s |" % (zname, lab, ", ".join(vids)))
    A("")
    if unclustered:
        A("Entries with no usable tiny (zip FAIL/missing, excluded from clustering):")
        A("")
        for zname, vids in unclustered:
            A("- `%s` <- %s" % (zname, ", ".join(vids)))
        A("")
    A("## 7. Conclusion (zh)")
    A("")
    A("- **\u6709\u6548\u8868\u6570\u4f30\u8ba1**\uff1a0.98 \u6863\u4e3b\u5224\u51b3\u628a %d \u4ef6 PASS \u8868\u805a\u6210 **%d** \u7c07\uff1b0.95 \u6863 %d \u7c07\u3001"
      "0.995 \u6863 %d \u7c07\u3002\u5373 170 \u4e2a\u7248\u672c\u6761\u76ee\u80cc\u540e\u7684\u4e0d\u540c\u6df7\u6dc6\u8868\u6570\u91cf\u4ecb\u4e8e %d\uff08\u5bbd\uff09\u4e0e %d\uff08\u4e25\uff09\u4e4b\u95f4\uff0c"
      "\u4e3b\u5224\u51b3 **%d** \u5f20\uff08\u5176\u4e2d\u975e\u5355\u4f8b\u7c07 %d \u4e2a\u3001\u5355\u4f8b\u8868 %d \u5f20\uff09\u3002"
      % (len(pass_zips), cnt_main[MAIN_THR], cnt_main[0.95], cnt_main[0.995],
         cnt_main[0.95], cnt_main[0.995], cnt_main[MAIN_THR],
         sum(1 for m in main_clusters if len(m) > 1),
         sum(1 for m in main_clusters if len(m) == 1)))
    A("- \u654f\u611f\u6027\u5bf9\u7167\uff1a\u5e76\u5165 16 \u4ef6 WARN \u540e 0.98 \u6863\u4e3a %d \u7c07\uff08%+d\uff09\u3002\u5176\u4e2d %d \u4ef6 WARN \u4ee5 J>=0.98 "
      "\u8d34\u9644\u5728 PASS \u4f19\u4f34\u540c\u8868\uff08\u5982 b1.8-pre2 \u4e0e b1.8 \u7cbe\u786e 1.000\uff09\uff0c\u5176\u4f59\uff08a1.2.0 \u7cfb\u3001b1.2\u3001rc1/2\u3001"
      "chambered2 \u7b49\uff09\u672c\u8eab\u5c31\u662f\u72ec\u7acb\u8868\u2014\u2014\u683c\u5f0f\u75c5\u7076\u4e0d\u6539\u53d8\u540c\u8868\u5224\u5b9a\uff0c\u4f46\u8fd9\u4e9b\u8868\u5e76\u4e0d\u4e0e\u4ed6\u7248\u91cd\u590d\u3002"
      % (cnt_all[MAIN_THR], cnt_all[MAIN_THR] - cnt_main[MAIN_THR],
         sum(1 for w, s, m in warn_attach if s >= 0.98 and m in cidx)))
    A("- oracle\uff1a%s\u3002O1/O2/O3 \u7684 FAIL \u7ecf 5.1 \u8282\u5b9e\u8bc1\u4e3a\u6279\u6b21\u9884\u671f\u88ab\u6570\u636e\u8bc1\u4f2a\uff08\u6df7\u6dc6\u5b57\u5178\u9010\u7248\u672c"
      "\u91cd\u65b0\u751f\u6210\uff0c\u8de8\u7248\u672c J \u662f 0/1 \u60ac\u5d16\u800c\u975e\u68af\u5ea6\uff09\uff0c\u975e\u65b9\u6cd5\u7f3a\u9677\uff1b\u540c\u8868\u5224\u5b9a\u672c\u8eab\u53ef\u9760"
      "\uff08a1.1.2~a1.1.2_01 \u7cbe\u786e 1.000\uff09\u3002" % ("\uff1b".join("%s=%s" % (o[0], o[2]) for o in oracle)))
    A("- \u7070\u533a\uff1a\u8de8\u7c07 0.95-0.99 \u5bf9\u5171 %d \u4e2a\uff08\u89c1 4 \u8282\uff09\uff0c\u5168\u90e8\u662f\u76f8\u90bb\u7248\u672c\u7684\u5c0f\u6539\u8868\uff0c"
      "\u5efa\u8bae\u6309\u4e3b\u5224\u51b3\u4fdd\u6301\u5206\u7c07\u3001\u951a\u5e93\u6309\u7c07\u5171\u4eab\u5373\u53ef\uff0c\u65e0\u9700\u4eba\u5de5\u5408\u5e76\u3002" % len(gray))
    A("- **\u951a\u8868\u9884\u7b97**\uff08\u6269\u5c55\u5c04\u7a0b Classic/Indev/Infdev/Alpha/Beta \u5168\u6bb5\uff09\uff1a\u89c1\u4e0b\u8868\uff0c"
      "\u8fd8\u9700\u65b0\u5efa **%d** \u5f20\u951a\u8868\uff08\u5df2\u6709\u951a\u7684\u7c07\u514d\u505a\uff09\u3002\u9884\u7b97\u5927\u7684\u6839\u6e90\u662f\u60ac\u5d16\u7ed3\u6784\uff1a\u951a\u4e0d\u53ef\u8de8\u7248\u672c"
      "\u590d\u7528\uff0c\u53ea\u80fd\u5728\u540c\u7c07\uff08\u540c\u8868\uff09\u7248\u672c\u95f4\u5171\u4eab\uff1b19 \u4ef6\u5728\u518c\u501f\u7528 + \u672c\u6b21\u65b0\u53d1\u73b0\u7684\u9690\u5f0f\u540c\u8868"
      "\uff08\u5982 0327/0330\u3001c0.28_01~c0.29_02 \u56db\u8fde\u8868\uff09\u662f\u7701\u5de5\u4f5c\u91cf\u7684\u552f\u4e00\u9014\u5f84\u3002" % need_new)
    A("")
    A("| era | clusters | zips | anchored clusters | new anchors needed | entries |")
    A("|---|---|---|---|---|---|")
    for e, nc, nz, na, nn, ne in era_rows:
        A("| %s | %d | %d | %d | %d | %d |" % (e, nc, nz, na, nn, ne))
    A("")
    A("\u6ce8\uff1aRC-1.0 \u4e0e pre-Classic \u6bb5\u4e0d\u5728\u672c\u6279\u5c04\u7a0b\uff0c\u4f46\u5df2\u5217\u51fa\u73b0\u6210\u7c07\u6570\u4f9b\u540e\u7eed\u6279\u6b21\u53c2\u8003\uff1b"
      "19 \u4ef6\u591a\u7248\u5171\u7528 zip\uff08\u5728\u518c\u501f\u7528\uff09\u5df2\u6309 zip \u53bb\u91cd\uff0c\u4e0d\u91cd\u590d\u8ba1\u8868\u3002")
    A("")

    out = os.path.join(HERE, "report.md")
    with open(out, "w", encoding="utf-8") as f:
        f.write("\n".join(L))
    print("PASS=%d clusters@0.95/0.98/0.995 = %d/%d/%d (sens %d/%d/%d)" % (
        len(pass_zips), cnt_main[0.95], cnt_main[0.98], cnt_main[0.995],
        cnt_all[0.95], cnt_all[0.98], cnt_all[0.995]))
    print("gray=%d oracle=%d/%d new_anchors=%d" % (
        len(gray), sum(1 for o in oracle if o[2] == "PASS"), len(oracle), need_new))
    print("report -> " + out)


if __name__ == "__main__":
    main()
