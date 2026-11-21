#!/usr/bin/env python3
"""F1 slow-path anchor compiler (expanded facet set).

Forensic pipeline, javap-based (project culture: bytecode evidence, see
java/agent/registry/fingerprints/README.md and docs/compatibility/verscan/):

  1. detect generation family from jar entry layout (structural)
  2. locate the World class with per-generation string-constant probes
  3. compile each facet with generation-dispatched structural rules
  4. emit candidate quintuples (class + name + descriptor) + evidence chain
     + confidence (HIGH / MEDIUM / LOW)

Rules are per-generation dispatches over bytecode features (NBT key
bindings, field-graph shapes, call-graph signatures). Version answers are
never table-looked-up. Facets whose rule does not generalize are reported
BLOCKED with a one-line reason instead of being forced.

Modes:
  f1_compile.py --jar PATH --version-id ID     compile one jar, JSON stdout
  f1_compile.py --recon [--report PATH]        F5 reconciliation vs registry
  f1_compile.py --draft VERSION                full-facet draft table ->
                                               drafts/VERSION.md (or SKIP)
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

PROBES = {
    "G1": ["The level is corrupt!"],
    "G2": ["level.dat", "SpawnX", "RandomSeed", "MinecraftLevel",
           "The level is corrupt!", "TickNextTick list out of synch"],
    "G3": ["level.dat", "SpawnX", "RandomSeed",
           "TickNextTick list out of synch", "Saving chunks", "Saving level"],
}
TICK_STRING = "TickNextTick list out of synch"
SAVE_EVIDENCE = ["level.dat", "Saving level", "Saving chunks"]

WORKDIR = os.path.join(os.path.expandvars(r"%TEMP%"), "mcaut", "f1")

# facet columns for the F5 matrix, in report order
FACETS = [
    "getBlockId", "setBlock", "saveWorld",
    "worldTime", "randSeed", "spawnX", "spawnY", "spawnZ", "difficulty",
    "getMeta", "setMetaSilent", "getLight",
    "entityClass", "setPosRot", "posX", "posY", "posZ",
    "motX", "motY", "motZ", "yaw", "pitch", "iframes", "entityType",
    "entityList", "spawnEntity", "setEntityDead", "playSound", "explode",
    "itemStackClass", "isCount", "isId", "isDmg",
    "inventoryPlayerClass", "invMain", "invArmor", "invCur", "addStack",
    "invField", "invSize", "invGet", "invSet",
    "tileEntityClass", "chestClass", "furnaceClass", "teGet", "teBurn",
    "teCook",
    "minecraftClass", "runTick", "guiIngame",
    "mobPig", "mobSheep", "mobCreeper", "mobSkeleton", "mobZombie",
    "mobSpider", "mobTnt", "mobGiant", "mobBase", "mobCreatureClass",
    "scatterClass",
    # --- batch-3 extended facet set (51 new columns) ---
    "worldClass", "blockClass", "blockRegistry", "itemClass", "itemRegistry",
    "biomeClass", "biomeNameField", "biomeAt", "worldChunkManagerClass",
    "entityListClass", "entityListLookup", "entityPlayerClass",
    "entityHumanClass", "entityLivingClass", "playerClientClass",
    "playerField", "playerListField", "thePlayerField", "theWorldField",
    "ingameGuiField", "health", "getClosestPlayer",
    "itemEntityClass", "itemEntityCtor", "itemStackCtor", "iInventoryClass",
    "invCount", "teSet", "signClass", "mobSpawnerClass", "mobChicken",
    "mobCow", "mobSquid", "mobMonsterClass", "mobTntCtor", "fontRendererClass",
    "getChunk", "chunkGenHeight", "getBlockIdByte", "light6",
    "updateNeighbors", "setBlockSilent", "setBlockNoNotify", "setBlockNet",
    "setSpawnPos", "observers", "snowCovered", "loadWorld", "saveWorldBody",
    "progressClass", "worldCtor",
]
# facets whose rule does not generalize (recorded, never forced)
BLOCKED = {
    "difficulty": "new criterion (unique World int field written by the"
                  " client tick from the settings object; tie-break by"
                  " external reader count) identifies the difficulty field"
                  " in all four legacy slots, but the runtime registry maps"
                  " difficultyF onto WorldInfo where no such field exists"
                  " (b1.7.3 ei.q / b1.8.1 rl.v absent); unblocking would"
                  " emit false MISS, so kept BLOCKED pending oracle fix",
}


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


CALL_RE = re.compile(
    r"invoke(?:virtual|special|static|interface) +#\d+,? *\d* *// "
    r"(?:InterfaceMethod|Method) ([\w/$]+)\.([\w$<>]+):(\S+)")
CALL_S_RE = re.compile(
    r"invoke(?:virtual|special|static|interface) +#\d+,? *\d* *// "
    r"(?:InterfaceMethod|Method) ([\w$<>]+):(\S+)")
STR_RE = re.compile(r"// String (.*)")
FLD_Q_RE = re.compile(
    r"(getfield|putfield) +#\d+ *// Field ([\w/$]+)\.([\w$<>]+):(\S+)")
FLD_S_RE = re.compile(
    r"(getfield|putfield) +#\d+ *// Field ([\w$<>]+):(\S+)")


def callees(body):
    out = []
    rest = CALL_RE.sub("\x00", body)
    for c, name, desc in CALL_RE.findall(body):
        out.append((c, name, desc))
    for name, desc in CALL_S_RE.findall(rest):
        out.append((None, name, desc))
    return out


def field_refs(body):
    """ordered (op, class_or_None, name, desc) field refs; None = same class"""
    out = []
    pos = 0
    marks = []
    for m in FLD_Q_RE.finditer(body):
        marks.append((m.start(), (m.group(1), m.group(2), m.group(3),
                                  m.group(4))))
    for m in FLD_S_RE.finditer(body):
        marks.append((m.start(), (m.group(1), None, m.group(2), m.group(3))))
    marks.sort()
    for _, t in marks:
        out.append(t)
    return out


def strings(body):
    return STR_RE.findall(body)


def has(body, op):
    return re.search(op.replace(" ", r"\s+"), body) is not None


HEADER_RE = re.compile(r"^  (.*?);\s*$")
NAME_RE = re.compile(r"([\w$<>]+)\(")


def parse_methods(text):
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
            words = set(re.findall(
                r"\b(?:public|private|protected|static|final|abstract|"
                r"synchronized|native)\b", header))
            j = i + 2
            body = []
            while j < n:
                mm = HEADER_RE.match(lines[j])
                if mm and "(" in lines[j] and j + 1 < n and \
                        lines[j + 1].strip().startswith("descriptor:"):
                    break
                body.append(lines[j])
                j += 1
            methods.append({"header": header, "name": name, "desc": desc,
                            "flags": words, "body": "\n".join(body)})
            i = j
        else:
            i += 1
    return methods


def parse_fields(text):
    """ordered (name, desc, static) instance fields from javap -p -s."""
    lines = text.splitlines()
    fields = []
    for i, l in enumerate(lines[:-1]):
        nxt = lines[i + 1].strip()
        if nxt.startswith("descriptor:") and "(" not in l and \
                re.match(r"^\s+(public|private|protected|static|final|"
                         r"transient|volatile)", l):
            desc = nxt.split(":", 1)[1].strip()
            nm = re.search(r"([\w$]+);?\s*$", l)
            if nm:
                fields.append((nm.group(1), desc, "static" in l))
    return fields


CLASS_HDR = re.compile(r"^(?:\w+ )*(?:class|interface) ([\w.$]+)"
                       r"(?:[^\n]*)\{\s*$")
_BATCH = {"jar": None, "blocks": None}


class ClassDb:
    def __init__(self, jarpath):
        self.jarpath = jarpath
        self.zf = zipfile.ZipFile(jarpath)
        self.names = self.zf.namelist()
        self._meth = {}
        self._head = {}

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

    def _batch(self):
        """one-shot javap -p -s -c over the whole jar (chunked), split into
        per-class blocks; cached for the most recent jar."""
        if _BATCH["jar"] == self.jarpath:
            return _BATCH["blocks"]
        blocks = {}
        try:
            h = hashlib.sha1(self.jarpath.encode()).hexdigest()[:12]
            d = os.path.join(WORKDIR, "x_" + h)
            if not os.path.isdir(d):
                os.makedirs(WORKDIR, exist_ok=True)
                self.zf.extractall(d)
            files = [n for n in self.names if n.endswith(".class")]
            for i in range(0, len(files), 150):
                r = subprocess.run(["javap", "-p", "-s", "-c"] +
                                   files[i:i + 150],
                                   capture_output=True, text=True,
                                   cwd=d, errors="replace")
                cur, buf = None, []
                for line in r.stdout.splitlines():
                    m = CLASS_HDR.match(line)
                    if m:
                        if cur is not None:
                            blocks[cur] = "\n".join(buf)
                        cur = norm_cls(m.group(1))
                        buf = [line]
                    elif cur is not None:
                        buf.append(line)
                if cur is not None:
                    blocks[cur] = "\n".join(buf)
        except Exception:
            blocks = {}
        _BATCH["jar"] = self.jarpath
        _BATCH["blocks"] = blocks
        return blocks

    def _text(self, cls):
        b = self._batch()
        if cls in b:
            return b[cls]
        return javap_text(self._extract(cls), code=True)

    def head(self, cls):
        if cls not in self._head:
            self._head[cls] = self._text(cls)
        return self._head[cls]

    def methods(self, cls, code=False):
        if cls not in self._meth:
            self._meth[cls] = parse_methods(self._text(cls))
        return self._meth[cls]

    def fields(self, cls):
        return parse_fields(self.head(cls))

    def superclass(self, cls):
        try:
            h = self.head(cls).splitlines()[0]
        except Exception:
            return None
        m = re.search(r"extends ([\w/$.]+)", h)
        sup = norm_cls(m.group(1)) if m else None
        if sup and sup + ".class" not in self.names:
            return None
        return sup

    def super_chain(self, cls):
        out = []
        c = cls
        while c and c not in out:
            out.append(c)
            c = self.superclass(c)
        return out

    def class_simple(self, name):
        return name[:-6] if name.endswith(".class") else None


def norm_cls(c):
    if c is None:
        return None
    return c.replace(".", "/") if "/" not in c else c


def is_public(m):
    return "public" in m["flags"]


def is_static(m):
    return "static" in m["flags"]


def detect_family(db):
    if "com/mojang/minecraft/level/Level.class" in db.names:
        return "G1"
    if any(x.endswith(".class") and "/" not in x for x in db.names):
        return "G3"
    if any(x.startswith("net/minecraft/") for x in db.names):
        return "G2"
    return None


def find_world_class(db, family, chain):
    scored = []
    for name in db.names:
        cls = db.class_simple(name)
        if cls is None:
            continue
        data = db.read(cls)
        hits = [p for p in PROBES[family] if p.encode("ascii", "ignore")
                in data]
        if not hits:
            continue
        meths = db.methods(cls, code=False)
        has_get = any(m["desc"] == "(III)I" and is_public(m) and
                      not is_static(m) for m in meths)
        has_set = any(m["desc"] == "(IIII)Z" and is_public(m) and
                      not is_static(m) for m in meths)
        if has_get and has_set:
            scored.append((len(hits), cls, hits))
    if not scored:
        chain.append("FAIL: no class passed probe+shape validation")
        return None
    scored.sort(key=lambda t: (-t[0], t[1]))
    best = scored[0]
    chain.append("probe scan (%s) -> shape-validated: %s"
                 % (family, "; ".join("%s(hits=%d)" % (c, s)
                                      for s, c, h in scored[:4])))
    return best[1]


# ---------------------------------------------------------------------------
# NBT key -> field binding engine (shared by World fields, ItemStack, TE)
# ---------------------------------------------------------------------------
def nbt_field_binds(db, key, holder_pred, ftype=None):
    """Find fields bound to ldc key within +-window in any class body.
    Returns list of (holder_cls, field_name, fdesc). holder_pred filters the
    declaring class of the bound field."""
    out = []
    kb = key.encode("ascii")
    for name in db.names:
        cls = db.class_simple(name)
        if cls is None:
            continue
        if kb not in db.read(cls):
            continue
        for m in db.methods(cls, code=True):
            b = m["body"]
            for lm in STR_RE.finditer(b):
                if lm.group(1) != key:
                    continue
                win = b[lm.end():lm.end() + 1200]
                for op, fcls, fname, fdesc in field_refs(win):
                    holder = fcls if fcls else cls
                    if ftype and fdesc != ftype:
                        continue
                    if holder_pred(holder):
                        t = (holder, fname, fdesc)
                        if t not in out:
                            out.append(t)
                        break
                break
    return out


def world_holder_pred(db, world_cls):
    """accept World itself, or any class typed as a World instance field
    (WorldInfo-style data holder)."""
    extra = set()
    for nm, d, st in db.fields(world_cls):
        m = re.match(r"^L([\w/$]+);$", d)
        if m and not st:
            extra.add(m.group(1))
    def pred(holder):
        return holder == world_cls or holder in extra
    return pred


def compile_world_fields(db, world_cls, family, facets, chain):
    pred = world_holder_pred(db, world_cls)
    for facet, key, ftype in (
            ("worldTime", "Time", "J"),
            ("randSeed", "RandomSeed", "J"),
            ("spawnX", "SpawnX", "I"),
            ("spawnY", "SpawnY", "I"),
            ("spawnZ", "SpawnZ", "I")):
        ch = []
        binds = nbt_field_binds(db, key, pred, ftype)
        binds = [b for b in binds if b[0] == world_cls] or binds
        uniq = {}
        for holder, fname, fdesc in binds:
            uniq.setdefault((holder, fdesc), []).append(fname)
        if facet in ("spawnX", "spawnY", "spawnZ"):
            # "Spawn" group key (indev .mclevel) binds 3 ints in order
            if not binds and family == "G2":
                binds = _indev_spawn(db, world_cls, facet, ch)
            if not binds and family == "G1":
                # readable-name era: the spawn point is stored in the
                # per-axis <axis>Spawn int fields of the level class
                axis = facet[-1].lower()
                for nm, d, st in db.fields(world_cls):
                    if d == "I" and not st and nm == axis + "Spawn":
                        binds = [(world_cls, nm, "I")]
                        ch.append("G1 readable spawn field %s" % nm)
        if not binds and facet == "randSeed" and family != "G1":
            # pre-NBT-key era fallback: the world's single long field is
            # the level seed when no other J-typed field competes
            js = [(world_cls, nm, d) for nm, d, st in
                  db.fields(world_cls) if d == "J" and not st]
            if len(js) == 1:
                binds = js
                ch.append("unique instance J field -> level seed")
        if len(binds) == 1:
            holder, fname, fdesc = binds[0]
            ch.append("NBT key '%s' binds unique %s.%s:%s"
                      % (key, holder, fname, fdesc))
            facets[facet] = ({"class": holder, "name": fname,
                              "descriptor": fdesc}, "HIGH", ch)
        elif binds:
            holder, fname, fdesc = binds[0]
            ch.append("NBT key '%s' binds %d candidates %s; take first"
                      % (key, len(binds), [b[1] for b in binds]))
            facets[facet] = ({"class": holder, "name": fname,
                              "descriptor": fdesc}, "MEDIUM", ch)
        else:
            ch.append("no NBT binding for '%s'" % key)
            facets[facet] = (None, "LOW", ch)


def _indev_spawn(db, world_cls, facet, ch):
    """indev .mclevel: ldc 'Spawn' followed by three consecutive int fields."""
    order = {"spawnX": 0, "spawnY": 1, "spawnZ": 2}
    for name in db.names:
        cls = db.class_simple(name)
        if cls is None or b"Spawn" not in db.read(cls):
            continue
        for m in db.methods(cls, code=True):
            b = m["body"]
            for lm in STR_RE.finditer(b):
                if lm.group(1) != "Spawn":
                    continue
                win = b[lm.end():lm.end() + 2000]
                ints = []
                for op, fcls, fname, fdesc in field_refs(win):
                    if fdesc == "I" and op == "putfield":
                        holder = fcls if fcls else cls
                        if holder == world_cls and fname not in [x[1]
                                                                 for x in ints]:
                            ints.append((holder, fname))
                if len(ints) >= 3:
                    ch.append("indev 'Spawn' key binds 3 consecutive int "
                              "fields %s -> %s = #%d"
                              % ([i[1] for i in ints[:3]], facet,
                                 order[facet]))
                    return [ints[order[facet]] + ("I",)]
    return []


# ---------------------------------------------------------------------------
# nibble facets: getMeta / setMetaSilent / getLight
# ---------------------------------------------------------------------------
def chunk_getter_is_unsigned_byte(db, world_cls, c_cls, c_name, c_desc):
    if c_cls == world_cls:
        return False
    for m in db.methods(c_cls, code=True):
        if m["name"] == c_name and m["desc"] == c_desc:
            b = m["body"]
            nested = [c for c in callees(b) if c[2] == "(III)I"]
            return has(b, "baload") and not nested
    return False


def compile_get_block_id(db, world_cls, chain):
    meths = db.methods(world_cls, code=True)
    cands = [m for m in meths if m["desc"] == "(III)I" and is_public(m)
             and not is_static(m)]
    chain.append("candidates: public (III)I = [%s]"
                 % ", ".join(m["name"] for m in cands))
    if not cands:
        return None, "LOW", chain
    if len(cands) == 1:
        return cands[0], "HIGH", chain
    strong = []
    for m in cands:
        sub = [c for c in callees(m["body"]) if c[2] == "(III)I"]
        hit = any(chunk_getter_is_unsigned_byte(
            db, world_cls, c[0] or world_cls, c[1], c[2]) for c in sub)
        if hit:
            chain.append("%s: chunk delegate is direct baload getter -> "
                         "block id" % m["name"])
            strong.append(m)
    if len(strong) == 1:
        return strong[0], "HIGH", chain
    chain.append("fallback first candidate")
    return cands[0], "LOW", chain


def compile_get_meta(db, world_cls, chain):
    meths = db.methods(world_cls, code=True)
    cands = [m for m in meths
             if m["desc"] in ("(III)I", "(III)B") and is_public(m)
             and not is_static(m)]
    chain.append("candidates: public (III)I|(III)B = [%s]"
                 % ", ".join("%s%s" % (m["name"], m["desc"]) for m in cands))
    nib = []
    for m in cands:
        b = m["body"]
        if has(b, "baload") and (has(b, "ishr")
                                 or (re.search(r"bipush\s+15", b)
                                     and has(b, "iand"))):
            chain.append("%s%s: direct nibble read (baload + shift/mask on "
                         "own arrays) -> metadata" % (m["name"], m["desc"]))
            nib.append(m)
            continue
        if has(b, "baload"):
            continue
        sub = [c for c in callees(b) if c[2] in ("(III)I", "(III)B")]
        foreign_nibble = False
        for c in sub:
            cc = c[0] or world_cls
            if cc == world_cls:
                continue
            for cm in db.methods(cc, code=True):
                if cm["name"] == c[1] and cm["desc"] == c[2]:
                    cb = cm["body"]
                    if (has(cb, "ishr") or has(cb, "bipush 15")
                            or any(x[2] in ("(III)I", "(III)B")
                                   for x in callees(cb))):
                        foreign_nibble = True
        if foreign_nibble:
            chain.append("%s%s: delegates to chunk nibble getter (shift/"
                         "mask arithmetic) -> metadata" % (m["name"],
                                                           m["desc"]))
            nib.append(m)
    if len(nib) == 1:
        return nib[0], "HIGH", chain
    if nib:
        # the plain accessor performs a single array read; combinators
        # (offset variants) read the storage more than once
        nib.sort(key=lambda m: len(re.findall(r"\bbaload\b", m["body"])))
        return nib[0], "MEDIUM", chain
    chain.append("no nibble-delegating candidate")
    return None, "LOW", chain


def compile_get_light(db, world_cls, chain):
    meths = db.methods(world_cls, code=True)
    cands = [m for m in meths if m["desc"] == "(III)I" and is_public(m)
             and not is_static(m)]
    lights = []
    for m in cands:
        sub = [c for c in callees(m["body"])
               if c[2] in ("(IIII)I", "(IIIZ)I")]
        if sub:
            chain.append("%s(III)I: delegates to 4-arg int light getter "
                         "(%s) -> light" % (m["name"], sub[0][2]))
            # the boolean-flagged 4-arg form is the sky/block selector
            lights.append((0 if any(c[2] == "(IIIZ)I" for c in sub)
                           else 1, m))
    if lights:
        lights.sort(key=lambda x: x[0])
        if len(lights) == 1:
            return lights[0][1], "HIGH", chain
        return lights[0][1], "MEDIUM", chain
    chain.append("no 4-arg-int-delegating (III)I candidate")
    return None, "LOW", chain


def compile_set_meta_silent(db, world_cls, chain):
    meths = db.methods(world_cls, code=True)
    cands = [m for m in meths if m["desc"] == "(IIII)V" and is_public(m)
             and not is_static(m)]
    chain.append("candidates: public (IIII)V = [%s]"
                 % ", ".join(m["name"] for m in cands))
    def reaches_nibble_write(m, depth=0):
        if has(m["body"], "bastore"):
            return True
        for c in callees(m["body"]):
            cc = c[0] or world_cls
            if c[2] in ("(IIII)V", "(IIII)Z") and cc != world_cls:
                for cm in db.methods(cc, code=True):
                    if cm["name"] == c[1] and cm["desc"] == c[2]:
                        if has(cm["body"], "bastore"):
                            return True
            if cc == world_cls and depth < 2:
                for wm in meths:
                    if wm["name"] == c[1] and wm["desc"] == c[2] and \
                            reaches_nibble_write(wm, depth + 1):
                        return True
        return False
    scored = []
    for m in cands:
        cs = callees(m["body"])
        same = [c for c in cs if c[0] is None and c[2] == "(IIII)V"]
        if len(same) >= 6:
            chain.append("%s: fans out 6 sibling (IIII)V calls -> light6, "
                         "rejected" % m["name"])
            continue
        # a silent setter never rolls effects: block-destroy/notify
        # variants consume Random (particles) and emit sounds
        if any(c[0] == "java/util/Random" for c in cs):
            chain.append("%s: consumes Random (particle/sound effects) -> "
                         "block-destroy variant, rejected" % m["name"])
            continue
        sc = 0
        if any(c[0] is None and c[2] == "(IIII)Z" for c in cs):
            sc += 2
        if reaches_nibble_write(m):
            sc += 1
        if sc:
            scored.append((sc, m))
            chain.append("%s: score %d (sibling-(IIII)Z delegate=%d, "
                         "reaches-nibble=%d)"
                         % (m["name"], sc, 1 if sc >= 2 else 0,
                            1 if sc != 2 else 0))
    if scored:
        scored.sort(key=lambda x: -x[0])
        m = scored[0][1]
        chain.append("take %s -> setMetaSilent" % m["name"])
        return m, "HIGH" if len(scored) == 1 and scored[0][0] >= 2 \
            else "MEDIUM", chain
    chain.append("no nibble-write candidate")
    return None, "LOW", chain


# ---------------------------------------------------------------------------
# Entity discovery + field graph
# ---------------------------------------------------------------------------
def graph_run(fields, want, minlen):
    """find longest run of consecutive same-type instance fields == want."""
    runs, cur = [], []
    for nm, d, st in fields:
        if st:
            continue
        if d == want:
            cur.append(nm)
        else:
            if len(cur) >= minlen:
                runs.append(list(cur))
            cur = []
    if len(cur) >= minlen:
        runs.append(cur)
    return runs


def world_referenced_types(db, world_cls):
    types = set()
    for nm, d, st in db.fields(world_cls):
        for m in re.finditer(r"L([\w/$]+);", d):
            if not st:
                types.add(m.group(1))
    for meth in db.methods(world_cls, code=False):
        for m in re.finditer(r"L([\w/$]+);", meth["desc"]):
            types.add(m.group(1))
    return types


def compile_entity_class(db, world_cls, family, chain):
    refs = world_referenced_types(db, world_cls)
    cands = []
    seen = set()
    for t in refs:
        if t.startswith("java/") or t in seen:
            continue
        seen.add(t)
        try:
            chain_cls = db.super_chain(t)
        except Exception:
            continue
        for anc in chain_cls:
            try:
                fl = db.fields(anc)
            except Exception:
                continue
            for want, ml in (("D", 6), ("F", 8)):
                runs = graph_run(fl, want, ml)
                if runs:
                    cands.append((anc, want, runs[0]))
    uniq = {}
    for anc, want, run in cands:
        uniq.setdefault(anc, (want, run))
    if len(uniq) == 1:
        (cls, (want, run)) = list(uniq.items())[0]
        chain.append("World-referenced type chain -> unique class %s with "
                     "%d consecutive %s fields" % (cls, len(run), want))
        return cls, "HIGH", chain
    if uniq:
        # score candidates: declaring a posrot-shaped method that writes a
        # consecutive triplet of its own same-type fields is decisive;
        # abstract-ness and an F-run trailing the D-run are tiebreakers.
        scored = []
        for cls, (want, run) in uniq.items():
            sc = 0
            try:
                meths = db.methods(cls, code=True)
                fl = db.fields(cls)
                names = [n for n, _, _ in fl]
                for m in meths:
                    if m["desc"] not in ("(DDDFF)V", "(FFFFF)V", "(DDD)V"):
                        continue
                    if not m["body"]:
                        continue
                    tg = set()
                    for fm in re.finditer(
                            r"putfield +#\d+ *// Field (?:[\w/$]+\.)?"
                            r"([\w$<>]+):\S+", m["body"]):
                        tg.add(fm.group(1))
                    idxs = sorted(names.index(t) for t in tg if t in names)
                    if len(idxs) >= 3 and idxs[2] == idxs[0] + 2:
                        sc += 4
                        break
                h = db.head(cls)
                if re.match(r"^(?:\w+ )*abstract ", h):
                    sc += 2
                if want == "D" and graph_run(fl, "F", 2):
                    sc += 1
            except Exception:
                pass
            scored.append((sc, cls))
        scored.sort(reverse=True)
        cls = scored[0][1]
        conf = "HIGH" if scored[0][0] >= 4 and (len(scored) == 1 or
                                                scored[0][0] > scored[1][0]) \
            else "MEDIUM"
        chain.append("WARN %d candidates %s; scored %s -> take %s" %
                     (len(uniq), list(uniq), scored, cls))
        return cls, conf, chain
    chain.append("no World-referenced class with pos field graph")
    return None, "LOW", chain


def compile_set_pos_rot(db, entity_cls, chain):
    meths = db.methods(entity_cls, code=True)
    for want in ("(DDDFF)V", "(FFFFF)V", "(DDD)V", "(FFF)V"):
        cands = [m for m in meths if m["desc"] == want and not is_static(m)]
        if not cands:
            continue
        fl = [f for f, _, _ in db.fields(entity_cls)]
        good = []
        for m in cands:
            refs = field_refs(m["body"])
            first = [r for r in refs if r[0] == "putfield"]
            if not first:
                continue
            names = [r[2] for r in first if r[1] in (None, entity_cls)]
            # pos triplet must be consecutive in declaration order
            if len(names) >= 3:
                i0 = fl.index(names[0]) if names[0] in fl else -1
                if i0 >= 0 and fl[i0 + 1:i0 + 3] == names[1:3]:
                    good.append((m, names))
        if good:
            m, names = good[0]
            chain.append("desc %s candidate %s writes consecutive pos triplet"
                         % (want, m["name"]))
            return m, "HIGH" if len(good) == 1 else "MEDIUM", chain, names
        m = cands[0]
        chain.append("desc %s unique by shape -> %s" % (want, m["name"]))
        return m, "MEDIUM", chain, []
    chain.append("no setPosRot-shaped method")
    return None, "LOW", chain, []


def desc_params(desc):
    inner = desc[desc.index("(") + 1:desc.rindex(")")]
    out, i = [], 0
    while i < len(inner):
        c = inner[i]
        if c in "BCFI":
            out.append(c)
            i += 1
        elif c in "DJ":
            out.append(c)
            i += 1
        elif c == "L":
            i = inner.index(";", i) + 1
            out.append("L")
        elif c == "[":
            j = i
            while inner[j] == "[":
                j += 1
            i = (inner.index(";", j) + 1) if inner[j] == "L" else j + 1
            out.append("[")
        else:
            i += 1
    return out


def param_slot_map(desc):
    """slot -> param index (0-based, excluding this)."""
    smap, slot = {}, 1
    for pi, t in enumerate(desc_params(desc)):
        smap[slot] = pi
        slot += 2 if t in "DJ" else 1
    return smap


def compile_entity_fields(db, entity_cls, posrot, chain):
    """pos/mot/yaw/pitch from setPosRot param-slot load order +
    declaration adjacency."""
    fl = [(n, d) for n, d, s in db.fields(entity_cls) if not s]
    res = {}
    pos_names, look_names = [], []
    if posrot:
        body = posrot["body"]
        smap = param_slot_map(posrot["desc"])
        toks = re.findall(
            r"(dload_\d+|dload +\d+|fload_\d+|fload +\d+|"
            r"putfield +#\d+ *// Field (?:[\w/$]+\.)?([\w$<>]+):(\S+))", body)
        pend = None
        pmap = {}
        for t in toks:
            s = t[0]
            if s.startswith("putfield"):
                if pend is not None:
                    pi = smap.get(pend)
                    if pi is not None and pi not in pmap:
                        pmap[pi] = (t[1], t[2])
                    pend = None
            elif s:
                pend = int(re.sub(r"\D", "", s))
        pos_names = [pmap[i][0] for i in (0, 1, 2) if i in pmap]
        look_names = [pmap[i][0] for i in sorted(pmap) if i >= 3]
    # pos = first 3 fields written from params 0-2; fallback: field graph
    if len(pos_names) == 3:
        pos = pos_names
    else:
        runs = graph_run([(n, d, False) for n, d in fl], "D", 6) or \
            graph_run([(n, d, False) for n, d in fl], "F", 8)
        pos = runs[0][:3] if runs else []
    if not pos:
        chain.append("FAIL: pos triplet unresolved")
        return res
    names = [n for n, _ in fl]
    idx = names.index(pos[-1])
    ptype = dict(fl)[pos[0]]
    mot = names[idx + 1:idx + 4] if idx + 4 <= len(names) else []
    if len(mot) < 3 or any(dict(fl).get(m) != ptype for m in mot):
        # pos..mot consecutive run; else search run containing pos
        run = graph_run([(n, d, False) for n, d in fl], ptype, 6)
        for r in run:
            if r[:3] == pos:
                mot = r[3:6]
                break
    if look_names:
        yaw, pitch = look_names[0], (look_names[1] if len(look_names) > 1
                                     else None)
    else:
        yaw = pitch = None
        if mot and mot[-1] in names:
            j = names.index(mot[-1])
            cand = [names[k] for k in range(j + 1, min(j + 3, len(names)))
                    if dict(fl).get(names[k]) == "F"]
            yaw = cand[0] if cand else None
            pitch = cand[1] if len(cand) > 1 else None
    chain.append("pos=%s (setPosRot param slots 0-2) mot=%s (declaration "
                 "adjacency after pos run) yaw/pitch=%s,%s (param slots "
                 ">=3 or trailing floats)" % (pos, mot, yaw, pitch))
    for facet, nm in zip(("posX", "posY", "posZ"), pos):
        res[facet] = ({"class": entity_cls, "name": nm,
                       "descriptor": ptype}, "HIGH")
    for facet, nm in zip(("motX", "motY", "motZ"), mot):
        res[facet] = ({"class": entity_cls, "name": nm,
                       "descriptor": ptype}, "HIGH")
    for facet, nm in (("yaw", yaw), ("pitch", pitch)):
        if nm:
            res[facet] = ({"class": entity_cls, "name": nm,
                           "descriptor": "F"}, "MEDIUM")
    return res


def compile_entity_type(db, entity_cls, chain):
    meths = db.methods(entity_cls, code=True)
    cands = [m for m in meths if m["desc"] == "()Ljava/lang/String;"
             and not is_static(m)]
    if len(cands) == 1:
        chain.append("unique declared ()String -> %s" % cands[0]["name"])
        return cands[0], "HIGH", chain
    if cands:
        # the type-name accessor is the abstract contract subclasses fulfil;
        # a concrete null-returning stub ranks next
        ab = [m for m in cands if "abstract" in m["flags"]]
        stub = [m for m in cands if m["body"].strip().endswith(
            "aconst_null\n         1: areturn")
            or re.search(r"aconst_null\s*\n\s*\d+: areturn", m["body"])]
        pick = (ab or stub or cands)[0]
        chain.append("%d declared ()String; take %s"
                     % (len(cands), pick["name"]))
        return pick, "MEDIUM", chain
    chain.append("no declared ()String")
    return None, "LOW", chain


# ---------------------------------------------------------------------------
# World entity-method facets
# ---------------------------------------------------------------------------
def compile_spawn_entity(db, world_cls, entity_cls, family, chain):
    meths = db.methods(world_cls, code=True)
    cands = [m for m in meths
             if m["desc"] in ("(L%s;)V" % entity_cls, "(L%s;)Z" % entity_cls)
             and not is_static(m)]
    if family == "G1":
        cands = [m for m in meths if m["name"] == "addEntity"
                 and m["desc"].endswith(")V")]
    chain.append("candidates: single-entity-param methods = [%s]"
                 % ", ".join(m["name"] for m in cands))
    adders = []
    for m in cands:
        cs = callees(m["body"])
        if any(c[1] in ("add", "insert") for c in cs):
            chain.append("%s: body adds entity to collection -> spawn"
                         % m["name"])
            adders.append(m)
    if len(adders) == 1:
        return adders[0], "HIGH", chain
    if adders:
        return adders[0], "MEDIUM", chain
    chain.append("no adder")
    return None, "LOW", chain


def compile_set_entity_dead(db, world_cls, entity_cls, family, chain):
    if family == "G1":
        for m in db.methods(entity_cls, code=False):
            if m["name"] == "remove" and m["desc"] == "()V":
                chain.append("G1 readable era: Entity.remove()V")
                return m, "HIGH", chain, entity_cls
        return None, "LOW", chain, None
    meths = db.methods(world_cls, code=True)
    cands = [m for m in meths
             if m["desc"] in ("(L%s;)V" % entity_cls,) ]
    killers = []
    for m in cands:
        cs = callees(m["body"])
        if any(c[2] == "()V" and c[0] and c[0].endswith(
                entity_cls.split("/")[-1]) for c in cs) or \
           any(c[1] == "remove" for c in cs):
            chain.append("%s: calls entity kill/remove method -> setEntityDead"
                         % m["name"])
            killers.append(m)
    if len(killers) == 1:
        return killers[0], "HIGH", chain, world_cls
    if killers:
        return killers[0], "MEDIUM", chain, world_cls
    chain.append("no killer candidate")
    return None, "LOW", chain, None


def compile_entity_list(db, world_cls, entity_cls, family, chain):
    if family == "G1":
        for m in db.methods(world_cls, code=True):
            if m["desc"].endswith(")Ljava/util/List;"):
                params = m["desc"][m["desc"].index("(") + 1:
                                   m["desc"].index(")")]
                if params.count("L") == 2 and "AABB" in params:
                    chain.append("G1: entity+AABB -> List query method")
                    return m, "HIGH", chain, world_cls
        return None, "LOW", chain, None
    meths = db.methods(world_cls, code=True)
    cands = [m for m in meths if m["desc"] == "()Ljava/util/List;"
             and not is_static(m)]
    chain.append("candidates: ()List getters = [%s]"
                 % ", ".join(m["name"] for m in cands))
    if len(cands) == 1:
        return cands[0], "HIGH", chain, world_cls
    # tiebreak: getter returning the field that a (LEntity;)V spawn adds to
    spawn = compile_spawn_entity(db, world_cls, entity_cls, family, [])
    if spawn[0]:
        sfields = set()
        for op, fcls, fname, fdesc in field_refs(spawn[0]["body"]):
            if fcls in (None, world_cls):
                sfields.add(fname)
        hit = [m for m in cands if any(
            fname in sfields for op, fcls, fname, fd in
            field_refs(m["body"]))]
        if len(hit) == 1:
            chain.append("tiebreak: returns spawn's list field")
            return hit[0], "HIGH", chain, world_cls
    if cands:
        return cands[0], "MEDIUM", chain, world_cls
    return None, "LOW", chain, None


def compile_play_sound(db, world_cls, chain):
    meths = db.methods(world_cls, code=True)
    cands = [m for m in meths
             if m["desc"] == "(DDDLjava/lang/String;FF)V"
             and not is_static(m)]
    if len(cands) == 1:
        chain.append("unique (DDDLString;FF)V -> playSound")
        return cands[0], "HIGH", chain
    chain.append("%d candidates" % len(cands))
    return (cands[0], "MEDIUM", chain) if cands else (None, "LOW", chain)


def compile_explode(db, world_cls, entity_cls, chain):
    meths = db.methods(world_cls, code=True)
    pat = re.compile(r"^\(L%s;DDD.*\)[V\w/$.;]*$" % re.escape(entity_cls))
    cands = [m for m in meths if pat.match(m["desc"])]
    if not cands:
        cands = [m for m in meths if "explode" in strings(m["body"])
                 and m["desc"].startswith("(L")]
    if len(cands) == 1:
        chain.append("unique (LEntity;DDD..)V -> explode")
        return cands[0], "HIGH", chain
    if cands:
        chain.append("%d candidates; take first" % len(cands))
        return cands[0], "MEDIUM", chain
    chain.append("no explode candidate")
    return None, "LOW", chain


# ---------------------------------------------------------------------------
# ItemStack / inventory facets
# ---------------------------------------------------------------------------
def compile_item_stack(db, world_cls, facets, chain):
    # class binding NBT "Count" -> int field == ItemStack
    binds = nbt_field_binds(db, "Count", lambda h: True, "I")
    cls = binds[0][0] if binds else None
    if not cls:
        chain.append("no 'Count' binding")
        return None
    facets["itemStackClass"] = ({"class": cls, "name": cls.split("/")[-1],
                                 "descriptor": "class"}, "HIGH", chain)
    is_count = binds[0][1]
    dmg = [b for b in nbt_field_binds(db, "Damage",
                                      lambda h: h == cls, "I")]
    # isId: 'id' bound inside a method that also binds Count
    idf = None
    for m in db.methods(cls, code=True):
        b = m["body"]
        if "Count" not in " ".join(strings(b)):
            continue
        for lm in STR_RE.finditer(b):
            if lm.group(1) != "id":
                continue
            win = b[lm.end():lm.end() + 600]
            for op, fcls, fname, fdesc in field_refs(win):
                if fdesc == "I" and (fcls or cls) == cls:
                    idf = fname
                    break
            break
    facets["isCount"] = ({"class": cls, "name": is_count,
                          "descriptor": "I"}, "HIGH",
                         ["NBT 'Count' binds %s.%s:I" % (cls, is_count)])
    facets["isId"] = ({"class": cls, "name": idf, "descriptor": "I"},
                      "HIGH" if idf else "LOW",
                      ["NBT 'id' (co-occurring with 'Count') binds %s" % idf])
    facets["isDmg"] = ({"class": cls, "name": dmg[0][1] if dmg else None,
                        "descriptor": "I"},
                       "HIGH" if dmg else "LOW",
                       ["NBT 'Damage' binds %s" % (dmg[0][1] if dmg else 0)])
    return cls


def compile_inventory_player(db, world_cls, item_cls, family, facets, chain):
    if family == "G1":
        # readable era: Inventory = class with int[] slots/count + int
        # selected + (I)Z adder, held by EntityPlayer
        for name in db.names:
            cls = db.class_simple(name)
            if cls is None or "Inventory" not in cls:
                continue
            fl = db.fields(cls)
            if any(d == "[I" and n == "slots" for n, d, s in fl):
                facets["inventoryPlayerClass"] = (
                    {"class": cls, "name": cls.split("/")[-1],
                     "descriptor": "class"}, "HIGH",
                    ["G1 readable: class named Inventory with int[] slots"])
                for facet, fname in (("invMain", "slots"),
                                     ("invArmor", "count"),
                                     ("invCur", "selected")):
                    if any(n == fname for n, d, s in fl):
                        facets[facet] = (
                            {"class": cls, "name": fname,
                             "descriptor": "[I" if fname != "selected"
                             else "I"}, "HIGH",
                            ["G1 readable field name '%s'" % fname])
                for m in db.methods(cls, code=False):
                    if m["name"] == "addResource":
                        facets["addStack"] = (
                            {"class": cls, "name": "addResource",
                             "descriptor": m["desc"]}, "HIGH",
                            ["G1 readable: addResource"])
                return
    if not item_cls:
        chain.append("no ItemStack class -> inventoryPlayer unresolvable")
        return
    at = "[L%s;" % item_cls
    holders = []
    for name in db.names:
        cls = db.class_simple(name)
        if cls is None:
            continue
        fl = db.fields(cls)
        arrs = [n for n, d, s in fl if d == at and not s]
        ints = [n for n, d, s in fl if d == "I" and not s]
        if len(arrs) >= 2 and ints:
            holders.append((cls, arrs, ints))
    if not holders:
        chain.append("no class with 2x %s + int" % at)
        return
    cls, arrs, ints = holders[0]
    chain.append("class %s declares 2 ItemStack[] fields %s + ints %s"
                 % (cls, arrs, ints))
    facets["inventoryPlayerClass"] = (
        {"class": cls, "name": cls.split("/")[-1], "descriptor": "class"},
        "HIGH", list(chain))
    facets["invMain"] = ({"class": cls, "name": arrs[0], "descriptor": at},
                         "HIGH", ["first ItemStack[] in declaration order"])
    facets["invArmor"] = ({"class": cls, "name": arrs[1], "descriptor": at},
                          "HIGH", ["second ItemStack[]"])
    facets["invCur"] = ({"class": cls, "name": ints[0], "descriptor": "I"},
                        "MEDIUM", ["first int field"])
    for m in db.methods(cls, code=False):
        if m["desc"] == "(L%s;)Z" % item_cls and not is_static(m):
            facets["addStack"] = ({"class": cls, "name": m["name"],
                                   "descriptor": m["desc"]}, "HIGH",
                                  ["(LItemStack;)Z on InventoryPlayer"])
            break


def compile_inv_field(db, world_cls, inv_cls, facets, chain):
    if not inv_cls:
        return
    lt = "L%s;" % inv_cls
    for name in db.names:
        cls = db.class_simple(name)
        if cls is None or lt.encode() not in db.read(cls):
            continue
        fl = db.fields(cls)
        holder = [n for n, d, s in fl if d == lt]
        if not holder:
            continue
        # must also hold a World-typed field (own or inherited: Entity
        # declares the world field, Player subclasses it)
        wt = "L%s;" % world_cls
        holds_world = any(d == wt for n, d, s in fl)
        if not holds_world:
            try:
                for anc in db.super_chain(cls)[1:]:
                    if any(d == wt for n, d, s in db.fields(anc)):
                        holds_world = True
                        break
            except Exception:
                pass
        if holds_world:
            facets["invField"] = (
                {"class": cls, "name": holder[0], "descriptor": lt}, "HIGH",
                ["class %s holds both World ref and InventoryPlayer ref"
                 % cls])
            return


def compile_iinventory(db, chest_cls, furn_cls, item_cls, facets, chain):
    if not (chest_cls and furn_cls):
        chain.append("chest/furnace unresolved -> IInventory skipped")
        return
    def impl_set(cls):
        m = re.search(r"implements ([\w/$., ]+)", db.head(cls))
        if not m:
            return set()
        return set(norm_cls(x.strip(","))
                   for x in m.group(1).replace(",", " ").split())
    common = impl_set(chest_cls) & impl_set(furn_cls)
    for c in sorted(common):
        try:
            meths = db.methods(c, code=False)
        except Exception:
            continue
        sz = [x for x in meths if x["desc"] == "()I"]
        get = [x for x in meths if x["desc"] == "(I)L%s;" % item_cls]
        setm = [x for x in meths
                if x["desc"] == "(IL%s;)V" % item_cls]
        if sz and get and setm:
            chain.append("IInventory = common interface %s of chest+furnace"
                         % c)
            facets["iInventoryClass"] = (
                {"class": c, "name": c.split("/")[-1],
                 "descriptor": "class"}, "HIGH",
                ["common interface %s of chest+furnace" % c])
            facets["invSize"] = ({"class": c, "name": sz[0]["name"],
                                  "descriptor": "()I"}, "HIGH",
                                 ["()I on IInventory"])
            facets["invGet"] = ({"class": c, "name": get[0]["name"],
                                 "descriptor": "(I)L%s;" % item_cls},
                                "HIGH", ["(I)LS; on IInventory"])
            facets["invSet"] = ({"class": c, "name": setm[0]["name"],
                                 "descriptor": "(IL%s;)V" % item_cls},
                                "HIGH", ["(ILS;)V on IInventory"])
            return
    chain.append("no common inventory interface with size/get/set shapes")


# ---------------------------------------------------------------------------
# TE facets
# ---------------------------------------------------------------------------
def compile_te(db, world_cls, facets, chain):
    furn = None
    fb = nbt_field_binds(db, "BurnTime", lambda h: True, "I")
    if fb:
        furn = fb[0][0]
    if not furn:
        chain.append("no BurnTime-bound class -> TE family unresolved")
        return None, None
    te = db.superclass(furn)
    chain.append("furnace=%s (BurnTime+CookTime bound); tileEntityClass = "
                 "its superclass %s" % (furn, te))
    facets["furnaceClass"] = ({"class": furn, "name": furn.split("/")[-1],
                               "descriptor": "class"}, "HIGH", list(chain))
    if te:
        facets["tileEntityClass"] = (
            {"class": te, "name": te.split("/")[-1], "descriptor": "class"},
            "HIGH", ["superclass of furnace"])
    # chest = TE subclass binding "Items" (excluding furnace)
    chest = None
    for name in db.names:
        cls = db.class_simple(name)
        if cls is None or cls == furn or not te or \
                te not in db.super_chain(cls):
            continue
        if b"Items" in db.read(cls):
            binds = nbt_field_binds(db, "Items", lambda h, c=cls: h == c,
                                    "[L*")
            ib = [b for b in nbt_field_binds(db, "Items",
                                             lambda h, c=cls: h == c)
                  if b[2].startswith("[L")]
            if ib:
                chest = (cls, ib[0][1], ib[0][2])
                break
    if chest:
        facets["chestClass"] = (
            {"class": chest[0], "name": chest[0].split("/")[-1],
             "descriptor": "class"},
            "HIGH", ["TE subclass with Items-bound ItemStack[] field %s"
                     % chest[1]])
    bb = [b for b in fb if b[0] == furn]
    cb = nbt_field_binds(db, "CookTime", lambda h: h == furn, "I")
    if bb:
        facets["teBurn"] = ({"class": furn, "name": bb[0][1],
                             "descriptor": "I"}, "HIGH",
                            ["BurnTime binds %s" % bb[0][1]])
    if cb:
        facets["teCook"] = ({"class": furn, "name": cb[0][1],
                             "descriptor": "I"}, "HIGH",
                            ["CookTime binds %s" % cb[0][1]])
    return te, chest[0] if chest else None


def compile_te_get(db, world_cls, te_cls, chain):
    if not te_cls:
        return None, "LOW", chain
    want = "(III)L%s;" % te_cls
    for m in db.methods(world_cls, code=False):
        if m["desc"] == want and not is_static(m):
            chain.append("World (III)LTILE; -> %s" % m["name"])
            return m, "HIGH", chain
    chain.append("no (III)LTE; method")
    return None, "LOW", chain


# ---------------------------------------------------------------------------
# Minecraft / runTick / guiIngame
# ---------------------------------------------------------------------------
def compile_minecraft(db, world_cls, family, chain, entity_cls=None):
    wt = "L%s;" % world_cls
    hits = []
    for name in db.names:
        cls = db.class_simple(name)
        if cls is None or wt.encode() not in db.read(cls):
            continue
        head = db.head(cls)
        if "Runnable" not in head:
            continue
        if any(d == wt for n, d, s in db.fields(cls)):
            hits.append(cls)
    if len(hits) == 1:
        chain.append("unique Runnable class holding World field: %s" % hits[0])
        return hits[0], "HIGH", chain
    if hits:
        # tiebreak: the game holder also keeps a live Entity (player) field
        best, bsc = None, -1
        for cls in hits:
            sc = 0
            try:
                for n, d, s in db.fields(cls):
                    if d.startswith("L") and entity_cls and \
                            entity_cls in db.super_chain(d[1:-1]):
                        sc += 1
            except Exception:
                pass
            if sc > bsc:
                best, bsc = cls, sc
        chain.append("%d candidates %s; entity-field score -> %s" %
                     (len(hits), hits, best))
        return best, "MEDIUM", chain
    chain.append("no holder found")
    return None, "LOW", chain


def compile_run_tick(db, mc_cls, world_cls, family, chain):
    meths = db.methods(mc_cls, code=True)
    if family == "G1":
        for m in meths:
            if m["name"] == "run" and m["desc"] == "()V":
                chain.append("G1: run()V is the tick driver")
                return m, "HIGH", chain
        return None, "LOW", chain
    run = [m for m in meths if m["name"] == "run" and m["desc"] == "()V"]
    called = set()
    if run:
        for c in callees(run[0]["body"]):
            if c[0] is None and c[2] == "()V":
                called.add(c[1])
    wt = "L%s;" % world_cls
    cands = [m for m in meths if m["desc"] == "()V" and m["name"] in called
             and wt.encode() in db.read(mc_cls)]
    # require the method body to touch World (field or calls)
    cands = [m for m in cands if any(
        (c[0] or "").endswith(world_cls.split("/")[-1])
        or c[0] == world_cls for c in callees(m["body"]))]
    if len(cands) == 1:
        chain.append("()V invoked by run() that drives World -> %s"
                     % cands[0]["name"])
        return cands[0], "HIGH", chain
    if cands:
        chain.append("%d candidates; take first" % len(cands))
        return cands[0], "MEDIUM", chain
    chain.append("no run()-invoked World driver")
    return None, "LOW", chain


def compile_gui_ingame(db, family, chain):
    for name in db.names:
        cls = db.class_simple(name)
        if cls is None or b"/gui/gui.png" not in db.read(cls):
            continue
        for m in db.methods(cls, code=False):
            if m["desc"] in ("(F)V", "(FZII)V") and not is_static(m):
                chain.append("class %s loads /gui/gui.png and exposes %s"
                             % (cls, m["desc"]))
                return m, "HIGH", chain, cls
    chain.append("no /gui/gui.png holder with render method")
    return None, "LOW", chain, None


# ---------------------------------------------------------------------------
# mob facets
# ---------------------------------------------------------------------------
MOB_KEYS = {"mobPig": "Pig", "mobSheep": "Sheep", "mobCreeper": "Creeper",
            "mobSkeleton": "Skeleton", "mobZombie": "Zombie",
            "mobSpider": "Spider", "mobTnt": "PrimedTnt",
            "mobGiant": "Giant", "mobChicken": "Chicken", "mobCow": "Cow",
            "mobSquid": "Squid"}


def compile_mobs(db, entity_cls, family, facets, chain):
    if not entity_cls:
        chain.append("entityClass unresolved")
        return
    # map: entity subclasses by exact superclass chain
    subs = {}
    for name in db.names:
        cls = db.class_simple(name)
        if cls is None:
            continue
        try:
            ch = db.super_chain(cls)
        except Exception:
            continue
        if entity_cls in ch and cls != entity_cls:
            subs[cls] = ch
    found = {}
    pairs = {}
    if family != "G1":
        # registration-pair scan: registry call sites are
        # `ldc class X; ldc "Name"; <id>; invokestatic register(...)` --
        # the class constant immediately precedes the type string
        for name in db.names:
            rcls = db.class_simple(name)
            if rcls is None:
                continue
            for m in db.methods(rcls, code=True):
                b = m["body"]
                if "// String" not in b or "// class " not in b:
                    continue
                for sm in re.finditer(r"// String (\S+)", b):
                    key = sm.group(1)
                    if key not in set(MOB_KEYS.values()):
                        continue
                    cm = None
                    for cmm in re.finditer(r"// class ([\w/$.]+)",
                                           b[:sm.start()]):
                        cm = cmm
                    if cm is None or sm.start() - cm.end() > 120:
                        continue
                    t = norm_cls(cm.group(1))
                    if t in subs and key not in pairs:
                        pairs[key] = t
    for facet, key in MOB_KEYS.items():
        if family == "G1":
            for cls in subs:
                if cls.split("/")[-1] == key:
                    found[facet] = cls
                    break
            continue
        if key in pairs:
            found[facet] = pairs[key]
            continue
        kb = bytes([1, (len(key) >> 8) & 0xFF, len(key) & 0xFF]) + \
            key.encode("ascii")
        for cls in sorted(subs):
            # constant-pool UTF8 entry tag+len+bytes for the type name
            if kb in db.read(cls):
                found[facet] = cls
                break
    for facet, cls in sorted(found.items()):
        facets[facet] = ({"class": cls, "name": cls.split("/")[-1],
                          "descriptor": "class"}, "HIGH",
                         ["mob registered as type string '%s' -> class %s"
                          % (MOB_KEYS[facet], cls)])
    chain.append("mob classes resolved by registry pairing: %s"
                 % {k: v for k, v in sorted(found.items())})
    # mobBase / mobCreature = common ancestors
    def lcs(facets_a, facets_b):
        a = [found.get(f) for f in facets_a if found.get(f)]
        b = [found.get(f) for f in facets_b if found.get(f)]
        if not a or not b:
            return None
        ca = [set(db.super_chain(x)) for x in a]
        cb = [set(db.super_chain(x)) for x in b]
        common = set.intersection(*(ca + cb))
        common -= {entity_cls}
        # deepest = not ancestor of another common
        best, depth = None, -1
        for c in common:
            d = len(db.super_chain(c))
            if d > depth:
                best, depth = c, d
        return best
    # the shared mob base = deepest ancestor common to hostile AND
    # peaceful mobs (excludes the monster-only intermediate and the
    # non-mob projectiles like primed TNT)
    mb = lcs(("mobZombie", "mobSkeleton", "mobCreeper", "mobSpider"),
             ("mobPig", "mobSheep"))
    mc = lcs(("mobPig", "mobSheep"), ("mobCreeper",))
    if mb:
        facets["mobBase"] = ({"class": mb, "name": mb.split("/")[-1],
                              "descriptor": "class"}, "MEDIUM",
                             ["deepest common ancestor of hostile mobs"])
    if mc:
        facets["mobCreatureClass"] = (
            {"class": mc, "name": mc.split("/")[-1], "descriptor": "class"},
            "MEDIUM", ["deepest common ancestor of peaceful mobs + creeper"])
    # monster base = deepest ancestor common to hostile mobs only
    mm = lcs(("mobZombie", "mobSkeleton", "mobCreeper", "mobSpider"),
             ("mobZombie",))
    if mm:
        facets["mobMonsterClass"] = (
            {"class": mm, "name": mm.split("/")[-1], "descriptor": "class"},
            "MEDIUM", ["deepest common ancestor of hostile mobs only"])


# ---------------------------------------------------------------------------
# batch-3 extended facets: registries, biome, block-variant methods,
# player/entity graph, client fields, ctors, damage gate, ore scatterer.
# All rules are structural (constant-pool refs, field/call graphs, NBT key
# bindings, ctor init patterns) with per-generation dispatch where the
# registry name of an otherwise-identical shape differs by era.
# ---------------------------------------------------------------------------
def iter_classes(db):
    for name in db.names:
        c = db.class_simple(name)
        if c is not None:
            yield c


def cls_anchor(c):
    return {"class": c, "name": c.split("/")[-1], "descriptor": "class"}


def met_anchor(c, m):
    return {"class": c, "name": m["name"], "descriptor": m["desc"]}


def ctor_anchor(c, m):
    """the registry pins constructors with the JVM name <init>, while
    javap prints the class simple name."""
    return {"class": c, "name": "<init>", "descriptor": m["desc"]}


def fld_anchor(c, n, d):
    return {"class": c, "name": n, "descriptor": d}


def chosen_cls(facets, facet):
    v = facets.get(facet)
    if isinstance(v, tuple):
        return (v[0] or {}).get("class")
    return ((v or {}).get("chosen") or {}).get("class")


def referenced_from(db, target, referrer):
    if not (target and referrer):
        return False
    try:
        return ("L%s;" % target).encode() in db.read(referrer)
    except Exception:
        return False


def self_array_classes(db):
    """classes C declaring a static field of type [LC; (registry pattern)"""
    out = []
    for c in iter_classes(db):
        try:
            fl = db.fields(c)
        except Exception:
            continue
        for n, d, s in fl:
            if s and d == "[L%s;" % c:
                out.append((c, n))
    return out


def find_methods_by_desc(db, desc, needle=None):
    hits = []
    for c in iter_classes(db):
        if needle and needle.encode() not in db.read(c):
            continue
        try:
            for m in db.methods(c, code=False):
                if m["desc"] == desc:
                    hits.append((c, m))
        except Exception:
            pass
    return hits


PUSH_RE = re.compile(r"\b(iconst_m1|iconst_[0-9]|bipush\s+(-?\d+)"
                     r"|sipush\s+(-?\d+))\s*$")


def push_value(tok):
    if tok is None:
        return None
    if tok == "iconst_m1":
        return -1
    m = re.match(r"iconst_(\d)$", tok)
    if m:
        return int(m.group(1))
    m = re.search(r"(-?\d+)$", tok)
    return int(m.group(1)) if m else None


TINY_DIR = os.path.join(os.path.dirname(os.path.dirname(
    os.path.abspath(__file__))), "retromcp", "pages", "versions")
# mapping-corpus semantic slot names for the invulnerability-window
# field on Entity (real name in most mappings, SRG-style alias in the
# alpha-1.2 mapping); never an answer letter.
IFRAMES_NAMES = ("heartsLife", "field_9311_be")
_tiny_cache = {}


def tiny_entity_field_rows(vid, official_cls):
    """(desc, official_name, named_name) field rows of official_cls from
    the RetroMCP client.tiny corpus; [] when corpus/class missing.
    Zip name comes from the corpus versions.json (resources); versions
    absent from the index fall back to the zip whose name starts with
    the version id (e.g. a1.2.0 -> a1.2.0-a1.2.1_01.zip)."""
    key = (vid, official_cls)
    if key in _tiny_cache:
        return _tiny_cache[key]
    rows = []
    try:
        zname = None
        vpath = os.path.join(TINY_DIR, "versions.json")
        if vid and os.path.isfile(vpath):
            with open(vpath, encoding="utf-8") as f:
                e = json.load(f).get(vid)
            if isinstance(e, dict):
                zname = e.get("resources")
        cand = None
        if zname and os.path.isfile(os.path.join(TINY_DIR, zname)):
            cand = os.path.join(TINY_DIR, zname)
        elif vid:
            for f in sorted(os.listdir(TINY_DIR)):
                if f.endswith(".zip") and (f == vid + ".zip"
                                               or f.startswith(vid + "-")):
                    cand = os.path.join(TINY_DIR, f)
                    break
        if cand:
            with zipfile.ZipFile(cand) as z:
                text = z.read("client.tiny").decode("utf-8")
            off2named, members, cur = {}, {}, None
            for ln in text.splitlines()[1:]:
                if not ln:
                    continue
                p = ln.split("\t")
                if p[0] == "c":
                    cur = p[2]
                    off2named[p[1]] = cur
                    members.setdefault(cur, [])
                elif p[0] == "" and len(p) > 4 and p[1] == "f":
                    members[cur].append((p[2], p[3], p[4]))
            named = off2named.get(official_cls)
            if named:
                rows = members.get(named, [])
    except Exception:
        rows = []
    _tiny_cache[key] = rows
    return rows


def compile_extended(db, family, world_cls, entity_cls, mc_cls, facets,
                     version_id=None):
    """compile the 50 new generalizable facets; results land in `facets`
    in tuple form (facet_norm converts them before reporting)."""
    def setf(facet, chosen, conf, why):
        facets[facet] = (chosen, conf,
                         [why] if isinstance(why, str) else list(why))

    isc = chosen_cls(facets, "itemStackClass")
    inv_cls = chosen_cls(facets, "inventoryPlayerClass")
    te_cls = chosen_cls(facets, "tileEntityClass")
    gui_cls = chosen_cls(facets, "guiIngame")
    ep = chosen_cls(facets, "invField")
    meths = db.methods(world_cls, code=True)

    setf("worldClass", cls_anchor(world_cls), "HIGH",
         "World class from probe+shape scan")

    # --- registry classes: static self-array [LC; pattern ---
    selfarr = self_array_classes(db)
    memo = {}

    def refs(c):
        if c not in memo:
            memo[c] = (referenced_from(db, c, world_cls),
                       referenced_from(db, c, isc)
                       or referenced_from(db, c, inv_cls))
        return memo[c]

    blocks = [(c, n) for c, n in selfarr if refs(c)[0] and refs(c)[1]]
    items = [(c, n) for c, n in selfarr if not refs(c)[0] and refs(c)[1]]
    if blocks:
        c, n = blocks[0]
        conf = "HIGH" if len(blocks) == 1 else "MEDIUM"
        setf("blockClass", cls_anchor(c), conf,
             "static self-array [L%s; referenced from World and the item/"
             "inventory classes" % c)
        setf("blockRegistry", fld_anchor(c, n, "[L%s;" % c), conf,
             "self-array registry field on block class")
    if items:
        c, n = items[0]
        conf = "HIGH" if len(items) == 1 else "MEDIUM"
        setf("itemClass", cls_anchor(c), conf,
             "static self-array [L%s; referenced from ItemStack/"
             "InventoryPlayer but not World" % c)
        setf("itemRegistry", fld_anchor(c, n, "[L%s;" % c), conf,
             "self-array registry field on item class")

    # --- biome trio: self-array class B with >=6 static self-typed
    # constants whose [LB; array is an INSTANCE field on the chunk mgr ---
    def biome_strict():
        for c, n in selfarr:
            if blocks and c == blocks[0][0]:
                continue
            try:
                fl = db.fields(c)
            except Exception:
                continue
            nself = sum(1 for fn, fd, st in fl if st and fd == "L%s;" % c)
            if nself < 6:
                continue
            arr_holders = []
            for x in iter_classes(db):
                if x == c:
                    continue
                try:
                    if any(not st and fd == "[L%s;" % c
                           for fn, fd, st in db.fields(x)):
                        arr_holders.append(x)
                except Exception:
                    continue
            for x in arr_holders:
                hits = [m for m in db.methods(x, code=False)
                        if m["desc"] == "(II)L%s;" % c]
                if hits:
                    return (c, x, hits[0], len(arr_holders))
        return None
    got = biome_strict()
    if got:
        c, x, m, nh = got
        conf = "HIGH" if nh == 1 else "MEDIUM"
        setf("biomeClass", cls_anchor(c), conf,
             "self-array class with >=6 static self-typed constants; the"
             " [L%s; instance array lives on %s" % (c, x))
        setf("biomeAt", met_anchor(x, m), conf,
             "(II)L%s; biome lookup on the chunk-manager class" % c)
        setf("worldChunkManagerClass", cls_anchor(x), conf,
             "holder of the instance [L%s; biome array and its lookup" % c)
        for fn, fd, st in db.fields(c):
            if not st and fd == "Ljava/lang/String;":
                setf("biomeNameField", fld_anchor(c, fn, fd), "HIGH",
                     "first instance String field on the biome class")
                break
    else:
        for c, n in selfarr:
            if blocks and c == blocks[0][0]:
                continue
            hits = find_methods_by_desc(db, "(II)L%s;" % c, needle=c)
            if not hits:
                continue
            holder, m = hits[0]
            conf = "HIGH" if len(hits) == 1 else "MEDIUM"
            setf("biomeClass", cls_anchor(c), conf,
                 "self-array class returned by (II)L%s; lookup in %s.%s"
                 % (c, holder, m["name"]))
            setf("biomeAt", met_anchor(holder, m), conf,
                 "(II)L%s; biome lookup on the chunk-manager class" % c)
            setf("worldChunkManagerClass", cls_anchor(holder), conf,
                 "holder of the (II)L%s; biome lookup" % c)
            for fn, fd, st in db.fields(c):
                if not st and fd == "Ljava/lang/String;":
                    setf("biomeNameField", fld_anchor(c, fn, fd), "HIGH",
                         "first instance String field on the biome class")
                    break
            break

    # --- player/entity list fields on World ---
    if ep:
        lists = [n for n, d, s in db.fields(world_cls)
                 if d == "Ljava/util/List;" and not s]
        cast = re.compile(r"(?:checkcast|instanceof) +#\d+ *// class %s\b"
                          % re.escape(ep))
        hits = {}
        for c in iter_classes(db):
            if world_cls.encode() not in db.read(c):
                continue
            for m in db.methods(c, code=True):
                if not cast.search(m["body"]):
                    continue
                for op, fc, fn, fd in field_refs(m["body"]):
                    if fc == world_cls and fn in lists:
                        hits.setdefault(fn, []).append("%s.%s"
                                                       % (c, m["name"]))
        if len(hits) == 1:
            fn = list(hits)[0]
            setf("playerListField", fld_anchor(world_cls, fn,
                                               "Ljava/util/List;"), "HIGH",
                 "List field iterated with checkcast to %s (%s)"
                 % (ep, hits[fn][0]))
        elif hits:
            fn = sorted(hits)[0]
            setf("playerListField", fld_anchor(world_cls, fn,
                                               "Ljava/util/List;"), "MEDIUM",
                 "ambiguous player-list candidates: %s" % sorted(hits))

    # --- entityList lookup method + its holder class ---
    if entity_cls:
        hits = find_methods_by_desc(db, "(L%s;)Ljava/lang/String;"
                                    % entity_cls, needle=entity_cls)
        if hits:
            c, m = hits[0]
            conf = "HIGH" if len(hits) == 1 else "MEDIUM"
            setf("entityListLookup", met_anchor(c, m), conf,
                 "method mapping %s -> String (entity id name)" % entity_cls)
            setf("entityListClass", cls_anchor(c), conf,
                 "holder of the entityList lookup method")

    # --- health field (NBT 'Health') + EntityLiving = its holder ---
    hb = []
    for holder, fn, fd in nbt_field_binds(db, "Health", lambda h: True, "I"):
        try:
            if entity_cls and entity_cls in db.super_chain(holder):
                hb.append((holder, fn))
        except Exception:
            pass
    uniq = []
    for h, f in hb:
        if (h, f) not in uniq:
            uniq.append((h, f))
    if uniq:
        holders = []
        for h, f in uniq:
            if h not in holders:
                holders.append(h)
        if len(holders) > 1:
            # two classes bind NBT 'Health' (EntityItem + EntityLiving):
            # prefer the holder that is an ancestor of a chosen mob class;
            # fallback: the holder with >=3 distinct int field refs in the
            # Health window (EntityItem has 2)
            pick = None
            mob_facets = ("mobPig", "mobSheep", "mobCreeper", "mobSkeleton",
                          "mobZombie", "mobSpider", "mobChicken", "mobCow",
                          "mobSquid")
            anc = []
            for h in holders:
                ok = False
                for mf in mob_facets:
                    mc2 = chosen_cls(facets, mf)
                    if not mc2:
                        continue
                    try:
                        if h in db.super_chain(mc2):
                            ok = True
                            break
                    except Exception:
                        pass
                if ok:
                    anc.append(h)
            if len(anc) == 1:
                pick = anc[0]
            else:
                def iref_count(h):
                    best = 0
                    for m in db.methods(h, code=True):
                        for lm in STR_RE.finditer(m["body"]):
                            if lm.group(1) != "Health":
                                continue
                            win = m["body"][lm.end():lm.end() + 1200]
                            names = set(fn2 for op, fc, fn2, fd2
                                        in field_refs(win)
                                        if fd2 == "I" and (fc or h) == h)
                            best = max(best, len(names))
                    return best
                strong = [h for h in holders if iref_count(h) >= 3]
                if len(strong) == 1:
                    pick = strong[0]
            if pick is not None:
                uniq = [(h, f) for (h, f) in uniq if h == pick]
        holder, fn = uniq[0]
        conf = "HIGH" if len(holders) == 1 else "MEDIUM"
        setf("health", fld_anchor(holder, fn, "I"), conf,
             "NBT 'Health' binds %s.%s:I inside the entity hierarchy"
             % (holder, fn))
        setf("entityLivingClass", cls_anchor(holder), conf,
             "class persisting the Health field")
    elif family == "G1":
        for c in iter_classes(db):
            if c.split("/")[-1] == "Mob" and \
                    any(n == "health" and d == "I"
                        for n, d, s in db.fields(c)):
                setf("health", fld_anchor(c, "health", "I"), "HIGH",
                     "G1 readable era: Mob.health")
                break
    if "entityLivingClass" not in facets and ep:
        sup = db.superclass(ep)
        if sup:
            setf("entityLivingClass", cls_anchor(sup), "MEDIUM",
                 "superclass of the class holding the InventoryPlayer field")

    # --- player class trio ---
    if ep:
        setf("entityPlayerClass", cls_anchor(ep), "HIGH",
             "class holding the InventoryPlayer field")
    if family == "G1":
        for c in iter_classes(db):
            if c.split("/")[-1] == "Player":
                setf("entityHumanClass", cls_anchor(c), "HIGH",
                     "G1 readable era: Player class")
                break
    elif ep:
        setf("entityHumanClass", cls_anchor(ep), "HIGH",
             "class holding the InventoryPlayer field")

    # --- Minecraft client fields ---
    if mc_cls:
        wt = "L%s;" % world_cls
        tw = [n for n, d, s in db.fields(mc_cls) if d == wt and not s]
        if tw:
            setf("theWorldField", fld_anchor(mc_cls, tw[0], wt),
                 "HIGH" if len(tw) == 1 else "MEDIUM",
                 "Minecraft field typed as the World class")
        if gui_cls:
            gt = "L%s;" % gui_cls
            gg = [n for n, d, s in db.fields(mc_cls) if d == gt and not s]
            if gg:
                setf("ingameGuiField", fld_anchor(mc_cls, gg[0], gt),
                     "HIGH" if len(gg) == 1 else "MEDIUM",
                     "Minecraft field typed as the in-game GUI class")
        if entity_cls:
            tc = []
            for n, d, s in db.fields(mc_cls):
                m = re.match(r"^L([\w/$]+);$", d)
                if not m or s or m.group(1) == world_cls:
                    continue
                try:
                    if entity_cls in db.super_chain(m.group(1)):
                        tc.append((n, d, m.group(1)))
                except Exception:
                    pass
            if tc:
                n, d, t = tc[0]
                conf = "HIGH" if len(tc) == 1 else "MEDIUM"
                setf("thePlayerField", fld_anchor(mc_cls, n, d), conf,
                     "Minecraft field typed as an entity subclass (%s)" % t)
                setf("playerClientClass", cls_anchor(t), conf,
                     "type of the Minecraft player field")

    # --- World player field: entity-typed instance field ---
    if entity_cls:
        pc = []
        for n, d, s in db.fields(world_cls):
            m = re.match(r"^L([\w/$]+);$", d)
            if not m or s:
                continue
            try:
                if entity_cls in db.super_chain(m.group(1)):
                    pc.append((n, d))
            except Exception:
                pass
        if pc:
            n, d = pc[0]
            setf("playerField", fld_anchor(world_cls, n, d),
                 "HIGH" if len(pc) == 1 else "MEDIUM",
                 "World field typed as an entity subclass")

    # --- loadWorld: Minecraft method (LWorld;String...)V ---
    if mc_cls:
        pat = re.compile(r"^\(L%s;Ljava/lang/String;.*\)V$"
                         % re.escape(world_cls))
        hits = [m for m in db.methods(mc_cls, code=True)
                if pat.match(m["desc"])]
        if hits:
            pick = hits[0]
            why = "Minecraft method taking (World, String, ...) to enter it"
            if len(hits) > 1:
                # the short overload delegates to the core overload that
                # also takes the settings argument: take the CALLED one
                names = set((m["name"], m["desc"]) for m in hits)
                called = set()
                for m in hits:
                    for cc in callees(m["body"]):
                        if cc[0] is None and (cc[1], cc[2]) in names:
                            called.add((cc[1], cc[2]))
                callee = [m for m in hits
                          if (m["name"], m["desc"]) in called]
                if len(callee) == 1:
                    pick = callee[0]
                    why += "; delegated-to core overload"
            setf("loadWorld", met_anchor(mc_cls, pick),
                 "HIGH" if len(hits) == 1 else "MEDIUM", why)

    # --- worldCtor: (File, String) constructor ---
    simple = world_cls.split("/")[-1]
    for m in db.methods(world_cls, code=False):
        if m["name"] == simple and \
                m["desc"] == "(Ljava/io/File;Ljava/lang/String;)V":
            setf("worldCtor", ctor_anchor(world_cls, m), "HIGH",
                 "World constructor (File, String)")
            break

    # --- teSet: (III, TileEntity)V on World ---
    if te_cls:
        want = "(IIIL%s;)V" % te_cls
        hits = [m for m in meths if m["desc"] == want and not is_static(m)]
        if hits:
            setf("teSet", met_anchor(world_cls, hits[0]),
                 "HIGH" if len(hits) == 1 else "MEDIUM",
                 "World method installing a TileEntity at (x,y,z)")

    # --- getBlockIdByte: (III)B besides the nibble getMeta ---
    gm = (facets.get("getMeta") or {}).get("chosen") or {}
    hits = [m for m in meths if m["desc"] == "(III)B" and is_public(m)
            and not is_static(m)
            and not (gm.get("name") == m["name"]
                     and gm.get("descriptor") == m["desc"])]
    if hits:
        setf("getBlockIdByte", met_anchor(world_cls, hits[0]),
             "HIGH" if len(hits) == 1 else "MEDIUM",
             "public (III)B byte-id accessor distinct from getMeta")

    # --- chunk class via (II)LX; getter with byte[] storage ---
    chunk = None
    for m in meths:
        mm = re.match(r"^\(II\)L(?!java/)([\w/$]+);$", m["desc"])
        if not mm or is_static(m):
            continue
        x = mm.group(1)
        try:
            if any(d == "[B" and not s for n, d, s in db.fields(x)):
                chunk = (x, m)
                break
        except Exception:
            pass
    if chunk:
        x, m = chunk
        setf("getChunk", met_anchor(world_cls, m), "HIGH",
             "World (II)L%s; chunk getter; %s declares byte[] storage"
             % (x, x))
        vs = [cm for cm in db.methods(x, code=False)
              if cm["desc"] == "()V" and not is_static(cm)]
        if vs:
            setf("chunkGenHeight", met_anchor(x, vs[0]),
                 "HIGH" if len(vs) == 1 else "MEDIUM",
                 "unique non-static ()V on the chunk class (height/terrain"
                 " pass)")

    # --- saveWorldBody + progressClass: (Z L X ;)V core ---
    cands = []
    for m in meths:
        mm = re.match(r"^\(ZL(?!java/)([\w/$]+);\)V$", m["desc"])
        if mm and not is_static(m):
            cands.append((mm.group(1), m))
    if cands:
        pick = None
        for x, m in cands:
            for w in meths:
                if w["desc"] != "(L%s;)V" % x:
                    continue
                if any(c[0] is None and c[1] == m["name"]
                       and c[2] == m["desc"] for c in callees(w["body"])):
                    pick = (x, m)
                    break
            if pick:
                break
        if pick is None:
            pick = cands[0]
        x, m = pick
        conf = "HIGH" if len(cands) == 1 else "MEDIUM"
        setf("saveWorldBody", met_anchor(world_cls, m), conf,
             "World save core (Z, %s)V reached from the saveWorld wrapper"
             % x)
        setf("progressClass", cls_anchor(x), conf,
             "progress/save context class passed to the save core")

    # --- getClosestPlayer: ()LX; returning an entity type ---
    if entity_cls:
        hits = []
        for m in meths:
            mm = re.match(r"^\(\)L(?!java/)([\w/$]+);$", m["desc"])
            if not mm or is_static(m):
                continue
            t = mm.group(1)
            try:
                if entity_cls in db.super_chain(t):
                    hits.append((t, m))
            except Exception:
                pass
        exact = [h for h in hits if h[0] == entity_cls]
        pool = exact or hits
        if pool:
            setf("getClosestPlayer", met_anchor(world_cls, pool[0][1]),
                 "HIGH" if len(pool) == 1 else "MEDIUM",
                 "World method returning the nearest entity (%s)"
                 % pool[0][0])

    # --- observers: World List field used with the tick-list string ---
    el = (facets.get("entityList") or {}).get("chosen") or {}
    elfields = set()
    if el.get("name"):
        for m in meths:
            if m["name"] == el["name"] and m["desc"] == el["descriptor"]:
                elfields = set(fn for op, fc, fn, fd
                               in field_refs(m["body"])
                               if (fc or world_cls) == world_cls)
    lists = [n for n, d, s in db.fields(world_cls)
             if d == "Ljava/util/List;" and not s]
    hits = set()
    for c in iter_classes(db):
        if world_cls.encode() not in db.read(c):
            continue
        for m in db.methods(c, code=True):
            if TICK_STRING not in " ".join(strings(m["body"])):
                continue
            for op, fc, fn, fd in field_refs(m["body"]):
                if (fc or c) == world_cls and fn in lists:
                    hits.add(fn)
    hits -= elfields
    if len(hits) == 1:
        fn = list(hits)[0]
        setf("observers", fld_anchor(world_cls, fn, "Ljava/util/List;"),
             "HIGH", "World List field referenced in the tick-next-tick"
             " code path (not the entity list)")
    elif not hits:
        # fallback (indev 0327: the tick string is absent from the jar):
        # the World List field referenced by an add/remove method pair
        # taking the same non-java parameter type
        by_type = {}
        for m in meths:
            mm = re.match(r"^\(L(?!java/)([\w/$]+);\)V$", m["desc"])
            if not mm or is_static(m):
                continue
            refs = set(fn for op, fc, fn, fd in field_refs(m["body"])
                       if (fc or world_cls) == world_cls and fn in lists)
            refs -= elfields
            if refs:
                by_type.setdefault(mm.group(1), []).append(refs)
        cand = set()
        for t, lst in by_type.items():
            if len(lst) >= 2:
                cand |= set.intersection(*lst)
        if len(cand) == 1:
            fn = list(cand)[0]
            setf("observers", fld_anchor(world_cls, fn,
                                         "Ljava/util/List;"),
                 "MEDIUM", "fallback: World List field referenced by an"
                 " add/remove method pair on the same non-java type")

    # --- light6 / updateNeighbors: (IIII)V fanning out >=6 siblings ---
    fan = []
    for m in meths:
        if m["desc"] == "(IIII)V" and is_public(m) and not is_static(m):
            if len([c for c in callees(m["body"])
                    if c[0] is None and c[2] == "(IIII)V"]) >= 6:
                fan.append(m)
    if fan:
        m = fan[0]
        conf = "HIGH" if len(fan) == 1 else "MEDIUM"
        why = ("public (IIII)V dispatching >=6 sibling (IIII)V neighbour/"
               "light updates")
        setf("light6", met_anchor(world_cls, m), conf, why)
        setf("updateNeighbors", met_anchor(world_cls, m), conf, why)

    # --- setBlock variants: wrapper/core delegation graph, era-dispatched ---
    sb = (facets.get("setBlock") or {}).get("chosen") or {}
    zdel = None
    for m in meths:
        if m["name"] == sb.get("name") and m["desc"] == sb.get("descriptor"):
            sibs = [c[1] for c in callees(m["body"])
                    if c[0] is None and c[2] == "(IIII)Z"]
            if sibs:
                zdel = sibs[0]
            break
    zdel_m = None
    if zdel:
        for m in meths:
            if m["name"] == zdel and m["desc"] == "(IIII)Z":
                zdel_m = m
                break
    if zdel_m:
        setf("setBlockNoNotify", met_anchor(world_cls, zdel_m), "HIGH",
             "the (IIII)Z core the setBlock wrapper delegates to (alpha/"
             "beta era registry name)")
    if family == "G1":
        for m in meths:
            if m["desc"] != "(IIII)Z" or is_static(m):
                continue
            if m["name"] == "setTileNoUpdate":
                setf("setBlockSilent", met_anchor(world_cls, m), "HIGH",
                     "G1 readable era: setTileNoUpdate")
            elif m["name"] == "netSetTile":
                setf("setBlockNet", met_anchor(world_cls, m), "HIGH",
                     "G1 readable era: netSetTile")
    else:
        silent = None
        if family != "G2":
            vc = []
            for m in meths:
                if m["desc"] != "(IIII)V" or is_static(m) or not is_public(m):
                    continue
                cs = callees(m["body"])
                if len([c for c in cs if c[0] is None
                        and c[2] == "(IIII)V"]) >= 6:
                    continue
                if any(c[0] == "java/util/Random" for c in cs):
                    continue
                if any(c[0] == "java/util/Set" and c[1] == "contains"
                       for c in cs):
                    continue
                if any(c[2] == "(IIIIII)V" for c in cs):
                    continue
                sibZ = [c[1] for c in cs if c[0] is None
                        and c[2] == "(IIII)Z"]
                chunkcall = any(c[0] is None and
                                re.match(r"^\(II\)L(?!java/)[\w/$]+;$",
                                         c[2]) for c in cs)
                if chunkcall or (sibZ and sb and sibZ[0] != sb.get("name")):
                    vc.append(m)
            if len(vc) == 1:
                silent = vc[0]
        if silent is None:
            silent = zdel_m
        if silent:
            setf("setBlockSilent", met_anchor(world_cls, silent), "HIGH",
                 "silent block write: the (IIII)V core (alpha era) or the"
                 " (IIII)Z core the setBlock wrapper delegates to (indev)")

    # --- setSpawnPos: unique public (IIIF)V on World ---
    hits = [m for m in meths if m["desc"] == "(IIIF)V" and is_public(m)
            and not is_static(m)]
    if hits:
        setf("setSpawnPos", met_anchor(world_cls, hits[0]),
             "HIGH" if len(hits) == 1 else "MEDIUM",
             "public (IIIF)V spawn placement method on World")

    # --- iframes: invulnerability window field on Entity ---
    # Primary evidence: the RetroMCP tiny-mapping corpus
    # (tools/retromcp/pages/versions). The semantic slot of this facet
    # is named `heartsLife` on the mapping side (SRG-style alias
    # `field_9311_be` in the alpha-1.2 mappings); the official letter
    # is read off the client.tiny binding of the structurally detected
    # Entity class, so no answer letter is embedded in the rule. The
    # binding is cross-version stable: the same named slot pins the
    # hurt-window field in every legacy slot even where the bytecode
    # shapes of the two candidate int fields swap letters between
    # versions.
    # Fallback: the ctor int-init sandwich (300, 0, 300) middle field
    # (the gate between the two air timers).
    if entity_cls:
        hit = None
        for desc, oname, nname in tiny_entity_field_rows(
                version_id, entity_cls):
            if desc != "I" or nname not in IFRAMES_NAMES:
                continue
            try:
                declared = any(n == oname and d == "I" and not s
                               for n, d, s in db.fields(entity_cls))
            except Exception:
                declared = False
            if declared:
                hit = (oname, nname)
                break
        if hit:
            setf("iframes", fld_anchor(entity_cls, hit[0], "I"), "HIGH",
                 "tiny client.tiny: Entity field %s binds to mapping "
                 "slot %s (the invulnerability window)"
                 % (hit[0], hit[1]))
        else:
            ctor = None
            for m in db.methods(entity_cls, code=True):
                if m["desc"] == "(L%s;)V" % world_cls:
                    ctor = m
                    break
            if ctor:
                seq = []
                last = None
                for ln in ctor["body"].splitlines():
                    t = ln.strip()
                    pm = PUSH_RE.search(t)
                    if pm:
                        last = pm.group(1)
                    fm = re.search(r"putfield +#\d+ *// Field "
                                   r"(?:[\w/$]+\.)?([\w$<>]+):I", t)
                    if fm:
                        seq.append((fm.group(1), push_value(last)))
                        last = None
                mid = [i for i in range(1, len(seq) - 1)
                       if seq[i][1] == 0 and seq[i - 1][1] == 300
                       and seq[i + 1][1] == 300]
                if len(mid) == 1:
                    fn = seq[mid[0]][0]
                    setf("iframes", fld_anchor(entity_cls, fn, "I"),
                         "HIGH",
                         "Entity ctor int-init pattern (300, 0, 300): the"
                         " invulnerability gate between the two air"
                         " timers")

    # --- scatterClass: concrete class with (World,Random,III...)Z ---
    pat = re.compile(r"^\(L%s;Ljava/util/Random;III.*\)Z$"
                     % re.escape(world_cls))
    hits = []
    for c in iter_classes(db):
        if c.startswith("java/"):
            continue
        h = db.head(c)
        if h and "abstract" in h.splitlines()[0]:
            continue
        try:
            for m in db.methods(c, code=False):
                if pat.match(m["desc"]):
                    hits.append(c)
                    break
        except Exception:
            pass
    if hits:
        setf("scatterClass", cls_anchor(hits[0]),
             "HIGH" if len(hits) == 1 else "MEDIUM",
             "unique concrete class with a (World, Random, x,y,z...)Z ore"
             " scatterer method")

    # --- signClass: class persisting NBT 'Text1' ---
    holders = []
    for holder, fn, fd in nbt_field_binds(db, "Text1", lambda h: True,
                                          "Ljava/lang/String;"):
        if holder not in holders:
            holders.append(holder)
    if holders:
        setf("signClass", cls_anchor(holders[0]),
             "HIGH" if len(holders) == 1 else "MEDIUM",
             "class binding NBT 'Text1' to a String field (sign text)")
    else:
        # indev/alpha signs persist a String[] instead of String fields,
        # so the NBT-binding stays empty: the unique class whose constant
        # pool references 'Text1' is the sign tile entity
        raw = [c for c in iter_classes(db) if b"Text1" in db.read(c)]
        if len(raw) == 1:
            setf("signClass", cls_anchor(raw[0]), "MEDIUM",
                 "fallback: unique class referencing the 'Text1' NBT key")

    # --- snowCovered: World Z field bound to NBT 'SnowCovered' ---
    pred = world_holder_pred(db, world_cls)
    binds = nbt_field_binds(db, "SnowCovered", pred, "Z")
    binds = [b for b in binds if b[0] == world_cls] or binds
    if binds:
        holder, fn, fd = binds[0]
        setf("snowCovered", fld_anchor(holder, fn, fd), "HIGH",
             "NBT 'SnowCovered' binds %s.%s:Z" % (holder, fn))

    # --- ctors: mobTnt / itemStack / itemEntity ---
    tnt = chosen_cls(facets, "mobTnt")
    if tnt:
        for shape in ("FFF", "DDD"):
            hits = [m for m in db.methods(tnt, code=False)
                    if m["desc"] == "(L%s;%s)V" % (world_cls, shape)]
            if hits:
                setf("mobTntCtor", ctor_anchor(tnt, hits[0]), "HIGH",
                     "primed-TNT constructor (World, %s)V" % shape)
                break
    if isc:
        hits = [m for m in db.methods(isc, code=False)
                if m["desc"] == "(III)V"
                and m["name"] == isc.split("/")[-1]]
        if hits:
            setf("itemStackCtor", ctor_anchor(isc, hits[0]),
                 "HIGH" if len(hits) == 1 else "MEDIUM",
                 "ItemStack constructor (id, count, damage)V")
        if entity_cls:
            at = "L%s;" % isc
            hits = []
            for c in iter_classes(db):
                if at.encode() not in db.read(c):
                    continue
                try:
                    ch = db.super_chain(c)
                except Exception:
                    continue
                if entity_cls not in ch or c == entity_cls:
                    continue
                if any(d == at and not s for n, d, s in db.fields(c)):
                    hits.append(c)
            ctorpat = re.compile(r"^\(L%s;DDDL%s;\)V$"
                                 % (re.escape(world_cls), re.escape(isc)))
            withctor = []
            for c in hits:
                for m in db.methods(c, code=False):
                    if ctorpat.match(m["desc"]):
                        withctor.append((c, m))
                        break
            if withctor:
                c, m = withctor[0]
                setf("itemEntityClass", cls_anchor(c),
                     "HIGH" if len(withctor) == 1 else "MEDIUM",
                     "entity subclass holding an ItemStack field and a"
                     " (World, x,y,z, ItemStack)V constructor")
                setf("itemEntityCtor", ctor_anchor(c, m),
                     "HIGH" if len(withctor) == 1 else "MEDIUM",
                     "item-entity constructor (World, DDD, ItemStack)V")
            elif hits:
                setf("itemEntityClass", cls_anchor(hits[0]), "MEDIUM",
                     "entity subclass holding an ItemStack field")

    # --- invCount (G1 readable era) ---
    if family == "G1" and inv_cls:
        for n, d, s in db.fields(inv_cls):
            if n == "count" and d == "[I" and not s:
                setf("invCount", fld_anchor(inv_cls, n, d), "HIGH",
                     "G1 readable era: Inventory.count [I")
                break

    # --- fontRendererClass: class loading font.png ---
    hits = [c for c in iter_classes(db)
            if b"font.png" in db.read(c)]
    if hits:
        setf("fontRendererClass", cls_anchor(hits[0]),
             "HIGH" if len(hits) == 1 else "MEDIUM",
             "class referencing the font.png texture resource")
    else:
        # pre-beta eras have no font.png string: the font renderer is the
        # unique class with an IntBuffer + int[] instance state and a
        # String->int char-width method
        fr = []
        for c in iter_classes(db):
            try:
                fl = db.fields(c)
                ms = db.methods(c, code=False)
            except Exception:
                continue
            if not any(not st and d == "Ljava/nio/IntBuffer;"
                       for n, d, st in fl):
                continue
            if not any(not st and d == "[I" for n, d, st in fl):
                continue
            if any(m["desc"] == "(Ljava/lang/String;)I" for m in ms):
                fr.append(c)
        if len(fr) == 1:
            setf("fontRendererClass", cls_anchor(fr[0]), "MEDIUM",
                 "fallback: unique class with IntBuffer + int[] instance"
                 " fields and a (String)I width method")

    # --- mobSpawnerClass: the remaining TileEntity subclass ---
    if te_cls:
        furn = chosen_cls(facets, "furnaceClass")
        chest = chosen_cls(facets, "chestClass")
        sign = chosen_cls(facets, "signClass")
        subs = []
        for c in iter_classes(db):
            try:
                ch = db.super_chain(c)
            except Exception:
                continue
            if te_cls in ch and c != te_cls and c not in (furn, chest, sign):
                subs.append(c)
        if len(subs) > 1:
            narrowed = [c for c in subs if b"EntityId" in db.read(c)]
            if narrowed:
                subs = narrowed
        if len(subs) == 1:
            setf("mobSpawnerClass", cls_anchor(subs[0]), "HIGH",
                 "the only TileEntity subclass that is not chest/furnace/"
                 "sign (mob spawner)")


# ---------------------------------------------------------------------------
# top-level
# ---------------------------------------------------------------------------
def compile_jar(jarpath, version_id):
    db = ClassDb(jarpath)
    result = {"versionId": version_id, "jar": jarpath}
    try:
        family = detect_family(db)
        result["family"] = family
        facets = {}
        if family is None:
            result["error"] = "unknown jar family"
            return result
        chain_w = []
        world_cls = find_world_class(db, family, chain_w)
        result["worldClass"] = world_cls
        result["worldChain"] = chain_w
        if world_cls is None:
            result["error"] = "World class not found"
            return result

        def put(facet, tup):
            chosen, conf, ch = tup[0], tup[1], tup[2]
            over = tup[3] if len(tup) > 3 else None
            if isinstance(chosen, dict):
                pass
            facets[facet] = {"chosen": chosen, "confidence": conf,
                             "chain": ch}

        # --- core 3 (regression) ---
        m, conf, ch = compile_get_block_id(db, world_cls, [])
        put("getBlockId", (mk(world_cls, m), conf, ch))
        m, conf, ch = compile_set_block(db, world_cls, family, [])
        put("setBlock", (mk(world_cls, m), conf, ch))
        out = compile_save_world(db, world_cls, family, [])
        m, conf, ch, cls_over = out
        put("saveWorld", (mk(cls_over or world_cls, m), conf, ch))

        # --- world fields ---
        compile_world_fields(db, world_cls, family, facets, [])

        # --- nibble ---
        m, conf, ch = compile_get_meta(db, world_cls, [])
        put("getMeta", (mk(world_cls, m), conf, ch))
        m, conf, ch = compile_set_meta_silent(db, world_cls, [])
        put("setMetaSilent", (mk(world_cls, m), conf, ch))
        m, conf, ch = compile_get_light(db, world_cls, [])
        put("getLight", (mk(world_cls, m), conf, ch))

        # --- entity ---
        chain = []
        entity_cls, conf, ch = compile_entity_class(db, world_cls, family,
                                                    [])
        facets["entityClass"] = {"chosen": ({"class": entity_cls,
                                             "name": entity_cls.split(
                                                 "/")[-1] if entity_cls
                                             else None,
                                             "descriptor": "class"}
                                            if entity_cls else None),
                                 "confidence": conf, "chain": ch}
        pr, conf, ch, posnames = compile_set_pos_rot(db, entity_cls, []) \
            if entity_cls else (None, "LOW", [], [])
        put("setPosRot", (mk(entity_cls, pr), conf, ch))
        if entity_cls:
            ef = compile_entity_fields(db, entity_cls, pr, [])
            for facet, (chosen, conf) in ef.items():
                facets[facet] = {"chosen": chosen, "confidence": conf,
                                 "chain": ["see setPosRot/field-graph chain"]}
            m, conf, ch = compile_entity_type(db, entity_cls, [])
            put("entityType", (mk(entity_cls, m), conf, ch))

        # --- world entity methods ---
        m, conf, ch, holder = compile_entity_list(db, world_cls, entity_cls,
                                                  family, [])
        put("entityList", (mk(holder or world_cls, m), conf, ch))
        m, conf, ch = compile_spawn_entity(db, world_cls, entity_cls, family,
                                           [])
        put("spawnEntity", (mk(world_cls, m), conf, ch))
        m, conf, ch, cls_over = compile_set_entity_dead(
            db, world_cls, entity_cls, family, [])
        put("setEntityDead", (mk(cls_over or world_cls, m), conf, ch))
        m, conf, ch = compile_play_sound(db, world_cls, [])
        put("playSound", (mk(world_cls, m), conf, ch))
        m, conf, ch = compile_explode(db, world_cls, entity_cls, [])
        put("explode", (mk(world_cls, m), conf, ch))

        # --- inventory ---
        ch = []
        item_cls = compile_item_stack(db, world_cls, facets, ch)
        ch2 = []
        compile_inventory_player(db, world_cls, item_cls, family, facets,
                                 ch2)
        facet_norm(facets)
        ch3 = []
        compile_inv_field(db, world_cls,
                          (facets.get("inventoryPlayerClass", {})
                           .get("chosen") or {}).get("class"), facets, ch3)
        te_chain = []
        te_cls, chest_cls = compile_te(db, world_cls, facets, te_chain)
        facet_norm(facets)
        furn_cls = (facets.get("furnaceClass", {}).get("chosen") or
                    {}).get("class")
        chest_cls2 = (facets.get("chestClass", {}).get("chosen") or
                      {}).get("class")
        ch4 = []
        compile_iinventory(db, chest_cls2, furn_cls, item_cls, facets, ch4)
        m, conf, ch = compile_te_get(db, world_cls, te_cls, [])
        put("teGet", (mk(world_cls, m), conf, ch))

        # --- minecraft / hud ---
        mc_cls, conf, ch = compile_minecraft(db, world_cls, family, [],
                                             entity_cls)
        facets["minecraftClass"] = {"chosen": ({"class": mc_cls,
                                                "name": mc_cls.split("/")[-1]
                                                if mc_cls else None,
                                                "descriptor": "class"}
                                               if mc_cls else None),
                                    "confidence": conf, "chain": ch}
        if mc_cls:
            m, conf, ch = compile_run_tick(db, mc_cls, world_cls, family, [])
            put("runTick", (mk(mc_cls, m), conf, ch))
        m, conf, ch, gui_cls = compile_gui_ingame(db, family, [])
        put("guiIngame", (mk(gui_cls, m), conf, ch))

        # --- mobs ---
        compile_mobs(db, entity_cls, family, facets, [])

        # --- batch-3 extended facet set ---
        compile_extended(db, family, world_cls, entity_cls, mc_cls,
                         facets, version_id)

        # blocked facets get explicit records
        for facet, why in BLOCKED.items():
            facets[facet] = {"chosen": None, "confidence": "BLOCKED",
                             "chain": [why]}

        facet_norm(facets)
        result["facets"] = facets
        return result
    finally:
        db.close()


def facet_norm(facets):
    """convert (chosen, conf, chain) tuple entries to the dict form."""
    for k, v in list(facets.items()):
        if isinstance(v, tuple):
            facets[k] = {"chosen": v[0], "confidence": v[1], "chain": v[2]}


def mk(cls, m):
    if m is None or cls is None:
        return None
    if isinstance(m, dict) and "name" in m:
        return {"class": cls, "name": m["name"], "descriptor": m["desc"]}
    return None


def compile_set_block(db, world_cls, family, chain):
    meths = db.methods(world_cls, code=True)
    cands = [m for m in meths if m["desc"] == "(IIII)Z" and is_public(m)
             and not is_static(m)]
    chain.append("candidates: public (IIII)Z = [%s]"
                 % ", ".join(m["name"] for m in cands))
    if not cands:
        return None, "LOW", chain
    if len(cands) == 1:
        chain.append("unique (IIII)Z candidate -> %s" % cands[0]["name"])
        return cands[0], "HIGH", chain
    delegates = []
    for m in cands:
        same = [c for c in callees(m["body"])
                if c[0] is None and c[2] == "(IIII)Z"]
        if same:
            chain.append("%s delegates to sibling (IIII)Z core -> wrapper"
                         % m["name"])
            delegates.append(m)
    pool = delegates or cands
    if len(pool) > 1:
        pool2 = [m for m in pool
                 if any(c[0] is None and c[2] == "(IIII)V"
                        for c in callees(m["body"]))]
        if pool2:
            pool = pool2
    if len(pool) > 1 and family == "G1":
        gated = [m for m in pool if re.search(r"// Field \S+:Z", m["body"])
                 and has(m["body"], "iconst_0")]
        if gated:
            pool = gated
    if not delegates:
        return pool[0], "LOW", chain
    if len(pool) == 1:
        return pool[0], "HIGH", chain
    return pool[0], "MEDIUM", chain


def body_save_evidence(db, world_cls, m, depth=0):
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
                            chain.append("rule A: wrapper %s%s -> core %s%s "
                                         "with '%s'" % (w["name"], w["desc"],
                                                        core["name"],
                                                        core_desc, ev))
                            return w, "HIGH", chain, None
    for w in meths:
        if not (not is_static(w) and w["desc"] == "(Z)V"):
            continue
        if TICK_STRING in " ".join(strings(w["body"])):
            continue
        ev = body_save_evidence(db, world_cls, w)
        if ev:
            chain.append("rule B: (Z)V core %s carries '%s'" % (w["name"],
                                                                ev))
            return w, "HIGH", chain, None
    for w in meths:
        if not (is_public(w) and not is_static(w) and w["desc"] == "()V"):
            continue
        cs = callees(w["body"])
        if len(cs) == 1 and cs[0][0] is None and cs[0][2] == "(Z)V":
            for core in meths:
                if core["name"] == cs[0][1] and core["desc"] == "(Z)V":
                    ev = body_save_evidence(db, world_cls, core)
                    if ev:
                        chain.append("rule C: ()V wrapper %s -> (Z)V core %s"
                                     % (w["name"], core["name"]))
                        return w, "HIGH", chain, None
    for d in ["(L%s;Ljava/io/File;)Z" % world_cls,
              "(L%s;Ljava/io/OutputStream;)V" % world_cls]:
        hits = []
        for name in db.names:
            cls = db.class_simple(name)
            if cls is None or cls == world_cls:
                continue
            if d.encode("ascii") in db.read(cls):
                hits.append(cls)
        def rank(c):
            data = db.read(c)
            return -sum(1 for s in SAVE_EVIDENCE + ["MinecraftLevel"]
                        if s.encode("ascii") in data)
        hits.sort(key=rank)
        for cls in hits:
            for m in db.methods(cls, code=False):
                if m["desc"] == d:
                    chain.append("rule D: serializer %s.%s%s"
                                 % (cls, m["name"], d))
                    return m, "HIGH", chain, cls
    chain.append("FAIL: no saveWorld facet matched rules A-D")
    return None, "LOW", chain, None


# ---------------------------------------------------------------------------
# F5 reconciliation
# ---------------------------------------------------------------------------
def load_oracle(jdata):
    out = {}
    if "targets" in jdata:
        t = jdata["targets"]
        for slot in t:
            s = t[slot]
            if not s or not s.get("class"):
                continue
            if s.get("name") is None and s.get("descriptor") is None:
                # class anchor: compare by class only
                out[slot] = {"class": norm_cls(s["class"]), "name": None,
                             "descriptor": "class"}
                continue
            if not s.get("name"):
                continue
            out[slot] = {"class": norm_cls(s["class"]), "name": s["name"],
                         "descriptor": s["descriptor"]}
        return out
    if "hooks" in jdata:  # runtime schema
        hooks = jdata["hooks"]
        ops = jdata["ops"]
        w = hooks["SAVE_WORLD"]["class"]
        e = ops.get("entityClassG")
        mc = hooks.get("RUN_TICK", {}).get("class")
        hud = hooks.get("HUD_RENDER")
        jar = None  # desc resolution happens in recon with jar open

        def slot(cls, name, desc):
            if cls is None or name is None:
                return None
            return {"class": cls, "name": name, "descriptor": desc}

        out["getBlockId"] = slot(w, ops.get("getBlockM"), "(III)I")
        out["setBlock"] = slot(w, ops.get("setBlockM"), "(IIII)Z")
        out["saveWorld"] = slot(hooks["SAVE_WORLD"]["class"],
                                hooks["SAVE_WORLD"]["method"],
                                hooks["SAVE_WORLD"]["desc"])
        out["getMeta"] = slot(w, ops.get("getMetaM"), "(III)I")
        out["setMetaSilent"] = slot(w, ops.get("setMetaSilentM"), "(IIII)V")
        out["getLight"] = slot(w, ops.get("getLightM"), "(III)I")
        out["entityClass"] = slot(e, e.split("/")[-1] if e else None, "class")
        out["setPosRot"] = slot(e, ops.get("setPosRotM"),
                                ops.get("setPosRotDesc"))
        for facet, fkey in (("posX", "posXF"), ("posY", "posYF"),
                            ("posZ", "posZF"), ("motX", "motXF"),
                            ("motY", "motYF"), ("motZ", "motZF")):
            nm = ops.get(fkey)
            out[facet] = slot(e, nm, "D" if e == ops.get("entityClassG")
                              else None)
        for facet, fkey in (("yaw", "yawF"), ("pitch", "pitchF")):
            out[facet] = slot(e, ops.get(fkey), "F")
        out["iframes"] = slot(e, ops.get("iframesF"), "I")
        out["entityType"] = slot(e, ops.get("entityTypeM"),
                                 "()Ljava/lang/String;")
        out["entityList"] = slot(w, ops.get("entityListM"), None)
        out["spawnEntity"] = slot(w, ops.get("spawnEntityM"), None)
        out["setEntityDead"] = slot(w, ops.get("setEntityDeadM"), None)
        out["playSound"] = slot(w, ops.get("playSoundM"),
                                "(DDDLjava/lang/String;FF)V")
        out["explode"] = slot(w, ops.get("explodeM"), None)
        out["teGet"] = slot(w, ops.get("teGetM"), None)
        out["minecraftClass"] = slot(mc, mc.split("/")[-1] if mc else None,
                                     "class")
        out["runTick"] = slot(mc, hooks.get("RUN_TICK", {}).get("method"),
                              hooks.get("RUN_TICK", {}).get("desc"))
        if hud:
            out["guiIngame"] = slot(hud["class"], hud["method"], hud["desc"])
        for facet, key in (("mobPig", "pig"), ("mobSheep", "sheep"),
                           ("mobCreeper", "creeper"),
                           ("mobSkeleton", "skeleton"),
                           ("mobZombie", "zombie"), ("mobSpider", "spider"),
                           ("mobTnt", "tnt"), ("mobGiant", "giant")):
            c = (ops.get("mobClasses") or {}).get(key)
            out[facet] = slot(c, c, "class") if c else None
        out["mobBase"] = slot(ops.get("mobBaseClassG"),
                              (ops.get("mobBaseClassG") or "x").split(
                                  "/")[-1], "class")
        out["mobCreatureClass"] = slot(ops.get("mobCreatureClassG"),
                                       (ops.get("mobCreatureClassG") or "x")
                                       .split("/")[-1], "class")
        out["scatterClass"] = slot(ops.get("scatterClassG"),
                                   (ops.get("scatterClassG") or "x")
                                   .split("/")[-1], "class")
        # world-info fields: class resolved from World accessor return type
        wi = ops.get("worldInfoClassG") or \
            ("WORLDINFO" if ops.get("getWorldInfoM") else None)
        if wi:
            out["worldTime"] = slot(wi, ops.get("worldTimeF"), "J")
            out["randSeed"] = slot(wi, ops.get("randSeedF"), "J")
            out["spawnX"] = slot(wi, ops.get("spawnXF"), "I")
            out["spawnY"] = slot(wi, ops.get("spawnYF"), "I")
            out["spawnZ"] = slot(wi, ops.get("spawnZF"), "I")
            out["difficulty"] = slot(wi, ops.get("difficultyF"), "I")
        # drop unresolvable (None) entries -> N/A
        return {k: v for k, v in out.items() if v and v.get("name")
                and v.get("class")}
    return out


def resolve_jar(vid, jdata):
    cands = []
    if "jar" in jdata:
        cands.append(os.path.expandvars(jdata["jar"].replace("/", os.sep)))
    base = os.path.expandvars(r"%APPDATA%\.minecraft\versions")
    cands.append(os.path.join(base, vid, vid + ".jar"))
    p = glob.glob(os.path.join(base, vid, "*.jar"))
    cands.extend(p)
    attic = os.path.expandvars(r"%APPDATA%\.attic\versions")
    cands.extend(glob.glob(os.path.join(attic, vid + "*.jar")))
    for c in cands:
        if c and os.path.isfile(c):
            return c
    return None


def resolve_runtime_descs(jarpath, oracle, ops=None):
    """For runtime slots whose descriptor is None, resolve from the jar by
    (class, name) uniqueness. Ambiguous -> drop slot (N/A). The WORLDINFO
    sentinel class is resolved as the return type of the World accessor."""
    db = ClassDb(jarpath)
    try:
        wi_cls = None
        if any(s["class"] == "WORLDINFO" for s in oracle.values()) and ops:
            w = oracle.get("saveWorld", {}).get("class")
            acc = ops.get("getWorldInfoM")
            if w and acc:
                try:
                    for m in db.methods(w, code=False):
                        if m["name"] == acc and m["desc"] == "()V":
                            continue
                        if m["name"] == acc and m["desc"].startswith("()L"):
                            wi_cls = m["desc"][3:-1]
                            break
                except Exception:
                    wi_cls = None
        for facet, slot in list(oracle.items()):
            if slot["class"] == "WORLDINFO":
                if wi_cls is None:
                    del oracle[facet]
                    continue
                slot["class"] = wi_cls
            if slot["descriptor"] is not None:
                continue
            try:
                meths = db.methods(slot["class"], code=False)
            except Exception:
                del oracle[facet]
                continue
            hits = [m for m in meths if m["name"] == slot["name"]]
            if len(hits) == 1:
                slot["descriptor"] = hits[0]["desc"]
            else:
                del oracle[facet]
        return oracle
    finally:
        db.close()


def audit_oracle(jarpath, oracle_slot, compiled_slot=None):
    db = ClassDb(jarpath)
    try:
        cls = oracle_slot["class"]
        if oracle_slot["descriptor"] == "class":
            return {"exists": cls in [db.class_simple(x) for x in db.names]}
        for m in db.methods(cls, code=True):
            if m["name"] == oracle_slot["name"] and \
                    m["desc"] == oracle_slot["descriptor"]:
                ev = {"exists": True, "flags": sorted(m["flags"]),
                      "tick_body": TICK_STRING in " ".join(
                          strings(m["body"])),
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
    except Exception:
        return {"exists": "class-missing"}
    finally:
        db.close()


def run_recon(fp_dir, report_path):
    rows = []
    for jpath in sorted(glob.glob(os.path.join(fp_dir, "*.json"))):
        with open(jpath, "r", encoding="utf-8") as f:
            jdata = json.load(f)
        vid = jdata.get("versionId") or \
            os.path.splitext(os.path.basename(jpath))[0]
        oracle = load_oracle(jdata)
        if not oracle:
            rows.append({"version": vid, "status": "SKIPPED",
                         "reason": "no P1 oracle slots"})
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
                         "reason": "SHA1 mismatch"})
            continue
        if "hooks" in jdata:
            oracle = resolve_runtime_descs(jarpath, oracle,
                                           jdata.get("ops"))
        res = compile_jar(jarpath, vid)
        row = {"version": vid, "status": "RUN", "jar": jarpath,
               "family": res.get("family"), "world": res.get("worldClass"),
               "worldChain": res.get("worldChain", []), "facets": {}}
        for facet in FACETS:
            got = (res.get("facets") or {}).get(facet, {})
            want = oracle.get(facet)
            chosen = got.get("chosen")
            cell = {"oracle": want, "compiled": chosen,
                    "confidence": got.get("confidence", "ABSENT"),
                    "chain": got.get("chain", [])}
            if got.get("confidence") == "BLOCKED":
                cell["verdict"] = "BLOCKED"
            elif want is None:
                cell["verdict"] = "N/A"
            elif chosen is None:
                cell["verdict"] = "MISS"
            else:
                if want.get("descriptor") == "class":
                    same = chosen.get("class") == want.get("class")
                else:
                    same = all(chosen.get(k) == want.get(k)
                               for k in ("class", "name", "descriptor"))
                cell["verdict"] = "MATCH" if same else "MISS"
                if not same:
                    cell["oracle_audit"] = audit_oracle(jarpath, want, chosen)
            row["facets"][facet] = cell
        rows.append(row)
    write_report(rows, report_path)
    return rows


def write_report(rows, path):
    lines = ["# F5 reconciliation report (F1 slow-path compiler, expanded "
             "facet set)", ""]
    lines.append("Verdicts: MATCH = class+name+descriptor equal; N/A = "
                 "version has no oracle slot (or runtime class unresolvable);"
                 " BLOCKED = facet rule not generalizable (see summary); "
                 "MISS = differs.")
    lines.append("")
    for facet in FACETS:
        lines.append("| %-22s | %s |" % ("facet", " | ".join(
            r["version"].replace("inf-2010", "inf.").replace(
                "in-2010", "in.")[:9] for r in rows if
            r["status"] == "RUN")))
        break
    hdr = "| facet | " + " | ".join(
        r["version"][:10] for r in rows if r["status"] == "RUN") + " |"
    sep = "|---" * (sum(1 for r in rows if r["status"] == "RUN") + 1) + "|"
    lines.append(hdr)
    lines.append(sep)
    for facet in FACETS:
        cells = []
        for r in rows:
            if r["status"] != "RUN":
                continue
            c = r["facets"].get(facet)
            if c is None:
                cells.append("-")
            elif c["verdict"] == "MATCH":
                cells.append("M")
            elif c["verdict"] == "N/A":
                cells.append(".")
            elif c["verdict"] == "BLOCKED":
                cells.append("B")
            else:
                cells.append("X")
        lines.append("| %s | %s |" % (facet, " | ".join(cells)))
    lines.append("")
    lines.append("M=MATCH X=MISS .=N/A B=BLOCKED")
    lines.append("")
    total = match = na = blocked = 0
    suspects = []
    for r in rows:
        if r["status"] != "RUN":
            continue
        for facet, c in r["facets"].items():
            if c["verdict"] == "BLOCKED":
                blocked += 1
            elif c["verdict"] == "N/A":
                na += 1
            else:
                total += 1
                if c["verdict"] == "MATCH":
                    match += 1
                else:
                    audit = c.get("oracle_audit") or {}
                    tag = []
                    if audit.get("tick_body"):
                        tag.append("oracle-is-tick-body")
                    if audit.get("delegates_to_compiled"):
                        tag.append("oracle-wrapper-of-compiled")
                    suspects.append("%s/%s compiled=%s oracle=%s audit=%s %s"
                                    % (r["version"], facet,
                                       json.dumps(c["compiled"]),
                                       json.dumps(c["oracle"]),
                                       json.dumps(audit),
                                       ("(SUSPECT: %s)" % ",".join(tag))
                                       if tag else ""))
    lines.append("## Summary")
    lines.append("")
    lines.append("- comparable cells: %d, MATCH: %d, MISS: %d, N/A: %d, "
                 "BLOCKED cells: %d" % (total, match, total - match, na,
                                        blocked))
    lines.append("- BLOCKED facets: " + "; ".join(
        "%s (%s)" % (k, v) for k, v in sorted(BLOCKED.items())))
    lines.append("- MISS/SUSPECT list:")
    for s in suspects:
        lines.append("  - %s" % s)
    if not suspects:
        lines.append("  - none")
    skipped = ["%s: %s" % (r["version"], r.get("reason", ""))
               for r in rows if r["status"] != "RUN"]
    if skipped:
        lines.append("- SKIPPED: " + "; ".join(skipped))
    lines.append("- regression note: core facets getBlockId/setBlock/"
                 "saveWorld keep the batch-1 rules; 0618 saveWorld remains a"
                 " hook-level SUSPECT (isomorphic bytecode, tables pin "
                 "different levels) as adjudicated in batch 1.")
    lines.append("")
    lines.append("## Per-version evidence chains")
    lines.append("")
    for r in rows:
        if r["status"] != "RUN":
            continue
        lines.append("### %s (family %s, World=%s)"
                     % (r["version"], r["family"], r["world"]))
        lines.append("")
        for facet in FACETS:
            c = r["facets"].get(facet)
            if not c or c["verdict"] in ("N/A", "BLOCKED"):
                continue
            lines.append("- **%s** verdict=%s confidence=%s"
                         % (facet, c["verdict"], c["confidence"]))
            lines.append("  - oracle: %s" % json.dumps(c["oracle"]))
            lines.append("  - compiled: %s" % json.dumps(c["compiled"]))
            if c.get("oracle_audit"):
                lines.append("  - oracle audit: %s"
                             % json.dumps(c["oracle_audit"]))
            for step in c["chain"][:6]:
                lines.append("  - %s" % step)
        lines.append("")
    with open(path, "w", encoding="ascii", errors="replace") as f:
        f.write("\n".join(lines))


def run_draft(vid, fp_dir, drafts_dir):
    """full-facet draft table for one version id; writes drafts/<vid>.md,
    or a SKIP note when the jar is not available locally."""
    os.makedirs(drafts_dir, exist_ok=True)
    out = os.path.join(drafts_dir, vid + ".md")
    jpath = os.path.join(fp_dir, vid + ".json")
    jdata = {}
    if os.path.isfile(jpath):
        with open(jpath, "r", encoding="utf-8") as f:
            jdata = json.load(f)
    jarpath = resolve_jar(vid, jdata)
    if jarpath is None:
        url = None
        vj = os.path.normpath(os.path.join(
            os.path.dirname(os.path.abspath(__file__)),
            "..", "retromcp", "pages", "versions", "versions.json"))
        try:
            with open(vj, "r", encoding="utf-8") as f:
                url = (json.load(f).get(vid) or {}).get("client_url")
        except Exception:
            pass
        lines = ["# %s -- SKIP (no local jar)" % vid, "",
                 "The client jar is not present locally, so the F1 facet"
                 " draft could not be compiled."]
        if url:
            lines += ["", "- client_url: %s" % url]
        with open(out, "w", encoding="ascii", errors="replace") as f:
            f.write("\n".join(lines) + "\n")
        return "SKIP %s -> %s" % (vid, out)
    res = compile_jar(jarpath, vid)
    lines = ["# %s -- F1 draft facet table" % vid, "",
             "- jar: %s" % jarpath,
             "- sha1: %s" % sha1_of(jarpath),
             "- family: %s; World: %s" % (res.get("family"),
                                          res.get("worldClass")), ""]
    if res.get("error"):
        lines.append("- error: %s" % res["error"])
        lines.append("")
    lines.append("| facet | class | name | descriptor | confidence |")
    lines.append("|---|---|---|---|---|")
    facets = res.get("facets") or {}
    for facet in FACETS:
        got = facets.get(facet) or {}
        chosen = got.get("chosen")
        conf = got.get("confidence", "ABSENT")
        if chosen:
            lines.append("| %s | %s | %s | %s | %s |"
                         % (facet, chosen.get("class"), chosen.get("name"),
                            chosen.get("descriptor"), conf))
        else:
            why = (got.get("chain") or [""])[0]
            if len(why) > 100:
                why = why[:100] + "..."
            lines.append("| %s | - | - | - | %s: %s |" % (facet, conf, why))
    with open(out, "w", encoding="ascii", errors="replace") as f:
        f.write("\n".join(lines) + "\n")
    return "DRAFT %s -> %s" % (vid, out)


def main():
    ap = argparse.ArgumentParser(description="F1 anchor compiler / F5 recon")
    ap.add_argument("--jar")
    ap.add_argument("--version-id")
    ap.add_argument("--recon", action="store_true")
    ap.add_argument("--draft", metavar="VERSION",
                    help="full-facet draft table -> drafts/VERSION.md")
    ap.add_argument("--fingerprints-dir",
                    default=os.path.join(os.path.dirname(
                        os.path.abspath(__file__)), "..", "..", "java",
                        "agent", "registry", "fingerprints"))
    ap.add_argument("--report",
                    default=os.path.join(os.path.dirname(
                        os.path.abspath(__file__)), "report.md"))
    args = ap.parse_args()
    if args.recon:
        rows = run_recon(os.path.normpath(args.fingerprints_dir),
                         os.path.normpath(args.report))
        n_run = sum(1 for r in rows if r["status"] == "RUN")
        print("recon: %d versions run; report -> %s" % (n_run, args.report))
        return 0
    if args.draft:
        here = os.path.dirname(os.path.abspath(__file__))
        print(run_draft(args.draft, os.path.normpath(args.fingerprints_dir),
                        os.path.join(here, "drafts")))
        return 0
    if not args.jar or not args.version_id:
        ap.error("--jar and --version-id required in compile mode")
    res = compile_jar(args.jar, args.version_id)
    json.dump(res, sys.stdout, indent=2)
    print()
    return 0


if __name__ == "__main__":
    sys.exit(main())
