#!/usr/bin/env python3
"""F1 slow-path anchor compiler (P1 minimal core: getBlockId / setBlock / saveWorld).

Forensic pipeline, javap-based (project culture: bytecode evidence, see
java/agent/registry/fingerprints/README.md and docs/compatibility/verscan/):

  1. detect generation family from jar entry layout (structural, not version-id)
  2. locate the World class with per-generation string-constant probes
     (NBT / storage keys are the most stable semantic carriers)
  3. compile each facet with generation-dispatched structural rules
     (bytecode call-graph signatures, never hardcoded answers)
  4. emit candidate quintuples (class + name + descriptor) + evidence chain
     + confidence (HIGH / MEDIUM / LOW)

Modes:
  f1_compile.py --jar PATH --version-id ID          compile one jar, JSON to stdout
  f1_compile.py --recon [--fingerprints-dir DIR] [--report PATH]
                  compile every registry version, reconcile against the
                  anchor library (F5), write report.md

Intermediate class dumps go to %TEMP%/mcaut/f1/. This tool never writes to
the java/ registry or docs/.
"""

import argparse
import glob
import hashlib
import json
import os
import re
import subprocess
import sys
import zipfile

# ---------------------------------------------------------------------------
# probe groups per generation family, extracted from docs/compatibility/verscan/
# detailed tables (which storage-key / NBT strings each era actually uses)
# ---------------------------------------------------------------------------
PROBES = {
    "G1": ["The level is corrupt!"],
    "G2": ["level.dat", "SpawnX", "RandomSeed", "MinecraftLevel",
           "The level is corrupt!", "TickNextTick list out of synch"],
    "G3": ["level.dat", "SpawnX", "RandomSeed",
           "TickNextTick list out of synch", "Saving chunks", "Saving level"],
}
# the tick body always carries this string (verscan 0629 / a1.1.2 rework):
# it is NEVER a saveWorld facet
TICK_STRING = "TickNextTick list out of synch"
# direct level-write evidence used by saveWorld rules
SAVE_EVIDENCE = ["level.dat", "Saving level", "Saving chunks"]

WORKDIR = os.path.join(os.path.expandvars(r"%TEMP%"), "mcaut", "f1")


# ---------------------------------------------------------------------------
# low-level helpers
# ---------------------------------------------------------------------------
def sha1_of(path):
    h = hashlib.sha1()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def javap_text(classfile, code=True):
    args = ["javap", "-p", "-s"] + (["-c"] if code else []) + [classfile]
    r = subprocess.run(args, capture_output=True, text=True)
    return r.stdout


class ClassDb:
    """Jar reader + javap cache."""

    def __init__(self, jarpath):
        self.jarpath = jarpath
        self.zf = zipfile.ZipFile(jarpath)
        self.names = self.zf.namelist()
        self._sig = {}   # class -> parsed method list (signatures only)
        self._code = {}  # class -> parsed method list (with bodies)

    def close(self):
        self.zf.close()

    def read(self, cls):
        return self.zf.read(cls + ".class")

    def _extract(self, cls):
        os.makedirs(WORKDIR, exist_ok=True)
        out = os.path.join(WORKDIR, cls.replace("/", "_") + ".class")
        with open(out, "wb") as f:
            f.write(self.read(cls))
        return out

    def methods(self, cls, code=False):
        cache = self._code if code else self._sig
        if cls not in cache:
            text = javap_text(self._extract(cls), code=code)
            cache[cls] = parse_methods(text)
        return cache[cls]


HEADER_RE = re.compile(r"^  (.*?);\s*$")
NAME_RE = re.compile(r"([\w$<>]+)\(")


