#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
B2-1 tiny 映射对账工具（spec 0327 tiny 映射对账，纯离线研究工具，与 agent 运行时解耦）。

输入（均只读）：
  - 锚库 JSON：java/agent/registry/fingerprints/inf-20100327.json 的 targets 节
  - RetromCP 语料：inf-20100327.zip 内 client.tiny（tiny v2，official→named）

流程：
  1. 解析 client.tiny → official↔named 双向类映射 + 按 named 类索引的成员表
     （注意：实测该 tiny 成员行只带一个 desc 字段，且为 official 侧类名形式；
      "named 侧 desc"由 official desc 经类映射反写 named 类名得到，再建索引）
  2. 对照表 SEMANTICS：逐槽写死"槽位→named 语义"（来源=锚库 provenance 文字 +
     对 named 侧映射的阅读），desc 用 named 类名的 JVM 内部形式书写
  3. 解析：named 语义 → 候选 official 三元组（desc 中 named 类名经映射换成 official 斜杠名）
  4. 与锚库 targets 逐槽 diff：MATCH / MISMATCH / MISSING
  5. 覆盖率统计 + 逐槽表写入 report.md

戒律：绝不写锚库/语料/java\\agent 下任何文件；产物只落本目录。
"""
import io
import json
import re
import sys
import zipfile
from datetime import datetime
from pathlib import Path

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

# ---------------------------------------------------------------- 输入路径
ZIP_PATH = Path(r'C:\Users\GAME\AppData\Local\Temp\mcaut\retromcp\pages\versions\inf-20100327.zip')
ANCHOR_PATH = Path(r'e:\4k\github-projects\MCWorldAgent\java\agent\registry\fingerprints\inf-20100327.json')
REPORT_PATH = Path(r'e:\4k\github-projects\MCWorldAgent\tools\b2-1-tiny-recon\report.md')

# ---------------------------------------------------------------- tiny 解析
def load_tiny(zip_path: Path):
    """解析 zip 内 client.tiny（tiny v2）。
    返回 (off2named, named2off, members)：
      members[named类斜杠名] = [(kind, official_desc, official名, named名), ...]
    """
    with zipfile.ZipFile(zip_path) as z:
        text = z.read('client.tiny').decode('utf-8')
    lines = text.splitlines()
    head = lines[0].split('\t')
    assert head[0] == 'tiny' and head[1] == '2', f'tiny v2 头不符: {lines[0]!r}'
    off2named, named2off, members = {}, {}, {}
    cur = None
    for ln in lines[1:]:
        if not ln:
            continue
        p = ln.split('\t')
        if p[0] == 'c':                      # c<TAB>official<TAB>named
            cur = p[2]
            off2named[p[1]] = cur
            named2off[cur] = p[1]
            members[cur] = []
        elif p[0] == '' and p[1] in ('m', 'f'):   # 成员行以 TAB 缩进
            members[cur].append((p[1], p[2], p[3], p[4]))
    return off2named, named2off, members

_LCLASS = re.compile(r'L([^;]+);')

def make_desc_converter(mapping):
    """生成 desc 类名改写函数：把 desc 里每个 L<class>; 的类名按 mapping 换名，
    映射中不存在的类名（如 java/util/List）原样保留。"""
    def conv(desc: str) -> str:
        return _LCLASS.sub(lambda m: 'L' + mapping.get(m.group(1), m.group(1)) + ';', desc)
    return conv

# ---------------------------------------------------------------- 槽位→named 语义对照表
# 本表是工具的核心知识：逐槽写死 named 侧语义，来源=锚库 provenance 文字 + 对
# client.tiny named 侧映射的阅读。kind: 'class' | 'method' | 'field'。
# desc 一律用 named 类名的 JVM 内部斜杠形式书写（解析时再经类映射换成 official 比对）。
W  = 'net/minecraft/game/world/World'
E  = 'net/minecraft/game/entity/Entity'
EL = 'net/minecraft/game/entity/EntityLiving'
EP = 'net/minecraft/game/entity/player/EntityPlayer'
IP = 'net/minecraft/game/entity/player/InventoryPlayer'
IS = 'net/minecraft/game/item/ItemStack'
IT = 'net/minecraft/game/item/Item'
II = 'net/minecraft/game/IInventory'
TE = 'net/minecraft/game/world/block/tileentity/TileEntity'
TC = 'net/minecraft/game/world/block/tileentity/TileEntityChest'
TF = 'net/minecraft/game/world/block/tileentity/TileEntityFurnace'
EI = 'net/minecraft/game/entity/misc/EntityItem'
WM = 'net/minecraft/game/world/terrain/generate/WorldGenMinable'
MC = 'net/minecraft/client/Minecraft'
GI = 'net/minecraft/client/gui/GuiIngame'
CK = 'net/minecraft/game/world/chunk/Chunk'
ENT = 'Lnet/minecraft/game/entity/Entity;'
IST = 'Lnet/minecraft/game/item/ItemStack;'
IPL = 'Lnet/minecraft/game/entity/player/InventoryPlayer;'
TES = 'Lnet/minecraft/game/world/block/tileentity/TileEntity;'

SEMANTICS = {
    # ---- World 系（provenance: WorldOps.java 系列 javap/NBT 取证）
    'worldClass':      ('class', W, None, None, '锚库称 World 宿主类；映射 named 侧即 World'),
    'worldCtor':       ('method', W, '<init>', '(Ljava/io/File;Ljava/lang/String;)V',
                        'provenance: World.<init>(File,String) 0x0001；构造器不重命名，tiny 不列 World 的 <init> 行，映射侧无法核验'),
    'setBlock':        ('method', W, 'setBlockWithNotify', '(IIII)Z',
                        'provenance: setBlockWithNotify (IIII)Z public final'),
    'getBlockId':      ('method', W, 'getBlockId', '(III)I', 'provenance: getBlockId (III)I'),
    'saveWorld':       ('method', W, 'saveWorld', '(Z)V', 'provenance: saveWorld (Z)V private'),
    'worldTime':       ('field', W, 'worldTime', 'J', 'provenance: NBT "Time" 键绑定'),
    'randSeed':        ('field', W, 'randomSeed', 'J', 'provenance: NBT "RandomSeed" 绑定'),
    'spawnX':          ('field', W, 'spawnX', 'I', 'provenance: NBT "SpawnX"'),
    'spawnY':          ('field', W, 'spawnY', 'I', 'provenance: NBT "SpawnY"'),
    'spawnZ':          ('field', W, 'spawnZ', 'I', 'provenance: NBT "SpawnZ"'),
    'difficulty':      ('field', W, 'difficultySetting', 'I', 'provenance: F_DIFFICULTY=d meta 轮 javap 取证'),
    'observers':       ('field', W, 'worldAccesses', 'Ljava/util/List;', 'provenance: private List 渲染观察者'),
    'playerField':     ('field', W, 'playerEntity', ENT, 'provenance: EntityPlayer 构造器自设 world.c=this'),
    'getMeta':         ('method', W, 'getBlockMetadata', '(III)I', 'provenance: Chunk.b(III)I 读 nibble 链'),
    'setMetaSilent':   ('method', W, 'setBlockMetadataWithNotify', '(IIII)V',
                        'provenance: 静默 meta 写，系 deobf 误猜名，以字节码为准'),
    'light6':          ('method', W, 'notifyBlocksOfNeighborChange', '(IIII)V', 'provenance: 光照 6 邻更新'),
    'getLight':        ('method', W, 'getBrightness', '(III)F',
                        'provenance: getBrightness 翻案钉死；映射实际 desc 为 (III)F'),
    'explode':         ('method', W, 'createExplosion', f'({ENT}DDDF)V',
                        'provenance: createExplosion (Entity,DDDF)V 双路交叉'),
    'spawnEntity':     ('method', W, 'spawnEntityInWorld', f'({ENT})V',
                        'provenance: spawnEntityInWorld (Entity)V=loadedEntityList.add'),
    'setEntityDead':   ('method', W, 'setEntityDead', f'({ENT})V',
                        'provenance: setEntityDead (Entity)V static（grow-clear 批）；映射 World 侧无此 named 方法（Entity 上有 ()V 实例版），无法核验'),
    'teGet':           ('method', W, 'getBlockTileEntity', f'(III){TES}', 'provenance: tileentity 批双路'),
    'entityList':      ('method', W, 'getLoadedEntityList', '()Ljava/util/List;', 'provenance: getLoadedEntityList()List'),
    'playSound':       ('method', W, 'playSoundEffect', '(DDDLjava/lang/String;FF)V',
                        'provenance: 参数序由 createExplosion 调用点实锤'),
    # ---- Entity 系
    'entityClass':     ('class', E, None, None, 'provenance: Entity 基类 a.c.b（全量反汇编 303 类）'),
    'setPosRot':       ('method', E, 'setLocationAndAngles', '(DDDFF)V',
                        'provenance 写作 setPositionAndRotation；0327 映射 named 名为 setLocationAndAngles'),
    'posX':            ('field', E, 'posX', 'D', 'provenance: d/e/f=lastTick、g/h/i=pos javap 布局钉'),
    'posY':            ('field', E, 'posY', 'D', 'provenance: E_POS_Y=h'),
    'posZ':            ('field', E, 'posZ', 'D', 'provenance: E_POS_Z=i'),
    'motX':            ('field', E, 'motionX', 'D', 'provenance: E_MOT_X=j'),
    'motY':            ('field', E, 'motionY', 'D', 'provenance: E_MOT_Y=k'),
    'motZ':            ('field', E, 'motionZ', 'D', 'provenance: E_MOT_Z=l'),
    'yaw':             ('field', E, 'rotationYaw', 'F', 'provenance: E_YAW=m'),
    'pitch':           ('field', E, 'rotationPitch', 'F', 'provenance: E_PITCH=n'),
    'iframes':         ('field', E, 'heartsLife', 'I', 'provenance: 全伤害源单入口判决（sed-1 批）'),
    'entityType':      ('method', E, 'getEntityType', '()Ljava/lang/String;',
                        'provenance: getEntityType()String，注"与 AABB 字段 q 同名合法"；映射中 q()String 实为 getEntityTexture'),
    # ---- EntityLiving / EntityPlayer（0327 无独立 EntityHuman，映射直接叫 EntityPlayer）
    'entityLivingClass': ('class', EL, None, None, 'provenance: EntityLiving=a.c.e'),
    'health':          ('field', EL, 'health', 'I', 'provenance: NBT "Health" 双向绑定实锤'),
    'entityHumanClass': ('class', EP, None, None, 'provenance: EntityHuman=a.c.e.a；映射 named 侧为 EntityPlayer'),
    'invField':        ('field', EP, 'inventory', IPL, 'provenance: EntityHuman.b=InventoryPlayer'),
    # ---- InventoryPlayer
    'inventoryPlayerClass': ('class', IP, None, None, 'provenance: InventoryPlayer=a.c.e.b'),
    'invMain':         ('field', IP, 'mainInventory', f'[Lnet/minecraft/game/item/ItemStack;', 'provenance: ItemStack[36] 主槽'),
    'invArmor':        ('field', IP, 'armorInventory', f'[Lnet/minecraft/game/item/ItemStack;', 'provenance: ItemStack[4] 盔甲槽'),
    'invCur':          ('field', IP, 'currentItem', 'I', 'provenance: 当前快捷槽'),
    'addStack':        ('method', IP, 'addStack', f'({IST})Z',
                        'provenance: addStack (ItemStack)Z 合并语义 javap 钉；映射 InventoryPlayer 侧无此 named 方法，无法核验'),
    # ---- ItemStack / Item
    'itemStackClass':  ('class', IS, None, None, 'provenance: ItemStack=a.b.k'),
    'isCount':         ('field', IS, 'stackSize', 'I', 'provenance: IS_COUNT=a'),
    'isId':            ('field', IS, 'itemID', 'I', 'provenance: IS_ID=c'),
    'isDmg':           ('field', IS, 'itemDamage', 'I', 'provenance: IS_DMG=d'),
    'itemClass':       ('class', IT, None, None, 'provenance: Item=a.b.i（注册表判决）'),
    'itemRegistry':    ('field', IT, 'itemsList', '[Lnet/minecraft/game/item/Item;', 'provenance: public static i[] 按 id 索引'),
    # ---- IInventory
    'iInventoryClass': ('class', II, None, None, 'provenance: IInventory=a.b'),
    'invSize':         ('method', II, 'getInventorySize', '()I', 'provenance: M_INV_SIZE=a'),
    'invGet':          ('method', II, 'getStackInSlot', f'(I){IST}', 'provenance: M_INV_GET=a'),
    'invSet':          ('method', II, 'setInventorySlotContents', f'(I{IST})V', 'provenance: M_INV_SET=a 直替换无合并'),
    # ---- TileEntity 系
    'tileEntityClass': ('class', TE, None, None, 'provenance: TE 基类=a.a.b.a.a'),
    'chestClass':      ('class', TC, None, None, 'provenance: Chest=a.a.b.a.c（27 槽）'),
    'furnaceClass':    ('class', TF, None, None, 'provenance: Furnace=a.a.b.a.b（3 槽）'),
    'teBurn':          ('field', TF, 'furnaceBurnTime', 'I', 'provenance: NBT BurnTime 对榫实锤'),
    'teCook':          ('field', TF, 'furnaceCookTime', 'I', 'provenance: NBT CookTime 对榫实锤'),
    # ---- 其他类槽
    'itemEntityClass': ('class', EI, None, None, 'provenance: EntityItem，字符串常量判决'),
    'scatterClass':    ('class', WM, None, None, 'provenance: 原版矿石散布器（meta-1 实跑翻案）'),
    # ---- MOB_CLASSES 9 件（字符串常量判决：贴图路径/实体名）
    'mobPig':          ('class', 'net/minecraft/game/entity/animal/EntityPig', None, None, 'MOB_CLASSES'),
    'mobSheep':        ('class', 'net/minecraft/game/entity/animal/EntitySheep', None, None, 'MOB_CLASSES'),
    'mobCreeper':      ('class', 'net/minecraft/game/entity/monster/EntityCreeper', None, None, 'MOB_CLASSES'),
    'mobSkeleton':     ('class', 'net/minecraft/game/entity/monster/EntitySkeleton', None, None, 'MOB_CLASSES'),
    'mobZombie':       ('class', 'net/minecraft/game/entity/monster/EntityZombie', None, None, 'MOB_CLASSES'),
    'mobSpider':       ('class', 'net/minecraft/game/entity/monster/EntitySpider', None, None, 'MOB_CLASSES'),
    'mobTnt':          ('class', 'net/minecraft/game/entity/misc/EntityTNT', None, None, 'MOB_CLASSES，fuse=80 实锤；0327 映射 named 名为 EntityTNT'),
    'mobGiant':        ('class', 'net/minecraft/game/entity/monster/EntityGiant', None, None, 'MOB_CLASSES；0327 映射 named 名为 EntityGiant'),
    'mobBase':         ('class', 'net/minecraft/game/entity/EntityCreature', None, None, 'MOB_CLASSES，a.c.c=EntityCreature 基类'),
    # ---- 客户端
    'minecraftClass':  ('class', MC, None, None, 'HookTargets: RUN_TICK 宿主 client.d'),
    'runTick':         ('method', MC, 'runTick', '()V', 'HookTargets: Minecraft.runTick() h()V 0x0002'),
    'guiIngame':       ('method', GI, 'renderGameOverlay', '(F)V', 'HookTargets: HUD_RENDER，GuiIngame=client.c.m'),
    'chunkGenHeight':  ('method', CK, 'generateHeightMap', '()V', 'HookTargets: GEN_HEIGHT，Chunk.generateHeightMap() a()V 0x0011'),
}

# ---------------------------------------------------------------- 解析 + diff
def resolve_slot(slot, sem, named2off, off2named, members_by_named):
    """named 语义 → 候选 official 三元组 (class, name, descriptor)；找不到返回 None。
    members_by_named[named类][(named名, named desc)] = [(official名, official desc)]
    """
    kind, ncls, nname, ndesc, note = sem
    if ncls not in named2off:
        return None, note + f'；映射中不存在 named 类 {ncls}'
    ocls = named2off[ncls]
    if kind == 'class':
        return (ocls, None, None), note
    entries = members_by_named.get(ncls, {}).get((nname, ndesc))
    if not entries:
        # 退化：仅按 named 名找（重载/签名漂移时仍可定位候选）
        same_name = [(k, v) for k, v in members_by_named.get(ncls, {}).items() if k[0] == nname]
        if same_name:
            (nm, _), vals = same_name[0]
            return (ocls, vals[0][0], vals[0][1]), note + f'；named 侧 {nname} 实际签名为 {vals[0][1]}（对照表书写 {ndesc}）'
        return None, note
    return (ocls, entries[0][0], entries[0][1]), note

def diff_slot(anchor, cand):
    """锚库三元组 vs 映射候选三元组 → (status, 差异描述, 是否同义漂移)"""
    if cand is None:
        return 'MISSING', '映射无此槽', False
    diffs = []
    a_cls = (anchor['class'] or '').replace('.', '/')
    if a_cls != cand[0]:
        diffs.append(f'class: 锚库 {a_cls} vs 映射 {cand[0]}')
    if anchor['name'] is not None:
        if anchor['name'] != cand[1]:
            diffs.append(f"name: 锚库 {anchor['name']} vs 映射 {cand[1]}")
    if anchor['descriptor'] is not None:
        if anchor['descriptor'] != cand[2]:
            diffs.append(f"desc: 锚库 {anchor['descriptor']} vs 映射 {cand[2]}")
    if not diffs:
        return 'MATCH', '', False
    # 同义漂移：class 与 desc 均符、仅 name 不符
    drift = all(d.startswith('name:') for d in diffs) and anchor['name'] is not None
    return 'MISMATCH', '; '.join(diffs), drift

def main():
    off2named, named2off, members = load_tiny(ZIP_PATH)
    # desc 反写方向：official desc 类名 → named 类名（用于建 named 索引）
    to_named_desc = make_desc_converter(off2named)
    # 展示用：锚库 official desc → named 形式
    members_by_named = {}
    for ncls, ms in members.items():
        d = {}
        for kind, odesc, oname, nname in ms:
            ndesc = to_named_desc(odesc)
            d.setdefault((nname, ndesc), []).append((oname, odesc))
        members_by_named[ncls] = d

    anchor = json.loads(ANCHOR_PATH.read_text(encoding='utf-8'))
    targets = anchor['targets']

    # 完整性自检：对照表必须覆盖锚库全部槽位
    missing_sem = [s for s in targets if s not in SEMANTICS]
    extra_sem = [s for s in SEMANTICS if s not in targets]
    if missing_sem or extra_sem:
        print(f'[警告] 对照表与锚库槽位不一致 missing={missing_sem} extra={extra_sem}')

    results = []
    counts = {'MATCH': 0, 'MISMATCH': 0, 'MISSING': 0}
    drift_slots = []
    for slot, anc in targets.items():
        sem = SEMANTICS.get(slot)
        if sem is None:
            results.append((slot, anc, None, ('MISSING', '对照表未覆盖该槽位', False), None))
            counts['MISSING'] += 1
            continue
        cand, note = resolve_slot(slot, sem, named2off, off2named, members_by_named)
        status, detail, drift = diff_slot(anc, cand)
        if drift:
            drift_slots.append(slot)
        results.append((slot, anc, cand, (status, detail, drift), note))
        counts[status] += 1

    total = len(targets)
    cov = counts['MATCH'] / total * 100 if total else 0.0

    # ------------------------------------------------------------ 控制台输出
    print(f'== B2-1 对账：inf-20100327 锚库 targets({total} 槽) vs client.tiny ==')
    print(f'锚库: {ANCHOR_PATH}')
    print(f'语料: {ZIP_PATH} :: client.tiny（类 {len(off2named)}，成员行 {sum(len(v) for v in members.values())}）')
    print()
    for slot, anc, cand, (status, detail, drift), note in results:
        a = f"{anc['class']}.{anc['name'] or ''}{anc['descriptor'] or ''}"
        c = f"{cand[0]}.{cand[1] or ''}{cand[2] or ''}" if cand else '-'
        mark = '（同义漂移：仅 name 不符）' if drift else ''
        print(f'[{status:8}] {slot:18} 锚库 {a}')
        print(f'{"":13}映射 {c} {detail}{mark}')
    print()
    print(f'== 统计 ==')
    print(f"MATCH {counts['MATCH']} / MISMATCH {counts['MISMATCH']} / MISSING {counts['MISSING']} / 总 {total}")
    print(f'覆盖率 = MATCH/总槽位 = {counts["MATCH"]}/{total} = {cov:.1f}%')
    if counts['MISMATCH'] > 3:
        print('!! MISMATCH >3：锚库与 tiny 映射存在系统性分歧信号')

    # ------------------------------------------------------------ report.md
    L = []
    L.append('# B2-1 inf-20100327 tiny 映射对账报告')
    L.append('')
    L.append(f'- 生成时间：{datetime.now().strftime("%Y-%m-%d %H:%M:%S")}')
    L.append(f'- 锚库：`{ANCHOR_PATH}`（targets 共 {total} 槽，只读）')
    L.append(f'- 语料：`{ZIP_PATH}` 内 `client.tiny`（tiny v2，official→named，类 {len(off2named)} 条；只读）')
    L.append('- 方法：槽位→named 语义对照表（逐槽写死，来源=锚库 provenance+映射阅读）→ 经类映射解析为候选 official 三元组 → 与锚库逐槽 diff。')
    L.append('- 注：实测该 tiny 成员行仅带 official 侧 desc；named 侧 desc 由 official desc 经类映射反写得到。构造器不重命名，tiny 不列 World 的 `<init>` 行。')
    L.append('')
    L.append('## 摘要')
    L.append('')
    L.append(f'| 总槽位 | MATCH | MISMATCH | MISSING | 覆盖率 |')
    L.append(f'|---|---|---|---|---|')
    L.append(f'| {total} | {counts["MATCH"]} | {counts["MISMATCH"]} | {counts["MISSING"]} | {cov:.1f}% |')
    L.append('')
    L.append('## 逐槽对账表')
    L.append('')
    L.append('| 槽位 | 锚库 class | 锚库 name | 锚库 desc | named 语义 | 映射 official 三元组 | 状态 | 备注 |')
    L.append('|---|---|---|---|---|---|---|---|')
    for slot, anc, cand, (status, detail, drift), note in results:
        sem = SEMANTICS.get(slot)
        sem_str = f'{sem[1].rsplit("/", 1)[-1]}' + (f'.{sem[2]}{sem[3]}' if sem[0] != 'class' else '') if sem else '-'
        ctrip = f'`{cand[0]}` `{cand[1] or "-"}` `{cand[2] or "-"}`' if cand else '-'
        at = anc['class'].replace('.', '/')
        note_s = (note or '').split('；')[0]
        L.append(f"| {slot} | `{at}` | `{anc['name'] or '-'}` | `{anc['descriptor'] or '-'}` | {sem_str} | {ctrip} | {status} | {note_s}{'；' + detail if detail else ''} |")
    L.append('')
    L.append('## MISMATCH 明细')
    L.append('')
    if counts['MISMATCH'] == 0:
        L.append('无。')
    for slot, anc, cand, (status, detail, drift), note in results:
        if status == 'MISMATCH':
            L.append(f'- **{slot}**：{detail}' + ('　**[同义漂移：name 不符但 class/desc 符，A2 表可能钉错或有版本漂移]**' if drift else ''))
            L.append(f'  - named 语义：{SEMANTICS[slot][1]}.{SEMANTICS[slot][2]}{SEMANTICS[slot][3] or ""}（{note}）')
    L.append('')
    L.append('## MISSING 明细')
    L.append('')
    if counts['MISSING'] == 0:
        L.append('无。')
    for slot, anc, cand, (status, detail, drift), note in results:
        if status == 'MISSING':
            L.append(f'- **{slot}**：{note}')
    L.append('')
    L.append('## 结论')
    L.append('')
    L.append(f'- 覆盖率 {cov:.1f}%（{counts["MATCH"]}/{total}）。')
    if counts['MISMATCH'] > 3:
        L.append(f'- MISMATCH 共 {counts["MISMATCH"]} 槽（>3），构成锚库与 tiny 映射的系统性分歧信号，需人工复核 A2 表，不应据此擅自修正锚库。')
    else:
        L.append(f'- MISMATCH 共 {counts["MISMATCH"]} 槽（≤3），属孤立漂移/钉错信号而非系统性分歧；重点复核上述 MISMATCH 槽。')
    if drift_slots:
        L.append(f'- 同义漂移（name 不符、class+desc 符）槽位：{", ".join(drift_slots)}——A2 表 name 侧可能钉错或存在版本漂移。')
    L.append('- 本工具只读对账，未对锚库与语料做任何修改。')
    L.append('')
    REPORT_PATH.write_text('\n'.join(L), encoding='utf-8')
    print(f'\n报告已写入 {REPORT_PATH}')

if __name__ == '__main__':
    main()
