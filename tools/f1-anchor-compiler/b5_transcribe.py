#!/usr/bin/env python3
# -*- coding: ascii -*-
"""B5 transcription tool: F1 draft md -> runtime fingerprint JSON.

Input : tools/f1-anchor-compiler/drafts/<versionId>.md  (F1 facet table)
Output: java/agent/registry/fingerprints/<versionId>.json (schema=mca-runtime-v1)

The runtime JSON must satisfy the MiniJson integer-subset protocol used by the
agent loader (FingerprintLoader.java):
  - no null literals (absent key == unpinned slot == table null, tier-1 model)
  - nesting <= 1 level (top object + ops/mobs/hooks maps of strings/ints)
  - total <= 4096 chars, each string <= 256 chars
  - non-ASCII text is emitted as \\uXXXX escapes (json.dumps ensure_ascii)

Slot mapping: ops keys are exactly VersionTable public field names. Details the
draft does not carry (difficulty field, HUD chain letters, hook access flags /
hit counts / inject modes) are supplemented from javap on the LOCAL jar
(%APPDATA%\\.minecraft\\versions\\<id>\\<id>.jar, SHA1 cross-checked below) and
embedded as OVERRIDES / HOOKS tables with the javap evidence in comments.
Evidence dumps: %TEMP%\\mcaut\\b5\\*.txt (javap -p -c, JDK 1.8.0_202).

Usage: python b5_transcribe.py            # transcribe all VERSIONS below
       python b5_transcribe.py <id> ...   # transcribe given ids only
"""

import json
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
DRAFT_DIR = os.path.join(HERE, "drafts")
OUT_DIR = os.path.normpath(os.path.join(HERE, "..", "..",
                                       "java", "agent", "registry", "fingerprints"))

# versionId -> jar SHA1 (must equal draft header "sha1:" line; strict gate)
EXPECTED_SHA1 = {
    "inf-20100624": "644c050e846035e06a6637bffa2afee1e5769c8c",
    "a1.0.3": "31e9736457ef3e0bfea69c720137a1bd8ba7caae",
}

# draft facet -> VersionTable field (plain method/field-name slot)
FACET_SLOT = {
    "getBlockId": "getBlockM",
    "setBlock": "setBlockM",
    "worldTime": "worldTimeF",
    "randSeed": "randSeedF",
    "spawnX": "spawnXF", "spawnY": "spawnYF", "spawnZ": "spawnZF",
    "getMeta": "getMetaM",
    "setMetaSilent": "setMetaSilentM",
    "getLight": "getLightM",
    "posX": "posXF", "posY": "posYF", "posZ": "posZF",
    "motX": "motXF", "motY": "motYF", "motZ": "motZF",
    "yaw": "yawF", "pitch": "pitchF",
    "iframes": "iframesF",
    "entityType": "entityTypeM",
    "entityList": "entityListM",
    "spawnEntity": "spawnEntityM",
    "setEntityDead": "setEntityDeadM",
    "playSound": "playSoundM",
    "explode": "explodeM",
    "isCount": "isCountF", "isId": "isIdF", "isDmg": "isDmgF",
    "invMain": "invMainF", "invArmor": "invArmorF", "invCur": "invCurF",
    "addStack": "addStackM",
    "invField": "invF",
    "invSize": "invSizeM", "invGet": "invGetM", "invSet": "invSetM",
    "teGet": "teGetM",
    "teBurn": "teBurnF", "teCook": "teCookF",
    "itemRegistry": "itemsF",
    "entityListLookup": "entityListLookupM",
    "playerField": "playerF",
    "playerListField": "playerListF",
    "health": "healthF",
    "observers": "observersF",
    "light6": "light6M",
}

# facet -> VersionTable field, value = class column with '/' -> '.' (FQCN form)
FACET_SLOT_CLASS = {
    "entityClass": "entityClassG",
    "itemClass": "itemClassG",
    "itemEntityClass": "itemEntityG",
    "entityListClass": "entityListClassG",
    "scatterClass": "scatterClassG",
}

# facet -> (name slot, desc slot). saveWorldDesc param class is dot-form
# ("(La/b;)V" -> "(La.b;)V"): WorldOps parses it with Class.forName().
FACET_SLOT_DESC = {
    "saveWorld": ("saveWorldM", "saveWorldDesc"),
    "setPosRot": ("setPosRotM", "setPosRotDesc"),
}