def parse_methods(text):
    """Parse javap -p -s [-c] output into method records."""
    lines = text.splitlines()
    methods = []
    i = 0
    n = len(lines)
    while i < n:
        m = HEADER_RE.match(lines[i])
        if m and "(" in lines[i] and i + 1 < n and \
                lines[i + 1].strip().startswith("descriptor:"):
            header = m.group(1).strip()
            desc = lines[i + 1].strip().split(":", 1)[1].strip()
            nm = NAME_RE.search(header)
            name = nm.group(1) if nm else "?"
            words = set(re.findall(r"\b(?:public|private|protected|static|final|abstract|synchronized|native)\b", header))
            j = i + 2
            body = []
            while j < n:
                mm = HEADER_RE.match(lines[j])
                if mm and "(" in lines[j] and j + 1 < n and \
                        lines[j + 1].strip().startswith("descriptor:"):
                    break
                body.append(lines[j])
                j += 1
            methods.append({
                "header": header, "name": name, "desc": desc,
                "flags": words, "body": "\n".join(body),
            })
            i = j
        else:
            i += 1
    return methods


CALL_Q_RE = re.compile(
    r"invoke(?:virtual|special|static|interface) +#\d+ *// Method ([\w/$]+)\.([\w$<>]+):(\S+)")
CALL_S_RE = re.compile(
    r"invoke(?:virtual|special|static|interface) +#\d+ *// Method ([\w$<>]+):(\S+)")
STR_RE = re.compile(r"// String (.*)")


def callees(body):
    """Return list of (class_or_None, name, desc); None = same class."""
    out = []
    rest = CALL_Q_RE.sub("", body)
    for c, name, desc in CALL_Q_RE.findall(body):
        out.append((c, name, desc))
    for name, desc in CALL_S_RE.findall(rest):
        out.append((None, name, desc))
    return out


def strings(body):
    return STR_RE.findall(body)


def has(body, op):
    return re.search(op.replace(" ", r"\s+"), body) is not None


def is_public(m):
    return "public" in m["flags"]


def is_static(m):
    return "static" in m["flags"]


def fmt(mcls, m):
    return "%s.%s%s" % (mcls, m["name"], m["desc"])


# ---------------------------------------------------------------------------
# step 1: family detection (structural)
# ---------------------------------------------------------------------------
def detect_family(db):
    if "com/mojang/minecraft/level/Level.class" in db.names:
        return "G1"
    if any(x.endswith(".class") and "/" not in x for x in db.names):
        return "G3"
    if any(x.startswith("net/minecraft/") for x in db.names):
        return "G2"
    return None


# ---------------------------------------------------------------------------
# step 2: World class discovery via per-generation string probes
# ---------------------------------------------------------------------------
def find_world_class(db, family, chain):
    scored = []
    for name in db.names:
        if not name.endswith(".class"):
            continue
        cls = name[:-6]
        data = db.read(cls)
        hits = [p for p in PROBES[family] if p.encode("ascii", "ignore") in data]
        if not hits:
            continue
        # shape validation: World must expose public (III)I and (IIII)Z
        meths = db.methods(cls, code=False)
        has_get = any(m["desc"] == "(III)I" and is_public(m) and not is_static(m)
                      for m in meths)
        has_set = any(m["desc"] == "(IIII)Z" and is_public(m) and not is_static(m)
                      for m in meths)
        if has_get and has_set:
            scored.append((len(hits), cls, hits))
    if not scored:
        chain.append("FAIL: no class passed probe+shape validation")
        return None, []
    scored.sort(key=lambda t: (-t[0], t[1]))
    best = scored[0]
    chain.append("probe scan (%s group: %s) -> shape-validated candidates: %s"
                 % (family, ",".join(PROBES[family]),
                    "; ".join("%s(hits=%d:%s)" % (c, s, ",".join(h))
                              for s, c, h in scored[:4])))
    others = [c for s, c, h in scored[1:] if s == best[0]]
    if others:
        chain.append("WARN: score tie with %s" % others)
        return best[1], others
    if best[0] == 1:
        chain.append("single probe hit on %s (weak but unique after shape filter)"
                     % best[1])
    return best[1], others


