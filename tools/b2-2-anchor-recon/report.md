# B2-2 anchor-vs-tiny reverse recon (batch 1, 2026-11-20)

```text
=== inf-20100327 (schema=legacy, tiny=inf-20100327.zip, classes=234 members=1993) ===
reconciled slots: 74  {'MATCH': 70, 'NAME-GAP': 1, 'DESC-DIFF': 3}
  [MATCH] worldClass = net/minecraft/a/a/f.NoneNone -> class->net/minecraft/game/world/World
  [NAME-GAP] worldCtor = net/minecraft/a/a/f.<init>(Ljava/io/File;Ljava/lang/String;)V  tiny:
  [MATCH] setBlock = net/minecraft/a/a/f.c(IIII)Z -> setBlockWithNotify
  [MATCH] getBlockId = net/minecraft/a/a/f.a(III)I -> getBlockId
  [MATCH] saveWorld = net/minecraft/a/a/f.a(Z)V -> saveWorld
  [MATCH] worldTime = net/minecraft/a/a/f.bJ -> worldTime
  [MATCH] randSeed = net/minecraft/a/a/f.xJ -> randomSeed
  [MATCH] spawnX = net/minecraft/a/a/f.gI -> spawnX
  [MATCH] spawnY = net/minecraft/a/a/f.hI -> spawnY
  [MATCH] spawnZ = net/minecraft/a/a/f.iI -> spawnZ
  [MATCH] difficulty = net/minecraft/a/a/f.dI -> difficultySetting
  [MATCH] observers = net/minecraft/a/a/f.uLjava/util/List; -> worldAccesses
  [MATCH] playerField = net/minecraft/a/a/f.cLnet/minecraft/a/c/b; -> playerEntity
  [MATCH] getMeta = net/minecraft/a/a/f.d(III)I -> getBlockMetadata
  [MATCH] setMetaSilent = net/minecraft/a/a/f.b(IIII)V -> setBlockMetadataWithNotify
  [MATCH] light6 = net/minecraft/a/a/f.e(IIII)V -> notifyBlocksOfNeighborChange
  [DESC-DIFF] getLight = net/minecraft/a/a/f.f(III)I  tiny:m(IIII)V->scheduleBlockUpdate fLjava/util/Random;->rand m()V->saveWorldIndirectly
  [MATCH] explode = net/minecraft/a/a/f.a(Lnet/minecraft/a/c/b;DDDF)V -> createExplosion
  [MATCH] spawnEntity = net/minecraft/a/a/f.a(Lnet/minecraft/a/c/b;)V -> spawnEntityInWorld
  [DESC-DIFF] setEntityDead = net/minecraft/a/a/f.b(Lnet/minecraft/a/c/b;)V  tiny:m(Lnet/minecraft/a/a/a;)V->removeWorldAccess m(III)Z->blockExists m(Ljava/io/File;Ljava/lang/String;)V->deleteWorld m(II)Z->chunkExists m(IIII)V->setBlockMetadataWithNotify m()Z->isDaytime fJ->worldTime m(F)F->getCelestialAngle m(Lnet/minecraft/a/d/b;)Z->getIsAnyLiquid
  [MATCH] teGet = net/minecraft/a/a/f.h(III)Lnet/minecraft/a/a/b/a/a; -> getBlockTileEntity
  [MATCH] entityList = net/minecraft/a/a/f.i()Ljava/util/List; -> getLoadedEntityList
  [MATCH] playSound = net/minecraft/a/a/f.a(DDDLjava/lang/String;FF)V -> playSoundEffect
  [MATCH] entityClass = net/minecraft/a/c/b.NoneNone -> class->net/minecraft/game/entity/Entity
  [MATCH] setPosRot = net/minecraft/a/c/b.b(DDDFF)V -> setLocationAndAngles
  [MATCH] posX = net/minecraft/a/c/b.gD -> posX
  [MATCH] posY = net/minecraft/a/c/b.hD -> posY
  [MATCH] posZ = net/minecraft/a/c/b.iD -> posZ
  [MATCH] motX = net/minecraft/a/c/b.jD -> motionX
  [MATCH] motY = net/minecraft/a/c/b.kD -> motionY
  [MATCH] motZ = net/minecraft/a/c/b.lD -> motionZ
  [MATCH] yaw = net/minecraft/a/c/b.mF -> rotationYaw
  [MATCH] pitch = net/minecraft/a/c/b.nF -> rotationPitch
  [MATCH] iframes = net/minecraft/a/c/b.KI -> heartsLife
  [MATCH] entityType = net/minecraft/a/c/b.a()Ljava/lang/String; -> getEntityType
  [MATCH] entityLivingClass = net/minecraft/a/c/e.NoneNone -> class->net/minecraft/game/entity/EntityLiving
  [MATCH] health = net/minecraft/a/c/e.WI -> health
  [MATCH] entityHumanClass = net/minecraft/a/c/e/a.NoneNone -> class->net/minecraft/game/entity/player/EntityPlayer
  [MATCH] invField = net/minecraft/a/c/e/a.bLnet/minecraft/a/c/e/b; -> inventory
  [MATCH] inventoryPlayerClass = net/minecraft/a/c/e/b.NoneNone -> class->net/minecraft/game/entity/player/InventoryPlayer
  [MATCH] invMain = net/minecraft/a/c/e/b.a[Lnet/minecraft/a/b/k; -> mainInventory
  [MATCH] invArmor = net/minecraft/a/c/e/b.b[Lnet/minecraft/a/b/k; -> armorInventory
  [MATCH] invCur = net/minecraft/a/c/e/b.cI -> currentItem
  [DESC-DIFF] addStack = net/minecraft/a/c/e/b.a(Lnet/minecraft/a/b/k;)Z  tiny:m()I->getInventorySize m(II)Lnet/minecraft/a/b/k;->decrStackSize m(I)Lnet/minecraft/a/b/k;->getStackInSlot m(ILnet/minecraft/a/b/k;)V->setInventorySlotContents f[Lnet/minecraft/a/b/k;->mainInventory
  [MATCH] itemStackClass = net/minecraft/a/b/k.NoneNone -> class->net/minecraft/game/item/ItemStack
  [MATCH] isCount = net/minecraft/a/b/k.aI -> stackSize
  [MATCH] isId = net/minecraft/a/b/k.cI -> itemID
  [MATCH] isDmg = net/minecraft/a/b/k.dI -> itemDamage
  [MATCH] itemClass = net/minecraft/a/b/i.NoneNone -> class->net/minecraft/game/item/Item
  [MATCH] itemRegistry = net/minecraft/a/b/i.b[Lnet/minecraft/a/b/i; -> itemsList
  [MATCH] iInventoryClass = net/minecraft/a/b.NoneNone -> class->net/minecraft/game/IInventory
  [MATCH] invSize = net/minecraft/a/b.a()I -> getInventorySize
  [MATCH] invGet = net/minecraft/a/b.a(I)Lnet/minecraft/a/b/k; -> getStackInSlot
  [MATCH] invSet = net/minecraft/a/b.a(ILnet/minecraft/a/b/k;)V -> setInventorySlotContents
  [MATCH] tileEntityClass = net/minecraft/a/a/b/a/a.NoneNone -> class->net/minecraft/game/world/block/tileentity/TileEntity
  [MATCH] chestClass = net/minecraft/a/a/b/a/c.NoneNone -> class->net/minecraft/game/world/block/tileentity/TileEntityChest
  [MATCH] furnaceClass = net/minecraft/a/a/b/a/b.NoneNone -> class->net/minecraft/game/world/block/tileentity/TileEntityFurnace
  [MATCH] teBurn = net/minecraft/a/a/b/a/b.fI -> furnaceBurnTime
  [MATCH] teCook = net/minecraft/a/a/b/a/b.hI -> furnaceCookTime
  [MATCH] itemEntityClass = net/minecraft/a/c/c/b.NoneNone -> class->net/minecraft/game/entity/misc/EntityItem
  [MATCH] scatterClass = net/minecraft/a/a/c/b/b.NoneNone -> class->net/minecraft/game/world/terrain/generate/WorldGenMinable
  [MATCH] mobPig = net/minecraft/a/c/b/c.NoneNone -> class->net/minecraft/game/entity/animal/EntityPig
  [MATCH] mobSheep = net/minecraft/a/c/b/b.NoneNone -> class->net/minecraft/game/entity/animal/EntitySheep
  [MATCH] mobCreeper = net/minecraft/a/c/a/d.NoneNone -> class->net/minecraft/game/entity/monster/EntityCreeper
  [MATCH] mobSkeleton = net/minecraft/a/c/a/c.NoneNone -> class->net/minecraft/game/entity/monster/EntitySkeleton
  [MATCH] mobZombie = net/minecraft/a/c/a/f.NoneNone -> class->net/minecraft/game/entity/monster/EntityZombie
  [MATCH] mobSpider = net/minecraft/a/c/a/b.NoneNone -> class->net/minecraft/game/entity/monster/EntitySpider
  [MATCH] mobTnt = net/minecraft/a/c/c/a.NoneNone -> class->net/minecraft/game/entity/misc/EntityTNT
  [MATCH] mobGiant = net/minecraft/a/c/a/a.NoneNone -> class->net/minecraft/game/entity/monster/EntityGiant
  [MATCH] mobBase = net/minecraft/a/c/c.NoneNone -> class->net/minecraft/game/entity/EntityCreature
  [MATCH] minecraftClass = net/minecraft/client/d.NoneNone -> class->net/minecraft/client/Minecraft
  [MATCH] runTick = net/minecraft/client/d.h()V -> runTick
  [MATCH] guiIngame = net/minecraft/client/c/m.a(F)V -> renderGameOverlay
  [MATCH] chunkGenHeight = net/minecraft/a/a/e/d.a()V -> generateHeightMap

=== a1.2.0 (schema=legacy, tiny=a1.2.0-a1.2.1_01.zip, classes=445 members=4205) ===
anchor-null slots (intentional absence): 2
reconciled slots: 78  {'MATCH': 75, 'NAME-GAP': 3}
  [MATCH] worldClass = cu.NoneNone -> class->net/minecraft/src/World
  [MATCH] getBlockId = cu.a(III)I -> getBlockId
  [MATCH] setBlock = cu.d(IIII)Z -> setBlockWithNotify
  [MATCH] setBlockNoNotify = cu.a(IIII)Z -> setBlock
  [MATCH] getMeta = cu.e(III)I -> getBlockMetadata
  [MATCH] setMetaSilent = cu.b(IIII)V -> setBlockMetadataWithNotify
  [MATCH] getLight = cu.j(III)I -> getBlockLightValue
  [MATCH] saveWorld = cu.a(Lph;)V -> func_651_a
  [MATCH] teGet = cu.b(III)Liz; -> getBlockTileEntity
  [MATCH] teSet = cu.a(IIILiz;)V -> setBlockTileEntity
  [MATCH] worldTime = cu.eJ -> worldTime
  [MATCH] randSeed = cu.vJ -> randomSeed
  [MATCH] spawnX = cu.nI -> spawnX
  [MATCH] spawnY = cu.oI -> spawnY
  [MATCH] spawnZ = cu.pI -> spawnZ
  [MATCH] playerListField = cu.dLjava/util/List; -> playerEntities
  [MATCH] entityClass = lk.NoneNone -> class->net/minecraft/src/Entity
  [MATCH] explode = cu.a(Llk;DDDF)V -> createExplosion
  [MATCH] spawnEntity = cu.a(Llk;)Z -> entityJoinedWorld
  [MATCH] setEntityDead = cu.d(Llk;)V -> setEntityDead
  [MATCH] entityList = cu.k()Ljava/util/List; -> func_658_i
  [MATCH] playSound = cu.a(DDDLjava/lang/String;FF)V -> playSoundEffect
  [MATCH] difficulty = cu.kI -> difficultySetting
  [NAME-GAP] worldCtor = cu.<init>(Ljava/io/File;Ljava/lang/String;)V  tiny:
  [MATCH] minecraftClass = net/minecraft/client/Minecraft.NoneNone -> class->
  [MATCH] theWorldField = net/minecraft/client/Minecraft.eLcu; -> theWorld
  [MATCH] thePlayerField = net/minecraft/client/Minecraft.gLbo; -> thePlayer
  [MATCH] ingameGuiField = net/minecraft/client/Minecraft.uLmz; -> field_6308_u
  [MATCH] loadWorld = net/minecraft/client/Minecraft.a(Lcu;Ljava/lang/String;Ldv;)V -> func_6256_a
  [MATCH] runTick = net/minecraft/client/Minecraft.i()V -> func_6246_i
  [MATCH] setPosRot = lk.b(DDDFF)V -> setPositionAndRotation
  [MATCH] posX = lk.apD -> posX
  [MATCH] posY = lk.aqD -> posY
  [MATCH] posZ = lk.arD -> posZ
  [MATCH] motX = lk.asD -> motionX
  [MATCH] motY = lk.atD -> motionY
  [MATCH] motZ = lk.auD -> motionZ
  [MATCH] yaw = lk.avF -> rotationYaw
  [MATCH] pitch = lk.awF -> rotationPitch
  [MATCH] iframes = lk.aWI -> field_9311_be
  [MATCH] entityListClass = fj.NoneNone -> class->net/minecraft/src/EntityList
  [MATCH] entityListLookup = fj.b(Llk;)Ljava/lang/String; -> getEntityString
  [MATCH] entityLivingClass = gx.NoneNone -> class->net/minecraft/src/EntityLiving
  [MATCH] health = gx.JI -> field_9337_J
  [MATCH] entityPlayerClass = dv.NoneNone -> class->net/minecraft/src/EntityPlayer
  [MATCH] invField = dv.eLfh; -> inventory
  [MATCH] inventoryPlayerClass = fh.NoneNone -> class->net/minecraft/src/InventoryPlayer
  [MATCH] invMain = fh.a[Lfi; -> mainInventory
  [MATCH] invArmor = fh.b[Lfi; -> armorInventory
  [MATCH] invCur = fh.dI -> currentItem
  [MATCH] addStack = fh.a(Lfi;)Z -> func_504_a
  [MATCH] itemStackClass = fi.NoneNone -> class->net/minecraft/src/ItemStack
  [MATCH] isCount = fi.aI -> stackSize
  [MATCH] isId = fi.cI -> itemID
  [MATCH] isDmg = fi.dI -> itemDamage
  [MATCH] itemClass = dr.NoneNone -> class->net/minecraft/src/Item
  [MATCH] itemRegistry = dr.c[Ldr; -> itemsList
  [MATCH] itemEntityClass = ei.NoneNone -> class->net/minecraft/src/EntityItem
  [NAME-GAP] itemEntityCtor = ei.<init>(Lcu;DDDLfi;)V  tiny:
  [MATCH] tileEntityClass = iz.NoneNone -> class->net/minecraft/src/TileEntity
  [MATCH] guiIngame = mz.a(FZII)V -> renderGameOverlay
  [MATCH] mobZombie = nh.NoneNone -> class->net/minecraft/src/EntityZombie
  [MATCH] mobSpider = bc.NoneNone -> class->net/minecraft/src/EntitySpider
  [MATCH] mobSkeleton = dd.NoneNone -> class->net/minecraft/src/EntitySkeleton
  [MATCH] mobCreeper = dl.NoneNone -> class->net/minecraft/src/EntityCreeper
  [MATCH] mobPig = oc.NoneNone -> class->net/minecraft/src/EntityPig
  [MATCH] mobSheep = bu.NoneNone -> class->net/minecraft/src/EntitySheep
  [MATCH] mobCow = aq.NoneNone -> class->net/minecraft/src/EntityCow
  [MATCH] mobChicken = oh.NoneNone -> class->net/minecraft/src/EntityChicken
  [MATCH] mobTnt = kg.NoneNone -> class->net/minecraft/src/EntityTNTPrimed
  [NAME-GAP] mobTntCtor = kg.<init>(Lcu;FFF)V  tiny:
  [MATCH] mobGiant = ig.NoneNone -> class->net/minecraft/src/EntityZombieSimple
  [MATCH] mobCreatureClass = ex.NoneNone -> class->net/minecraft/src/EntityCreature
  [MATCH] worldChunkManagerClass = pb.NoneNone -> class->net/minecraft/src/WorldChunkManager
  [MATCH] getWorldChunkManager = cu.a()Lpb; -> func_4075_a
  [MATCH] biomeAt = pb.a(II)Lfy; -> func_4073_a
  [MATCH] biomeClass = fy.NoneNone -> class->net/minecraft/src/MobSpawnerBase
  [MATCH] biomeNameField = fy.mLjava/lang/String; -> biomeName

=== b1.2_02 (schema=legacy, tiny=b1.2.zip, classes=517 members=5070) ===
anchor-null slots (intentional absence): 3
reconciled slots: 79  {'MATCH': 75, 'NAME-GAP': 4}
  [MATCH] worldClass = dn.NoneNone -> class->net/minecraft/src/World
  [MATCH] getBlockId = dn.a(III)I -> getBlockId
  [MATCH] setBlock = dn.e(IIII)Z -> setBlockWithNotify
  [MATCH] setBlockNoNotify = dn.b(IIII)Z -> setBlock
  [MATCH] getMeta = dn.e(III)I -> getBlockMetadata
  [MATCH] setMetaSilent = dn.c(IIII)V -> setBlockMetadataWithNotify
  [MATCH] getLight = dn.l(III)I -> getBlockLightValue
  [MATCH] saveWorld = dn.a(Lru;)V -> func_651_a
  [MATCH] teGet = dn.b(III)Lkp; -> getBlockTileEntity
  [MATCH] teSet = dn.a(IIILkp;)V -> setBlockTileEntity
  [MATCH] worldTime = dn.eJ -> worldTime
  [MATCH] randSeed = dn.uJ -> randomSeed
  [MATCH] spawnX = dn.mI -> spawnX
  [MATCH] spawnY = dn.nI -> spawnY
  [MATCH] spawnZ = dn.oI -> spawnZ
  [MATCH] playerListField = dn.dLjava/util/List; -> playerEntities
  [MATCH] entityClass = nl.NoneNone -> class->net/minecraft/src/Entity
  [MATCH] explode = dn.a(Lnl;DDDF)Lmc; -> createExplosion
  [MATCH] spawnEntity = dn.a(Lnl;)Z -> entityJoinedWorld
  [MATCH] setEntityDead = dn.d(Lnl;)V -> setEntityDead
  [MATCH] entityList = dn.l()Ljava/util/List; -> getLoadedEntityList
  [MATCH] playSound = dn.a(DDDLjava/lang/String;FF)V -> playSoundEffect
  [MATCH] difficulty = dn.kI -> difficultySetting
  [NAME-GAP] worldCtor = dn.<init>(Ljava/io/File;Ljava/lang/String;)V  tiny:
  [MATCH] minecraftClass = net/minecraft/client/Minecraft.NoneNone -> class->net/minecraft/client/Minecraft
  [MATCH] theWorldField = net/minecraft/client/Minecraft.eLdn; -> theWorld
  [MATCH] thePlayerField = net/minecraft/client/Minecraft.gLbz; -> thePlayer
  [MATCH] ingameGuiField = net/minecraft/client/Minecraft.uLpe; -> ingameGUI
  [MATCH] loadWorld = net/minecraft/client/Minecraft.a(Ldn;Ljava/lang/String;Leu;)V -> changeWorld
  [MATCH] runTick = net/minecraft/client/Minecraft.i()V -> runTick
  [MATCH] setPosRot = nl.b(DDDFF)V -> setPositionAndRotation
  [MATCH] posX = nl.aFD -> posX
  [MATCH] posY = nl.aGD -> posY
  [MATCH] posZ = nl.aHD -> posZ
  [MATCH] motX = nl.aID -> motionX
  [MATCH] motY = nl.aJD -> motionY
  [MATCH] motZ = nl.aKD -> motionZ
  [MATCH] yaw = nl.aLF -> rotationYaw
  [MATCH] pitch = nl.aMF -> rotationPitch
  [MATCH] entityListClass = gn.NoneNone -> class->net/minecraft/src/EntityList
  [MATCH] entityListLookup = gn.b(Lnl;)Ljava/lang/String; -> getEntityString
  [MATCH] entityLivingClass = ig.NoneNone -> class->net/minecraft/src/EntityLiving
  [MATCH] health = ig.SI -> health
  [MATCH] entityPlayerClass = eu.NoneNone -> class->net/minecraft/src/EntityPlayer
  [MATCH] invField = eu.fLgl; -> inventory
  [MATCH] inventoryPlayerClass = gl.NoneNone -> class->net/minecraft/src/InventoryPlayer
  [MATCH] invMain = gl.a[Lgm; -> mainInventory
  [MATCH] invArmor = gl.b[Lgm; -> armorInventory
  [MATCH] invCur = gl.cI -> currentItem
  [MATCH] addStack = gl.a(Lgm;)Z -> addItemStackToInventory
  [MATCH] itemStackClass = gm.NoneNone -> class->net/minecraft/src/ItemStack
  [MATCH] isCount = gm.aI -> stackSize
  [MATCH] isId = gm.cI -> itemID
  [MATCH] isDmg = gm.dI -> itemDamage
  [NAME-GAP] itemStackCtor = gm.<init>(III)V  tiny:
  [MATCH] itemClass = eo.NoneNone -> class->net/minecraft/src/Item
  [MATCH] itemRegistry = eo.c[Leo; -> itemsList
  [MATCH] itemEntityClass = fh.NoneNone -> class->net/minecraft/src/EntityItem
  [NAME-GAP] itemEntityCtor = fh.<init>(Ldn;DDDLgm;)V  tiny:
  [MATCH] tileEntityClass = kp.NoneNone -> class->net/minecraft/src/TileEntity
  [MATCH] guiIngame = pe.a(FZII)V -> renderGameOverlay
  [MATCH] mobZombie = pn.NoneNone -> class->net/minecraft/src/EntityZombie
  [MATCH] mobSpider = bn.NoneNone -> class->net/minecraft/src/EntitySpider
  [MATCH] mobSkeleton = dy.NoneNone -> class->net/minecraft/src/EntitySkeleton
  [MATCH] mobCreeper = eh.NoneNone -> class->net/minecraft/src/EntityCreeper
  [MATCH] mobPig = qk.NoneNone -> class->net/minecraft/src/EntityPig
  [MATCH] mobSheep = ch.NoneNone -> class->net/minecraft/src/EntitySheep
  [MATCH] mobCow = bb.NoneNone -> class->net/minecraft/src/EntityCow
  [MATCH] mobChicken = qs.NoneNone -> class->net/minecraft/src/EntityChicken
  [MATCH] mobSquid = rm.NoneNone -> class->net/minecraft/src/EntitySquid
  [MATCH] mobTnt = mb.NoneNone -> class->net/minecraft/src/EntityTNTPrimed
  [NAME-GAP] mobTntCtor = mb.<init>(Ldn;DDD)V  tiny:
  [MATCH] mobGiant = jv.NoneNone -> class->net/minecraft/src/EntityZombieSimple
  [MATCH] mobCreatureClass = fz.NoneNone -> class->net/minecraft/src/EntityCreature
  [MATCH] worldChunkManagerClass = ro.NoneNone -> class->net/minecraft/src/WorldChunkManager
  [MATCH] getWorldChunkManager = dn.a()Lro; -> getWorldChunkManager
  [MATCH] biomeAt = ro.a(II)Lhf; -> func_4073_a
  [MATCH] biomeClass = hf.NoneNone -> class->net/minecraft/src/MobSpawnerBase
  [MATCH] biomeNameField = hf.mLjava/lang/String; -> biomeName

=== b1.7.3 (schema=runtime, tiny=b1.7.zip, classes=677 members=4836) ===
ops bare-name slots (unreconcilable offline, F-2): 54
reconciled slots: 4  {'MATCH': 4}
  [MATCH] SAVE_WORLD = fd.a(Lyb;)V -> saveWorldIndirectly
  [MATCH] LOAD_WORLD = net/minecraft/client/Minecraft.a(Lfd;Ljava/lang/String;Lgs;)V -> changeWorld
  [MATCH] RUN_TICK = net/minecraft/client/Minecraft.k()V -> runTick
  [MATCH] HUD_RENDER = uq.a(FZII)V -> renderGameOverlay

=== b1.8.1 (schema=runtime, tiny=b1.8.zip, classes=811 members=5754) ===
ops bare-name slots (unreconcilable offline, F-2): 54
reconciled slots: 4  {'MATCH': 4}
  [MATCH] SAVE_WORLD = rv.a(Lny;)V -> saveWorldIndirectly
  [MATCH] LOAD_WORLD = net/minecraft/client/Minecraft.a(Lrv;Ljava/lang/String;Lsz;)V -> changeWorld
  [MATCH] RUN_TICK = net/minecraft/client/Minecraft.k()V -> runTick
  [MATCH] HUD_RENDER = abj.a(FZII)V -> renderGameOverlay

```