# facet -> mobClasses key (value = class column, dot form)
FACET_MOB = {
    "mobPig": "pig", "mobSheep": "sheep", "mobCreeper": "creeper",
    "mobSkeleton": "skeleton", "mobZombie": "zombie", "mobSpider": "spider",
    "mobTnt": "tnt", "mobGiant": "giant", "mobBase": "mob",
}

# Details BLOCKED/absent in the draft, pinned from javap on the local jar.
OVERRIDES = {
    "inf-20100624": {
        # javap net.minecraft.client.f (Minecraft) runTick: 1078 putfield
        # net/minecraft/a/a/h.g:I <- getfield net/minecraft/client/u.v:I
        # (GameSettings.difficulty). Unique World int field written by the
        # client tick from the settings object (0327 'd' criterion, same site).
        "difficultyF": "g",
        # HUD chain (MCAHooks.onHud letters; javap client/f field decls):
        # f=Lnet/minecraft/client/g/a; (thePlayer) m=Lnet/minecraft/client/c/k;
        # (fontRenderer) u=Lnet/minecraft/client/u; (gameSettings); gate=
        # client/u.d:Z (showFPS) read in GuiIngame client/c/m a(F)V @1272.
        "hudPlayerF": "f", "hudFontF": "m", "hudSettingsF": "u",
        "hudDebugGateF": "d",
    },
    "a1.0.3": {
        # javap av (Minecraft) runTick: 802 putfield fi.i:I <- getfield
        # jj.x:I (GameSettings.difficulty). Same uniqueness criterion.
        "difficultyF": "i",
        # HUD chain (javap av field decls): g=Ldf; (thePlayer, df extends gn
        # extends ki=EntityLiving) o=Lem; (fontRenderer) y=Ljj; (gameSettings);
        # gate=jj.d:Z (showFPS) read in gq a(FZII)V @930.
        "hudPlayerF": "g", "hudFontF": "o", "hudSettingsF": "y",
        "hudDebugGateF": "d",
    },
}

# Hook weaving anchors (class|name|desc|access|expectedHits|injectMode).
# access = javap flags (decimal): 1=public 2=private 17=public final.
# expectedHits = count of exact (name,desc) overloads in the class (javap -p).
# Not consumed by FingerprintLoader yet (HookTargets untouched this batch);
# carried as anchor data for the B5-d live batch. Giant ctors verified alive
# in both jars (javap net.minecraft.a.c.a.a / ac) -> no NEW_CTOR hook needed.
HOOKS = {
    "inf-20100624": {
        "SAVE_WORLD": "net/minecraft/a/a/h|a|(La/b;)V|17|1|ENTRY",
        "WORLD_CTOR": "net/minecraft/a/a/h|<init>|(Ljava/io/File;Ljava/lang/String;)V|1|1|ENTRY",
        "LOAD_WORLD": "net/minecraft/client/f|a|(Lnet/minecraft/a/a/h;Ljava/lang/String;)V|2|1|ENTRY",
        "RUN_TICK": "net/minecraft/client/f|m|()V|2|1|ENTRY",
        "HUD_RENDER": "net/minecraft/client/c/m|a|(F)V|17|1|EXIT",
    },
    "a1.0.3": {
        "SAVE_WORLD": "fi|a|(Ljm;)V|1|1|ENTRY",
        "WORLD_CTOR": "fi|<init>|(Ljava/io/File;Ljava/lang/String;)V|1|1|ENTRY",
        "LOAD_WORLD": "av|a|(Lfi;Ljava/lang/String;)V|1|1|ENTRY",
        "RUN_TICK": "av|i|()V|1|1|ENTRY",
        "HUD_RENDER": "gq|a|(FZII)V|1|1|EXIT",
    },
}

# Provenance (Chinese text emitted as \\uXXXX by json.dumps ensure_ascii).
PROVENANCE = ("B5 \u8f6c\u5199\u81ea F1 \u8349\u7a3f %s.md\uff08sha1 \u5bf9"
              "\u69ab\uff09\uff1bjavap \u672c\u5730 jar \u8865\u9489\uff08"
              "difficulty/HUD/hooks access\uff09\uff1b\u5f85\u6d3b\u4f53"
              "\u8f6c\u6b63\uff1b\u8f6c\u6b63\u524d status=draft")