# ---------------------------------------------------------------------------
# facet: getBlockId
# ---------------------------------------------------------------------------
def chunk_getter_is_unsigned_byte(db, world_cls, callee_cls, callee_name,
                                  callee_desc):
    """The chunk-side block-id getter reads the byte array directly (baload),
    with no nested (III)I delegation; the metadata getter delegates to a
    nibble section object or does ishr/bipush-15 arithmetic (verscan 0327/0618/
    0629/a1.x/b1.x: World.getBlockId -> chunk.a|b(III)I -> baload)."""
    if callee_cls == world_cls:
        return False
    for m in db.methods(callee_cls, code=True):
        if m["name"] == callee_name and m["desc"] == callee_desc:
            b = m["body"]
            nested = [c for c in callees(b) if c[2] == "(III)I"]
            return has(b, "baload") and not nested
    return False


def compile_get_block_id(db, world_cls, chain):
    meths = db.methods(world_cls, code=True)
    cands = [m for m in meths
             if m["desc"] == "(III)I" and is_public(m) and not is_static(m)]
    chain.append("candidates: public non-static (III)I on %s = [%s]"
                 % (world_cls, ", ".join(m["name"] for m in cands)))
    if not cands:
        return None, "LOW", chain
    if len(cands) == 1:
        chain.append("unique (III)I candidate -> %s" % cands[0]["name"])
        return cands[0], "HIGH", chain
    strong = []
    for m in cands:
        b = m["body"]
        if has(b, "baload"):
            chain.append("%s%s: body reads byte array directly (baload) "
                         "-> unsigned block-id read signature" % (m["name"], m["desc"]))
            strong.append(m)
            continue
        sub = [c for c in callees(b) if c[2] == "(III)I"]
        hit = False
        for ccls, cname, cdesc in sub:
            full = ccls if ccls else world_cls
            if chunk_getter_is_unsigned_byte(db, world_cls, full, cname, cdesc):
                chain.append("%s%s: tail-calls chunk getter %s.%s%s which does "
                             "direct baload (no nibble delegation) -> block-id"
                             % (m["name"], m["desc"], full, cname, cdesc))
                hit = True
        if not hit:
            other = [c for c in callees(b) if c[2].startswith("(III)")]
            chain.append("%s%s: callee pattern %s does not match unsigned-byte "
                         "chunk getter -> rejected (getMeta/getLight shape)"
                         % (m["name"], m["desc"],
                            "; ".join("%s%s" % (c[1], c[2]) for c in other) or "none"))
        if hit:
            strong.append(m)
    if len(strong) == 1:
        return strong[0], "HIGH", chain
    if len(strong) > 1:
        chain.append("WARN: %d strong candidates, taking first" % len(strong))
        return strong[0], "MEDIUM", chain
    chain.append("no candidate matched the unsigned-byte getter rule; fallback")
    return cands[0], "LOW", chain