## Verdicts (adjudicated 2026-11-20, post-fix rerun)

Jar SHA1: all five live jars match anchor pins (47b1/8632/551b/43db/6b56) -- PASS.

1. ctor NAME-GAP x8 (worldCtor/itemStackCtor/itemEntityCtor/mobTntCtor across versions): tiny v2 does
   not map <init>. Expected, not a gap. Excluded from drift accounting.
2. 0327 DESC-DIFF x3 = tiny gaps, anchors correct (runtime-pinned + live verified):
   getLight f(III)I and setEntityDead b(LEntity;)V (known since b2-1 batch),
   addStack a(LItemStack;)Z = NEW finding (give-1 live proof stands; tiny InventoryPlayer incomplete).
3. b1.2_02 explode DESC-DIFF = ANCHOR ERROR, FIXED this batch:
   real method = public mc a(nl,DDDF) i.e. (Lnl;DDDF)Lmc; (javap direct read).
   Descriptor field was recorded V. Runtime unaffected (WorldOps getMethod matches name+paramTypes,
   return type not consulted; legacy JSON is recon asset, not runtime-loaded). Hygiene fix only.
4. F-2 structural finding: runtime schema (b1.7.3/b1.8.1) ops = 54 bare-name slots without
   class/desc context -> offline reconciliation impossible (only 4 hooks coverable).
   No functional risk (full live matrices el173-3/el120-5), but violates B1 self-describing-triple
   discipline and blocks F1 compiler / F5 strict-conformance golden runs.
   PROPOSAL (awaiting approval): unify anchor format to self-describing triples in F1 batch.