ROW_RE = re.compile(r"^\|\s*([A-Za-z0-9_]+)\s*\|\s*([^|]*?)\s*\|\s*([^|]*?)\s*\|\s*([^|]*?)\s*\|\s*([^|]*?)\s*\|\s*$")


def parse_draft(path):
    facets = {}
    header = {}
    with open(path, "r", encoding="utf-8") as f:
        for line in f:
            line = line.rstrip("\n")
            m = re.match(r"^- (jar|sha1|family): (.*)$", line)
            if m:
                header[m.group(1)] = m.group(2).strip()
                continue
            m = ROW_RE.match(line.strip())
            if m and m.group(1) != "facet":
                cls, name, desc, conf = (m.group(i).strip()
                                         for i in (2, 3, 4, 5))
                facets[m.group(1)] = (cls, name, desc, conf)
    return header, facets


def dots(cls):
    return cls.replace("/", ".")


def transcribe(vid):
    src = os.path.join(DRAFT_DIR, vid + ".md")
    header, facets = parse_draft(src)
    sha1 = header.get("sha1", "")
    if sha1 != EXPECTED_SHA1[vid]:
        raise SystemExit("FATAL %s: draft sha1 %s != expected %s"
                         % (vid, sha1, EXPECTED_SHA1[vid]))
    fam = header.get("family", "").split(";")[0].strip()
    ops = {}
    mobs = {}
    unpinned = []
    for facet, (cls, name, desc, conf) in sorted(facets.items()):
        if facet in FACET_SLOT:
            if name == "-":
                unpinned.append(FACET_SLOT[facet])
                continue
            ops[FACET_SLOT[facet]] = name
        elif facet in FACET_SLOT_CLASS:
            if name == "-":
                unpinned.append(FACET_SLOT_CLASS[facet])
                continue
            ops[FACET_SLOT_CLASS[facet]] = dots(cls)
        elif facet in FACET_SLOT_DESC:
            nslot, dslot = FACET_SLOT_DESC[facet]
            if name == "-":
                unpinned.extend([nslot, dslot])
                continue
            ops[nslot] = name
            ops[dslot] = desc if dslot == "setPosRotDesc" else desc.replace("/", ".")
        elif facet in FACET_MOB:
            if name == "-":
                unpinned.append("mob." + FACET_MOB[facet])
                continue
            mobs[FACET_MOB[facet]] = dots(cls)
    for key, val in sorted(OVERRIDES[vid].items()):
        ops[key] = val
    # sanity: every mapped facet present in draft at all (schema drift guard)
    expected = set(FACET_SLOT) | set(FACET_SLOT_CLASS) | set(FACET_SLOT_DESC) | set(FACET_MOB)
    missing = expected - set(facets)
    if missing:
        raise SystemExit("FATAL %s: draft missing facets %s" % (vid, sorted(missing)))
    doc = {
        "schema": "mca-runtime-v1",
        "versionId": vid,
        "family": fam,
        "jarSha1": sha1,
        "status": "draft",
        "provenance": PROVENANCE % vid,
        "ops": ops,
        "mobs": mobs,
        "hooks": HOOKS[vid],
    }
    text = json.dumps(doc, ensure_ascii=True, sort_keys=True, indent=1)
    # MiniJson protocol gates
    if len(text) > 4096:
        raise SystemExit("FATAL %s: runtime JSON %d chars > MiniJson 4096" % (vid, len(text)))
    if "null" in text:
        raise SystemExit("FATAL %s: null literal leaked" % vid)
    for s in re.findall(r'"((?:[^"\\]|\\.)*)"', text):
        if len(s) > 256:
            raise SystemExit("FATAL %s: string too long: %s..." % (vid, s[:40]))
    if not text.startswith("{") or not text.endswith("}"):
        raise SystemExit("FATAL %s: not a top-level object" % vid)
    out = os.path.join(OUT_DIR, vid + ".json")
    with open(out, "w", encoding="ascii", newline="\n") as f:
        f.write(text + "\n")
    print("WROTE %s (%d chars, ops=%d mobs=%d hooks=%d unpinned=%s)"
          % (out, len(text), len(ops), len(mobs), len(doc["hooks"]),
             ",".join(sorted(unpinned)) or "-"))


def main():
    ids = sys.argv[1:] or sorted(EXPECTED_SHA1)
    for vid in ids:
        if vid not in EXPECTED_SHA1:
            raise SystemExit("unknown version id: " + vid)
        transcribe(vid)


if __name__ == "__main__":
    main()