# ---------------------------------------------------------------------------
# facet: setBlock
# ---------------------------------------------------------------------------
def compile_set_block(db, world_cls, family, chain):
    meths = db.methods(world_cls, code=True)
    cands = [m for m in meths
             if m["desc"] == "(IIII)Z" and is_public(m) and not is_static(m)]
    chain.append("candidates: public non-static (IIII)Z on %s = [%s]"
                 % (world_cls, ", ".join(m["name"] for m in cands)))
    if not cands:
        return None, "LOW", chain
    if len(cands) == 1:
        chain.append("unique (IIII)Z candidate -> %s" % cands[0]["name"])
        return cands[0], "HIGH", chain
    # main write facet = the with-notify wrapper: delegates to a sibling
    # (IIII)Z silent core (verscan: 0223 b->a, 0327/0618/0629 c->a,
    # a1.1.2/a1.2.0 d->a, b1.2_02 e->b, b1.7.3 f->c, b1.8.1 g->d)
    delegates = []
    for m in cands:
        same = [c for c in callees(m["body"])
                if c[0] is None and c[2] == "(IIII)Z"]
        if same:
            chain.append("%s: delegates to sibling (IIII)Z core %s -> wrapper"
                         % (m["name"], same[0][1]))
            delegates.append(m)
        else:
            chain.append("%s: no sibling (IIII)Z delegation -> silent core / "
                         "metadata setter" % m["name"])
    pool = delegates or cands
    if len(pool) > 1:
        # tiebreak 1: also calls a sibling (IIII)V neighbor-notification
        pool2 = [m for m in pool
                 if any(c[0] is None and c[2] == "(IIII)V"
                        for c in callees(m["body"]))]
        if pool2:
            chain.append("tiebreak: also calls sibling (IIII)V notification -> [%s]"
                         % ", ".join(m["name"] for m in pool2))
            pool = pool2
    if len(pool) > 1 and family == "G1":
        # tiebreak 2 (G1 readable-name era): setTile carries the networkMode
        # boolean gate (getfield :Z + early iconst_0 ireturn); netSetTile
        # bypasses it (verscan c0.30-c section 1)
        gated = [m for m in pool
                 if re.search(r"// Field \S+:Z", m["body"])
                 and has(m["body"], "iconst_0")]
        if gated:
            chain.append("tiebreak: networkMode boolean gate (getfield :Z + "
                         "early false return) -> %s" % gated[0]["name"])
            pool = gated
    if not delegates:
        chain.append("WARN: no delegation found, fallback to first candidate")
        return pool[0], "LOW", chain
    if len(pool) == 1:
        return pool[0], "HIGH", chain
    chain.append("WARN: %d wrapper candidates remain, taking first" % len(pool))
    return pool[0], "MEDIUM", chain


# ---------------------------------------------------------------------------
# facet: saveWorld
# ---------------------------------------------------------------------------
def body_save_evidence(db, world_cls, m, depth=0):
    """Direct or 1-hop-transitive level-write evidence."""
    b = m["body"]
    if TICK_STRING in " ".join(strings(b)):
        return None
    for s in SAVE_EVIDENCE:
        if s in strings(b):
            return s
    if depth == 0:
        meths = db.methods(world_cls, code=True)
        for ccls, cname, cdesc in callees(b):
            if ccls is None and cdesc == "()V":
                for x in meths:
                    if x["name"] == cname and x["desc"] == "()V":
                        e = body_save_evidence(db, world_cls, x, depth + 1)
                        if e:
                            return "%s (via %s()V)" % (e, cname)
    return None


