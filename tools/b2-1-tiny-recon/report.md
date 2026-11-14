# B2-1 inf-20100327 tiny 映射对账报告

- 生成时间：2026-11-14 21:03:27
- 锚库：`e:\4k\github-projects\MCWorldAgent\java\agent\registry\fingerprints\inf-20100327.json`（targets 共 74 槽，只读）
- 语料：`C:\Users\GAME\AppData\Local\Temp\mcaut\retromcp\pages\versions\inf-20100327.zip` 内 `client.tiny`（tiny v2，official→named，类 234 条；只读）
- 方法：槽位→named 语义对照表（逐槽写死，来源=锚库 provenance+映射阅读）→ 经类映射解析为候选 official 三元组 → 与锚库逐槽 diff。
- 注：实测该 tiny 成员行仅带 official 侧 desc；named 侧 desc 由 official desc 经类映射反写得到。构造器不重命名，tiny 不列 World 的 `<init>` 行。

## 摘要

| 总槽位 | MATCH | MISMATCH | MISSING | 覆盖率 |
|---|---|---|---|---|
| 74 | 69 | 2 | 3 | 93.2% |

## 逐槽对账表

| 槽位 | 锚库 class | 锚库 name | 锚库 desc | named 语义 | 映射 official 三元组 | 状态 | 备注 |
|---|---|---|---|---|---|---|---|
| worldClass | `net/minecraft/a/a/f` | `-` | `-` | World | `net/minecraft/a/a/f` `-` `-` | MATCH | 锚库称 World 宿主类 |
| worldCtor | `net/minecraft/a/a/f` | `<init>` | `(Ljava/io/File;Ljava/lang/String;)V` | World.<init>(Ljava/io/File;Ljava/lang/String;)V | - | MISSING | provenance: World.<init>(File,String) 0x0001；映射无此槽 |
| setBlock | `net/minecraft/a/a/f` | `c` | `(IIII)Z` | World.setBlockWithNotify(IIII)Z | `net/minecraft/a/a/f` `c` `(IIII)Z` | MATCH | provenance: setBlockWithNotify (IIII)Z public final |
| getBlockId | `net/minecraft/a/a/f` | `a` | `(III)I` | World.getBlockId(III)I | `net/minecraft/a/a/f` `a` `(III)I` | MATCH | provenance: getBlockId (III)I |
| saveWorld | `net/minecraft/a/a/f` | `a` | `(Z)V` | World.saveWorld(Z)V | `net/minecraft/a/a/f` `a` `(Z)V` | MATCH | provenance: saveWorld (Z)V private |
| worldTime | `net/minecraft/a/a/f` | `b` | `J` | World.worldTimeJ | `net/minecraft/a/a/f` `b` `J` | MATCH | provenance: NBT "Time" 键绑定 |
| randSeed | `net/minecraft/a/a/f` | `x` | `J` | World.randomSeedJ | `net/minecraft/a/a/f` `x` `J` | MATCH | provenance: NBT "RandomSeed" 绑定 |
| spawnX | `net/minecraft/a/a/f` | `g` | `I` | World.spawnXI | `net/minecraft/a/a/f` `g` `I` | MATCH | provenance: NBT "SpawnX" |
| spawnY | `net/minecraft/a/a/f` | `h` | `I` | World.spawnYI | `net/minecraft/a/a/f` `h` `I` | MATCH | provenance: NBT "SpawnY" |
| spawnZ | `net/minecraft/a/a/f` | `i` | `I` | World.spawnZI | `net/minecraft/a/a/f` `i` `I` | MATCH | provenance: NBT "SpawnZ" |
| difficulty | `net/minecraft/a/a/f` | `d` | `I` | World.difficultySettingI | `net/minecraft/a/a/f` `d` `I` | MATCH | provenance: F_DIFFICULTY=d meta 轮 javap 取证 |
| observers | `net/minecraft/a/a/f` | `u` | `Ljava/util/List;` | World.worldAccessesLjava/util/List; | `net/minecraft/a/a/f` `u` `Ljava/util/List;` | MATCH | provenance: private List 渲染观察者 |
| playerField | `net/minecraft/a/a/f` | `c` | `Lnet/minecraft/a/c/b;` | World.playerEntityLnet/minecraft/game/entity/Entity; | `net/minecraft/a/a/f` `c` `Lnet/minecraft/a/c/b;` | MATCH | provenance: EntityPlayer 构造器自设 world.c=this |
| getMeta | `net/minecraft/a/a/f` | `d` | `(III)I` | World.getBlockMetadata(III)I | `net/minecraft/a/a/f` `d` `(III)I` | MATCH | provenance: Chunk.b(III)I 读 nibble 链 |
| setMetaSilent | `net/minecraft/a/a/f` | `b` | `(IIII)V` | World.setBlockMetadataWithNotify(IIII)V | `net/minecraft/a/a/f` `b` `(IIII)V` | MATCH | provenance: 静默 meta 写，系 deobf 误猜名，以字节码为准 |
| light6 | `net/minecraft/a/a/f` | `e` | `(IIII)V` | World.notifyBlocksOfNeighborChange(IIII)V | `net/minecraft/a/a/f` `e` `(IIII)V` | MATCH | provenance: 光照 6 邻更新 |
| getLight | `net/minecraft/a/a/f` | `f` | `(III)I` | World.getBrightness(III)F | `net/minecraft/a/a/f` `g` `(III)F` | MISMATCH | provenance: getBrightness 翻案钉死；name: 锚库 f vs 映射 g; desc: 锚库 (III)I vs 映射 (III)F |
| explode | `net/minecraft/a/a/f` | `a` | `(Lnet/minecraft/a/c/b;DDDF)V` | World.createExplosion(Lnet/minecraft/game/entity/Entity;DDDF)V | `net/minecraft/a/a/f` `a` `(Lnet/minecraft/a/c/b;DDDF)V` | MATCH | provenance: createExplosion (Entity,DDDF)V 双路交叉 |
| spawnEntity | `net/minecraft/a/a/f` | `a` | `(Lnet/minecraft/a/c/b;)V` | World.spawnEntityInWorld(Lnet/minecraft/game/entity/Entity;)V | `net/minecraft/a/a/f` `a` `(Lnet/minecraft/a/c/b;)V` | MATCH | provenance: spawnEntityInWorld (Entity)V=loadedEntityList.add |
| setEntityDead | `net/minecraft/a/a/f` | `b` | `(Lnet/minecraft/a/c/b;)V` | World.setEntityDead(Lnet/minecraft/game/entity/Entity;)V | - | MISSING | provenance: setEntityDead (Entity)V static（grow-clear 批）；映射无此槽 |
| teGet | `net/minecraft/a/a/f` | `h` | `(III)Lnet/minecraft/a/a/b/a/a;` | World.getBlockTileEntity(III)Lnet/minecraft/game/world/block/tileentity/TileEntity; | `net/minecraft/a/a/f` `h` `(III)Lnet/minecraft/a/a/b/a/a;` | MATCH | provenance: tileentity 批双路 |
| entityList | `net/minecraft/a/a/f` | `i` | `()Ljava/util/List;` | World.getLoadedEntityList()Ljava/util/List; | `net/minecraft/a/a/f` `i` `()Ljava/util/List;` | MATCH | provenance: getLoadedEntityList()List |
| playSound | `net/minecraft/a/a/f` | `a` | `(DDDLjava/lang/String;FF)V` | World.playSoundEffect(DDDLjava/lang/String;FF)V | `net/minecraft/a/a/f` `a` `(DDDLjava/lang/String;FF)V` | MATCH | provenance: 参数序由 createExplosion 调用点实锤 |
| entityClass | `net/minecraft/a/c/b` | `-` | `-` | Entity | `net/minecraft/a/c/b` `-` `-` | MATCH | provenance: Entity 基类 a.c.b（全量反汇编 303 类） |
| setPosRot | `net/minecraft/a/c/b` | `b` | `(DDDFF)V` | Entity.setLocationAndAngles(DDDFF)V | `net/minecraft/a/c/b` `b` `(DDDFF)V` | MATCH | provenance 写作 setPositionAndRotation |
| posX | `net/minecraft/a/c/b` | `g` | `D` | Entity.posXD | `net/minecraft/a/c/b` `g` `D` | MATCH | provenance: d/e/f=lastTick、g/h/i=pos javap 布局钉 |
| posY | `net/minecraft/a/c/b` | `h` | `D` | Entity.posYD | `net/minecraft/a/c/b` `h` `D` | MATCH | provenance: E_POS_Y=h |
| posZ | `net/minecraft/a/c/b` | `i` | `D` | Entity.posZD | `net/minecraft/a/c/b` `i` `D` | MATCH | provenance: E_POS_Z=i |
| motX | `net/minecraft/a/c/b` | `j` | `D` | Entity.motionXD | `net/minecraft/a/c/b` `j` `D` | MATCH | provenance: E_MOT_X=j |
| motY | `net/minecraft/a/c/b` | `k` | `D` | Entity.motionYD | `net/minecraft/a/c/b` `k` `D` | MATCH | provenance: E_MOT_Y=k |
| motZ | `net/minecraft/a/c/b` | `l` | `D` | Entity.motionZD | `net/minecraft/a/c/b` `l` `D` | MATCH | provenance: E_MOT_Z=l |
| yaw | `net/minecraft/a/c/b` | `m` | `F` | Entity.rotationYawF | `net/minecraft/a/c/b` `m` `F` | MATCH | provenance: E_YAW=m |
| pitch | `net/minecraft/a/c/b` | `n` | `F` | Entity.rotationPitchF | `net/minecraft/a/c/b` `n` `F` | MATCH | provenance: E_PITCH=n |
| iframes | `net/minecraft/a/c/b` | `K` | `I` | Entity.heartsLifeI | `net/minecraft/a/c/b` `K` `I` | MATCH | provenance: 全伤害源单入口判决（sed-1 批） |
| entityType | `net/minecraft/a/c/b` | `q` | `()Ljava/lang/String;` | Entity.getEntityType()Ljava/lang/String; | `net/minecraft/a/c/b` `a` `()Ljava/lang/String;` | MISMATCH | provenance: getEntityType()String，注"与 AABB 字段 q 同名合法"；name: 锚库 q vs 映射 a |
| entityLivingClass | `net/minecraft/a/c/e` | `-` | `-` | EntityLiving | `net/minecraft/a/c/e` `-` `-` | MATCH | provenance: EntityLiving=a.c.e |
| health | `net/minecraft/a/c/e` | `W` | `I` | EntityLiving.healthI | `net/minecraft/a/c/e` `W` `I` | MATCH | provenance: NBT "Health" 双向绑定实锤 |
| entityHumanClass | `net/minecraft/a/c/e/a` | `-` | `-` | EntityPlayer | `net/minecraft/a/c/e/a` `-` `-` | MATCH | provenance: EntityHuman=a.c.e.a |
| invField | `net/minecraft/a/c/e/a` | `b` | `Lnet/minecraft/a/c/e/b;` | EntityPlayer.inventoryLnet/minecraft/game/entity/player/InventoryPlayer; | `net/minecraft/a/c/e/a` `b` `Lnet/minecraft/a/c/e/b;` | MATCH | provenance: EntityHuman.b=InventoryPlayer |
| inventoryPlayerClass | `net/minecraft/a/c/e/b` | `-` | `-` | InventoryPlayer | `net/minecraft/a/c/e/b` `-` `-` | MATCH | provenance: InventoryPlayer=a.c.e.b |
| invMain | `net/minecraft/a/c/e/b` | `a` | `[Lnet/minecraft/a/b/k;` | InventoryPlayer.mainInventory[Lnet/minecraft/game/item/ItemStack; | `net/minecraft/a/c/e/b` `a` `[Lnet/minecraft/a/b/k;` | MATCH | provenance: ItemStack[36] 主槽 |
| invArmor | `net/minecraft/a/c/e/b` | `b` | `[Lnet/minecraft/a/b/k;` | InventoryPlayer.armorInventory[Lnet/minecraft/game/item/ItemStack; | `net/minecraft/a/c/e/b` `b` `[Lnet/minecraft/a/b/k;` | MATCH | provenance: ItemStack[4] 盔甲槽 |
| invCur | `net/minecraft/a/c/e/b` | `c` | `I` | InventoryPlayer.currentItemI | `net/minecraft/a/c/e/b` `c` `I` | MATCH | provenance: 当前快捷槽 |
| addStack | `net/minecraft/a/c/e/b` | `a` | `(Lnet/minecraft/a/b/k;)Z` | InventoryPlayer.addStack(Lnet/minecraft/game/item/ItemStack;)Z | - | MISSING | provenance: addStack (ItemStack)Z 合并语义 javap 钉；映射无此槽 |
| itemStackClass | `net/minecraft/a/b/k` | `-` | `-` | ItemStack | `net/minecraft/a/b/k` `-` `-` | MATCH | provenance: ItemStack=a.b.k |
| isCount | `net/minecraft/a/b/k` | `a` | `I` | ItemStack.stackSizeI | `net/minecraft/a/b/k` `a` `I` | MATCH | provenance: IS_COUNT=a |
| isId | `net/minecraft/a/b/k` | `c` | `I` | ItemStack.itemIDI | `net/minecraft/a/b/k` `c` `I` | MATCH | provenance: IS_ID=c |
| isDmg | `net/minecraft/a/b/k` | `d` | `I` | ItemStack.itemDamageI | `net/minecraft/a/b/k` `d` `I` | MATCH | provenance: IS_DMG=d |
| itemClass | `net/minecraft/a/b/i` | `-` | `-` | Item | `net/minecraft/a/b/i` `-` `-` | MATCH | provenance: Item=a.b.i（注册表判决） |
| itemRegistry | `net/minecraft/a/b/i` | `b` | `[Lnet/minecraft/a/b/i;` | Item.itemsList[Lnet/minecraft/game/item/Item; | `net/minecraft/a/b/i` `b` `[Lnet/minecraft/a/b/i;` | MATCH | provenance: public static i[] 按 id 索引 |
| iInventoryClass | `net/minecraft/a/b` | `-` | `-` | IInventory | `net/minecraft/a/b` `-` `-` | MATCH | provenance: IInventory=a.b |
| invSize | `net/minecraft/a/b` | `a` | `()I` | IInventory.getInventorySize()I | `net/minecraft/a/b` `a` `()I` | MATCH | provenance: M_INV_SIZE=a |
| invGet | `net/minecraft/a/b` | `a` | `(I)Lnet/minecraft/a/b/k;` | IInventory.getStackInSlot(I)Lnet/minecraft/game/item/ItemStack; | `net/minecraft/a/b` `a` `(I)Lnet/minecraft/a/b/k;` | MATCH | provenance: M_INV_GET=a |
| invSet | `net/minecraft/a/b` | `a` | `(ILnet/minecraft/a/b/k;)V` | IInventory.setInventorySlotContents(ILnet/minecraft/game/item/ItemStack;)V | `net/minecraft/a/b` `a` `(ILnet/minecraft/a/b/k;)V` | MATCH | provenance: M_INV_SET=a 直替换无合并 |
| tileEntityClass | `net/minecraft/a/a/b/a/a` | `-` | `-` | TileEntity | `net/minecraft/a/a/b/a/a` `-` `-` | MATCH | provenance: TE 基类=a.a.b.a.a |
| chestClass | `net/minecraft/a/a/b/a/c` | `-` | `-` | TileEntityChest | `net/minecraft/a/a/b/a/c` `-` `-` | MATCH | provenance: Chest=a.a.b.a.c（27 槽） |
| furnaceClass | `net/minecraft/a/a/b/a/b` | `-` | `-` | TileEntityFurnace | `net/minecraft/a/a/b/a/b` `-` `-` | MATCH | provenance: Furnace=a.a.b.a.b（3 槽） |
| teBurn | `net/minecraft/a/a/b/a/b` | `f` | `I` | TileEntityFurnace.furnaceBurnTimeI | `net/minecraft/a/a/b/a/b` `f` `I` | MATCH | provenance: NBT BurnTime 对榫实锤 |
| teCook | `net/minecraft/a/a/b/a/b` | `h` | `I` | TileEntityFurnace.furnaceCookTimeI | `net/minecraft/a/a/b/a/b` `h` `I` | MATCH | provenance: NBT CookTime 对榫实锤 |
| itemEntityClass | `net/minecraft/a/c/c/b` | `-` | `-` | EntityItem | `net/minecraft/a/c/c/b` `-` `-` | MATCH | provenance: EntityItem，字符串常量判决 |
| scatterClass | `net/minecraft/a/a/c/b/b` | `-` | `-` | WorldGenMinable | `net/minecraft/a/a/c/b/b` `-` `-` | MATCH | provenance: 原版矿石散布器（meta-1 实跑翻案） |
| mobPig | `net/minecraft/a/c/b/c` | `-` | `-` | EntityPig | `net/minecraft/a/c/b/c` `-` `-` | MATCH | MOB_CLASSES |
| mobSheep | `net/minecraft/a/c/b/b` | `-` | `-` | EntitySheep | `net/minecraft/a/c/b/b` `-` `-` | MATCH | MOB_CLASSES |
| mobCreeper | `net/minecraft/a/c/a/d` | `-` | `-` | EntityCreeper | `net/minecraft/a/c/a/d` `-` `-` | MATCH | MOB_CLASSES |
| mobSkeleton | `net/minecraft/a/c/a/c` | `-` | `-` | EntitySkeleton | `net/minecraft/a/c/a/c` `-` `-` | MATCH | MOB_CLASSES |
| mobZombie | `net/minecraft/a/c/a/f` | `-` | `-` | EntityZombie | `net/minecraft/a/c/a/f` `-` `-` | MATCH | MOB_CLASSES |
| mobSpider | `net/minecraft/a/c/a/b` | `-` | `-` | EntitySpider | `net/minecraft/a/c/a/b` `-` `-` | MATCH | MOB_CLASSES |
| mobTnt | `net/minecraft/a/c/c/a` | `-` | `-` | EntityTNT | `net/minecraft/a/c/c/a` `-` `-` | MATCH | MOB_CLASSES，fuse=80 实锤 |
| mobGiant | `net/minecraft/a/c/a/a` | `-` | `-` | EntityGiant | `net/minecraft/a/c/a/a` `-` `-` | MATCH | MOB_CLASSES |
| mobBase | `net/minecraft/a/c/c` | `-` | `-` | EntityCreature | `net/minecraft/a/c/c` `-` `-` | MATCH | MOB_CLASSES，a.c.c=EntityCreature 基类 |
| minecraftClass | `net/minecraft/client/d` | `-` | `-` | Minecraft | `net/minecraft/client/d` `-` `-` | MATCH | HookTargets: RUN_TICK 宿主 client.d |
| runTick | `net/minecraft/client/d` | `h` | `()V` | Minecraft.runTick()V | `net/minecraft/client/d` `h` `()V` | MATCH | HookTargets: Minecraft.runTick() h()V 0x0002 |
| guiIngame | `net/minecraft/client/c/m` | `a` | `(F)V` | GuiIngame.renderGameOverlay(F)V | `net/minecraft/client/c/m` `a` `(F)V` | MATCH | HookTargets: HUD_RENDER，GuiIngame=client.c.m |
| chunkGenHeight | `net/minecraft/a/a/e/d` | `a` | `()V` | Chunk.generateHeightMap()V | `net/minecraft/a/a/e/d` `a` `()V` | MATCH | HookTargets: GEN_HEIGHT，Chunk.generateHeightMap() a()V 0x0011 |

## MISMATCH 明细

- **getLight**：name: 锚库 f vs 映射 g; desc: 锚库 (III)I vs 映射 (III)F
  - named 语义：net/minecraft/game/world/World.getBrightness(III)F（provenance: getBrightness 翻案钉死；映射实际 desc 为 (III)F）
- **entityType**：name: 锚库 q vs 映射 a　**[同义漂移：name 不符但 class/desc 符，A2 表可能钉错或有版本漂移]**
  - named 语义：net/minecraft/game/entity/Entity.getEntityType()Ljava/lang/String;（provenance: getEntityType()String，注"与 AABB 字段 q 同名合法"；映射中 q()String 实为 getEntityTexture）

## MISSING 明细

- **worldCtor**：provenance: World.<init>(File,String) 0x0001；构造器不重命名，tiny 不列 World 的 <init> 行，映射侧无法核验
- **setEntityDead**：provenance: setEntityDead (Entity)V static（grow-clear 批）；映射 World 侧无此 named 方法（Entity 上有 ()V 实例版），无法核验
- **addStack**：provenance: addStack (ItemStack)Z 合并语义 javap 钉；映射 InventoryPlayer 侧无此 named 方法，无法核验

## 结论

- 覆盖率 93.2%（69/74）。
- MISMATCH 共 2 槽（≤3），属孤立漂移/钉错信号而非系统性分歧；重点复核上述 MISMATCH 槽。
- 同义漂移（name 不符、class+desc 符）槽位：entityType——A2 表 name 侧可能钉错或存在版本漂移。
- 本工具只读对账，未对锚库与语料做任何修改。