def compile_save_world(db, world_cls, family, chain):
    meths = db.methods(world_cls, code=True)
    # rule A (G3 level.dat+progress era): public (Lx;)V wrapper delegating to
    # a sibling (ZLx;)V core that carries save evidence (verscan 0629 onward:
    # saveWorld(progress) public wrapper; the (Z)Z tick body is FORBIDDEN)
    for w in meths:
        if not (is_public(w) and not is_static(w)
                and re.match(r"^\(L(?!java/)[\w/$]+;\)V$", w["desc"])):
            continue
        arg = w["desc"][2:-3]
        core_desc = "(ZL%s;)V" % arg
        for c in callees(w["body"]):
            if c[0] is None and c[2] == core_desc:
                for core in meths:
                    if core["name"] == c[1] and core["desc"] == core_desc:
                        ev = body_save_evidence(db, world_cls, core)
                        if ev:
                            chain.append("rule A: public wrapper %s%s delegates "
                                         "to core %s%s carrying save evidence '%s'"
                                         % (w["name"], w["desc"], core["name"],
                                            core_desc, ev))
                            return w, "HIGH", chain, None
    # rule B (0327): (Z)V core that writes level.dat itself (private allowed;
    # the tick body a(Z)Z is excluded by descriptor and by TICK_STRING).
    # Priority note (recorded in report): the 0327 and 0618 detailed tables
    # pin isomorphic bytecode at different hook levels (core vs ()V wrapper);
    # this compiler follows the 0327/phase1 discipline and pins the core.
    for w in meths:
        if not (not is_static(w) and w["desc"] == "(Z)V"):
            continue
        if TICK_STRING in " ".join(strings(w["body"])):
            chain.append("rule B: %s(Z)V rejected as tick body (TickNextTick)"
                         % w["name"])
            continue
        ev = body_save_evidence(db, world_cls, w)
        if ev:
            chain.append("rule B: (Z)V core %s carries save evidence '%s'"
                         % (w["name"], ev))
            return w, "HIGH", chain, None
    # rule C (0618 fallback): public final ()V whose only call is a sibling (Z)V
    for w in meths:
        if not (is_public(w) and not is_static(w) and w["desc"] == "()V"):
            continue
        cs = callees(w["body"])
        if len(cs) == 1 and cs[0][0] is None and cs[0][2] == "(Z)V":
            for core in meths:
                if core["name"] == cs[0][1] and core["desc"] == "(Z)V":
                    ev = body_save_evidence(db, world_cls, core)
                    if ev:
                        chain.append("rule C: public ()V wrapper %s%s delegates "
                                     "solely to (Z)V core %s carrying '%s'"
                                     % (w["name"], w["desc"], core["name"], ev))
                        return w, "HIGH", chain, None
    # rule D (G1/G2-early serializer era): the world serializer lives on a
    # different class; find (LWorld;Ljava/io/File;)Z first (c0.30), then
    # (LWorld;Ljava/io/OutputStream;)V (0223). Descriptors are searched as
    # literal constant-pool bytes, then confirmed with javap.
    for d in ["(L%s;Ljava/io/File;)Z" % world_cls,
              "(L%s;Ljava/io/OutputStream;)V" % world_cls]:
        hits = []
        for name in db.names:
            if not name.endswith(".class"):
                continue
            cls = name[:-6]
            if cls == world_cls:
                continue
            if d.encode("ascii") in db.read(cls):
                hits.append(cls)
        # rank: the declaring storage manager also carries save-evidence
        # strings; plain callers referencing the descriptor do not
        def rank(c):
            data = db.read(c)
            return -sum(1 for s in SAVE_EVIDENCE + ["MinecraftLevel"]
                        if s.encode("ascii") in data)
        hits.sort(key=rank)
        for cls in hits:
            for m in db.methods(cls, code=False):
                if m["desc"] == d:
                    chain.append("rule D: serializer method %s.%s%s found on %s "
                                 "(storage manager for this era)"
                                 % (cls, m["name"], d, cls))
                    return m, "HIGH", chain, cls
    chain.append("FAIL: no saveWorld facet matched rules A-D")
    return None, "LOW", chain, None


# ---------------------------------------------------------------------------
# top-level compile
# ---------------------------------------------------------------------------
def compile_jar(jarpath, version_id):
    db = ClassDb(jarpath)
    result = {"versionId": version_id, "jar": jarpath}
    try:
        family = detect_family(db)
        result["family"] = family
        if family is None:
            result["error"] = "unknown jar family"
            return result
        chain_w = []
        world_cls, _ = find_world_class(db, family, chain_w)
        result["worldClass"] = world_cls
        result["worldChain"] = chain_w
        if world_cls is None:
            result["error"] = "World class not found"
            return result
        result["facets"] = {}
        for facet, fn in (("getBlockId",
                           lambda ch: compile_get_block_id(db, world_cls, ch)),
                          ("setBlock",
                           lambda ch: compile_set_block(db, world_cls, family, ch)),
                          ("saveWorld",
                           lambda ch: compile_save_world(db, world_cls, family, ch))):
            chain = []
            out = fn(chain)
            if len(out) == 4:
                m, conf, chain, cls_over = out
            else:
                m, conf, chain = out
                cls_over = None
            chosen = None
            if m is not None:
                chosen = {"class": cls_over or world_cls, "name": m["name"],
                          "descriptor": m["desc"]}
            result["facets"][facet] = {"chosen": chosen, "confidence": conf,
                                       "chain": chain}
        return result
    finally:
        db.close()


# ---------------------------------------------------------------------------
# F5 reconciliation against the anchor library
# ---------------------------------------------------------------------------
def load_oracle(jdata):
    """Return {facet: {class,name,descriptor}} for either registry schema."""
    if "targets" in jdata:  # legacy schema
        t = jdata["targets"]
        out = {}
        for facet in ("getBlockId", "setBlock", "saveWorld"):
            slot = t.get(facet)
            if slot:
                out[facet] = {"class": slot["class"].replace(".", "/"),
                              "name": slot["name"],
                              "descriptor": slot["descriptor"]}
        return out
    if "hooks" in jdata:  # runtime schema (b1.7.3 / b1.8.1)
        sw = jdata["hooks"]["SAVE_WORLD"]
        ops = jdata["ops"]
        w = sw["class"].replace(".", "/")
        return {
            "getBlockId": {"class": w, "name": ops["getBlockM"],
                           "descriptor": "(III)I"},
            "setBlock": {"class": w, "name": ops["setBlockM"],
                         "descriptor": "(IIII)Z"},
            "saveWorld": {"class": w, "name": ops["saveWorldM"],
                          "descriptor": ops["saveWorldDesc"]},
        }
    return {}


def resolve_jar(vid, jdata):
    cands = []
    if "jar" in jdata:
        cands.append(os.path.expandvars(jdata["jar"].replace("/", os.sep)))
    base = os.path.expandvars(r"%APPDATA%\.minecraft\versions")
    cands.append(os.path.join(base, vid, vid + ".jar"))
    cands.append(os.path.join(base, vid, vid + ".zip"))
    p = glob.glob(os.path.join(base, vid, "*.jar"))
    cands.extend(p)
    attic = os.path.expandvars(r"%APPDATA%\.attic\versions")
    cands.extend(glob.glob(os.path.join(attic, vid + "*.jar")))
    for c in cands:
        if c and os.path.isfile(c):
            return c
    return None


def audit_oracle(jarpath, oracle_slot, compiled_slot=None):
    """When a slot misses, collect evidence about the oracle method itself:
    existence, access flags, tick-body marker, and whether it is a pure
    delegation wrapper of the compiled facet (hook-level equivalence)."""
    db = ClassDb(jarpath)
    try:
        cls = oracle_slot["class"]
        for m in db.methods(cls, code=True):
            if m["name"] == oracle_slot["name"] and m["desc"] == oracle_slot["descriptor"]:
                ev = {"exists": True, "flags": sorted(m["flags"]),
                      "tick_body": TICK_STRING in " ".join(strings(m["body"])),
                      "calls_foreign_IIII_V": any(
                          c[2] == "(IIII)V" and c[0]
                          for c in callees(m["body"]))}
                if compiled_slot:
                    rel = [c for c in callees(m["body"])
                           if (c[0] or cls) == compiled_slot["class"]
                           and c[1] == compiled_slot["name"]
                           and c[2] == compiled_slot["descriptor"]]
                    ev["delegates_to_compiled"] = bool(rel)
                return ev
        return {"exists": False}
    except KeyError:
        return {"exists": "class-missing"}
    finally:
        db.close()


def run_recon(fp_dir, report_path):
    rows = []
    for jpath in sorted(glob.glob(os.path.join(fp_dir, "*.json"))):
        with open(jpath, "r", encoding="utf-8") as f:
            jdata = json.load(f)
        vid = jdata.get("versionId") or os.path.splitext(os.path.basename(jpath))[0]
        oracle = load_oracle(jdata)
        if not oracle:
            rows.append({"version": vid, "status": "SKIPPED",
                         "reason": "no P1 oracle slots in schema"})
            continue
        jarpath = resolve_jar(vid, jdata)
        if jarpath is None:
            rows.append({"version": vid, "status": "SKIPPED",
                         "reason": "jar not found"})
            continue
        expected = [jdata["jarSha1"].lower()]
        if jdata.get("jarSha1Alt"):
            expected.append(jdata["jarSha1Alt"].lower())
        actual = sha1_of(jarpath)
        if actual not in expected:
            rows.append({"version": vid, "status": "SKIPPED",
                         "reason": "SHA1 mismatch (want %s got %s)"
                                   % (expected[0], actual)})
            continue
        res = compile_jar(jarpath, vid)
        row = {"version": vid, "status": "RUN", "jar": jarpath,
               "family": res.get("family"), "world": res.get("worldClass"),
               "worldChain": res.get("worldChain", []), "facets": {}}
        for facet in ("getBlockId", "setBlock", "saveWorld"):
            got = (res.get("facets") or {}).get(facet, {})
            want = oracle.get(facet)
            chosen = got.get("chosen")
            cell = {"oracle": want, "compiled": chosen,
                    "confidence": got.get("confidence"),
                    "chain": got.get("chain", [])}
            if want is None:
                cell["verdict"] = "NO-ORACLE"
            elif chosen is None:
                cell["verdict"] = "MISS"
            else:
                same = all(chosen[k] == want[k]
                           for k in ("class", "name", "descriptor"))
                cell["verdict"] = "MATCH" if same else "MISS"
                if not same:
                    cell["oracle_audit"] = audit_oracle(jarpath, want, chosen)
            row["facets"][facet] = cell
        rows.append(row)
    write_report(rows, report_path)
    return rows


def write_report(rows, path):
    lines = []
    lines.append("# F5 reconciliation report (F1 slow-path compiler vs anchor library)")
    lines.append("")
    lines.append("Facets: getBlockId (III)I / setBlock (IIII)Z / saveWorld "
                 "(era-dependent form). Verdicts: MATCH = class+name+descriptor "
                 "all equal; MISS = differs (see oracle audit); SKIPPED = jar "
                 "missing or SHA1 gate failed.")
    lines.append("")
    lines.append("## Matrix")
    lines.append("")
    lines.append("| version | family | World | getBlockId | setBlock | saveWorld |")
    lines.append("|---|---|---|---|---|---|")
    for r in rows:
        if r["status"] != "RUN":
            lines.append("| %s | - | - | SKIPPED: %s | SKIPPED | SKIPPED |"
                         % (r["version"], r.get("reason", "")))
            continue
        cells = []
        for facet in ("getBlockId", "setBlock", "saveWorld"):
            c = r["facets"][facet]
            v = c["verdict"]
            if v == "MISS":
                audit = c.get("oracle_audit") or {}
                if audit.get("tick_body"):
                    tag = "MISS(SUSPECT:oracle-is-tick-body)"
                elif audit.get("delegates_to_compiled"):
                    tag = "MISS(SUSPECT:oracle-wrapper-of-compiled-core)"
                elif facet == "setBlock" and audit.get("calls_foreign_IIII_V"):
                    tag = "MISS(SUSPECT:oracle-is-chunk-metadata-write)"
                else:
                    tag = "MISS"
                v = "%s %s!=%s" % (tag,
                                   (c["compiled"] or {}).get("name", "?"),
                                   (c["oracle"] or {}).get("name", "?"))
            cells.append(v)
        lines.append("| %s | %s | %s | %s | %s | %s |"
                     % (r["version"], r["family"], r["world"], *cells))
    lines.append("")
    total = sum(1 for r in rows if r["status"] == "RUN") * 3
    match = sum(1 for r in rows if r["status"] == "RUN"
                for c in r["facets"].values() if c["verdict"] == "MATCH")
    suspects = []
    for r in rows:
        if r["status"] != "RUN":
            continue
        for facet, c in r["facets"].items():
            if c["verdict"] == "MISS":
                audit = c.get("oracle_audit") or {}
                if audit.get("tick_body") or audit.get("delegates_to_compiled") \
                        or (facet == "setBlock"
                            and audit.get("calls_foreign_IIII_V")):
                    suspects.append("%s/%s: compiled=%s oracle=%s %s"
                                    % (r["version"], facet,
                                       json.dumps(c["compiled"]),
                                       json.dumps(c["oracle"]),
                                       json.dumps(audit)))
    skipped = ["%s: %s" % (r["version"], r.get("reason", ""))
               for r in rows if r["status"] != "RUN"]
    lines.append("## Summary")
    lines.append("")
    lines.append("- facets compared: %d, MATCH: %d, MISS: %d, SKIPPED versions: %d"
                 % (total, match, total - match, len(skipped)))
    lines.append("- SUSPECT (anchor library pending human adjudication; "
                 "compiler did NOT modify any JSON):")
    for s in suspects:
        lines.append("  - %s" % s)
    if not suspects:
        lines.append("  - none")
    if skipped:
        lines.append("- SKIPPED:")
        for s in skipped:
            lines.append("  - %s" % s)
    lines.append("- note: 0327 and 0618 are structurally isomorphic for "
                 "saveWorld (public final ()V wrapper delegating with "
                 "iconst_1 to a private (Z)V level.dat core; identical "
                 "call-graph shape). The detailed tables pin them at "
                 "different hook levels (0327=core a(Z)V per phase1 "
                 "WorldOps, 0618=wrapper f()V per verscan). No generalizable "
                 "rule can reproduce both; this compiler follows the phase1 "
                 "discipline (core preferred) and reports 0618 as a "
                 "hook-level SUSPECT, not a rule failure.")
    lines.append("")
    lines.append("## Per-version evidence chains")
    lines.append("")
    for r in rows:
        if r["status"] != "RUN":
            continue
        lines.append("### %s (family %s, World=%s)" % (r["version"], r["family"],
                                                       r["world"]))
        lines.append("")
        for step in r["worldChain"]:
            lines.append("- world: %s" % step)
        for facet in ("getBlockId", "setBlock", "saveWorld"):
            c = r["facets"][facet]
            lines.append("- **%s** verdict=%s confidence=%s"
                         % (facet, c["verdict"], c["confidence"]))
            lines.append("  - oracle: %s" % json.dumps(c["oracle"]))
            lines.append("  - compiled: %s" % json.dumps(c["compiled"]))
            if c.get("oracle_audit"):
                lines.append("  - oracle audit: %s" % json.dumps(c["oracle_audit"]))
            for step in c["chain"]:
                lines.append("  - %s" % step)
        lines.append("")
    with open(path, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))


def main():
    ap = argparse.ArgumentParser(description="F1 anchor compiler / F5 recon")
    ap.add_argument("--jar")
    ap.add_argument("--version-id")
    ap.add_argument("--recon", action="store_true")
    ap.add_argument("--fingerprints-dir",
                    default=os.path.join(os.path.dirname(os.path.abspath(__file__)),
                                         "..", "..", "java", "agent", "registry",
                                         "fingerprints"))
    ap.add_argument("--report",
                    default=os.path.join(os.path.dirname(os.path.abspath(__file__)),
                                         "report.md"))
    args = ap.parse_args()
    if args.recon:
        rows = run_recon(os.path.normpath(args.fingerprints_dir),
                         os.path.normpath(args.report))
        n_run = sum(1 for r in rows if r["status"] == "RUN")
        n_match = sum(1 for r in rows if r["status"] == "RUN"
                      and all(c["verdict"] == "MATCH"
                              for c in r["facets"].values()))
        print("recon: %d versions run, %d fully MATCH; report -> %s"
              % (n_run, n_match, args.report))
        return 0
    if not args.jar or not args.version_id:
        ap.error("--jar and --version-id required in compile mode")
    res = compile_jar(args.jar, args.version_id)
    json.dump(res, sys.stdout, indent=2)
    print()
    return 0


if __name__ == "__main__":
    sys.exit(main())
