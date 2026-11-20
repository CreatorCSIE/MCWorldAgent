# F5 reconciliation report (F1 slow-path compiler vs anchor library)

Facets: getBlockId (III)I / setBlock (IIII)Z / saveWorld (era-dependent form). Verdicts: MATCH = class+name+descriptor all equal; MISS = differs (see oracle audit); SKIPPED = jar missing or SHA1 gate failed.

## Matrix

| version | family | World | getBlockId | setBlock | saveWorld |
|---|---|---|---|---|---|
| a1.1.2_01 | G3 | cn | MATCH | MATCH | MATCH |
| a1.2.0 | G3 | cu | MATCH | MATCH | MATCH |
| a1.2.6 | G3 | cy | MATCH | MATCH | MATCH |
| b1.2_02 | G3 | dn | MATCH | MATCH | MATCH |
| b1.7.3 | G3 | fd | MATCH | MATCH | MATCH |
| b1.8.1 | G3 | rv | MATCH | MATCH | MATCH |
| c0.30-c | G1 | com/mojang/minecraft/level/Level | MATCH | MATCH | MATCH |
| in-20100223 | G2 | net/minecraft/a/a/g | MATCH | MATCH | MATCH |
| inf-20100327 | G2 | net/minecraft/a/a/f | MATCH | MATCH | MATCH |
| inf-20100618 | G2 | net/minecraft/a/a/g | MATCH | MATCH | MATCH |
| inf-20100629 | G3 | ez | MATCH | MATCH | MATCH |

## Summary

- facets compared: 33, MATCH: 33, MISS: 0, SKIPPED versions: 0
- SUSPECT (anchor library pending human adjudication; compiler did NOT modify any JSON):
  - none
- note: 0327 and 0618 are structurally isomorphic for saveWorld (public final ()V wrapper delegating with iconst_1 to a private (Z)V level.dat core; identical call-graph shape). The detailed tables pin them at different hook levels (0327=core a(Z)V per phase1 WorldOps, 0618=wrapper f()V per verscan). No generalizable rule can reproduce both; this compiler follows the phase1 discipline (core preferred) and reports 0618 as a hook-level SUSPECT, not a rule failure.

## Per-version evidence chains

### a1.1.2_01 (family G3, World=cn)

- world: probe scan (G3 group: level.dat,SpawnX,RandomSeed,TickNextTick list out of synch,Saving chunks,Saving level) -> shape-validated candidates: cn(hits=6:level.dat,SpawnX,RandomSeed,TickNextTick list out of synch,Saving chunks,Saving level)
- **getBlockId** verdict=MATCH confidence=HIGH
  - oracle: {"class": "cn", "name": "a", "descriptor": "(III)I"}
  - compiled: {"class": "cn", "name": "a", "descriptor": "(III)I"}
  - candidates: public non-static (III)I on cn = [a, e, j]
  - a(III)I: tail-calls chunk getter ga.a(III)I which does direct baload (no nibble delegation) -> block-id
  - e(III)I: callee pattern b(III)I does not match unsigned-byte chunk getter -> rejected (getMeta/getLight shape)
  - j(III)I: callee pattern none does not match unsigned-byte chunk getter -> rejected (getMeta/getLight shape)
- **setBlock** verdict=MATCH confidence=HIGH
  - oracle: {"class": "cn", "name": "d", "descriptor": "(IIII)Z"}
  - compiled: {"class": "cn", "name": "d", "descriptor": "(IIII)Z"}
  - candidates: public non-static (IIII)Z on cn = [a, c, d, j, k]
  - a: no sibling (IIII)Z delegation -> silent core / metadata setter
  - c: no sibling (IIII)Z delegation -> silent core / metadata setter
  - d: delegates to sibling (IIII)Z core a -> wrapper
  - j: no sibling (IIII)Z delegation -> silent core / metadata setter
  - k: no sibling (IIII)Z delegation -> silent core / metadata setter
- **saveWorld** verdict=MATCH confidence=HIGH
  - oracle: {"class": "cn", "name": "a", "descriptor": "(Lnu;)V"}
  - compiled: {"class": "cn", "name": "a", "descriptor": "(Lnu;)V"}
  - rule A: public wrapper a(Lnu;)V delegates to core a(ZLnu;)V carrying save evidence 'Saving level'

### a1.2.0 (family G3, World=cu)

- world: probe scan (G3 group: level.dat,SpawnX,RandomSeed,TickNextTick list out of synch,Saving chunks,Saving level) -> shape-validated candidates: cu(hits=6:level.dat,SpawnX,RandomSeed,TickNextTick list out of synch,Saving chunks,Saving level)
- **getBlockId** verdict=MATCH confidence=HIGH
  - oracle: {"class": "cu", "name": "a", "descriptor": "(III)I"}
  - compiled: {"class": "cu", "name": "a", "descriptor": "(III)I"}
  - candidates: public non-static (III)I on cu = [a, e, j]
  - a(III)I: tail-calls chunk getter gs.a(III)I which does direct baload (no nibble delegation) -> block-id
  - e(III)I: callee pattern b(III)I does not match unsigned-byte chunk getter -> rejected (getMeta/getLight shape)
  - j(III)I: callee pattern none does not match unsigned-byte chunk getter -> rejected (getMeta/getLight shape)
- **setBlock** verdict=MATCH confidence=HIGH
  - oracle: {"class": "cu", "name": "d", "descriptor": "(IIII)Z"}
  - compiled: {"class": "cu", "name": "d", "descriptor": "(IIII)Z"}
  - candidates: public non-static (IIII)Z on cu = [a, c, d, j, k]
  - a: no sibling (IIII)Z delegation -> silent core / metadata setter
  - c: no sibling (IIII)Z delegation -> silent core / metadata setter
  - d: delegates to sibling (IIII)Z core a -> wrapper
  - j: no sibling (IIII)Z delegation -> silent core / metadata setter
  - k: no sibling (IIII)Z delegation -> silent core / metadata setter
- **saveWorld** verdict=MATCH confidence=HIGH
  - oracle: {"class": "cu", "name": "a", "descriptor": "(Lph;)V"}
  - compiled: {"class": "cu", "name": "a", "descriptor": "(Lph;)V"}
  - rule A: public wrapper a(Lph;)V delegates to core a(ZLph;)V carrying save evidence 'Saving level'

### a1.2.6 (family G3, World=cy)

- world: probe scan (G3 group: level.dat,SpawnX,RandomSeed,TickNextTick list out of synch,Saving chunks,Saving level) -> shape-validated candidates: cy(hits=6:level.dat,SpawnX,RandomSeed,TickNextTick list out of synch,Saving chunks,Saving level)
- **getBlockId** verdict=MATCH confidence=HIGH
  - oracle: {"class": "cy", "name": "a", "descriptor": "(III)I"}
  - compiled: {"class": "cy", "name": "a", "descriptor": "(III)I"}
  - candidates: public non-static (III)I on cy = [a, e, j]
  - a(III)I: tail-calls chunk getter ha.a(III)I which does direct baload (no nibble delegation) -> block-id
  - e(III)I: callee pattern b(III)I does not match unsigned-byte chunk getter -> rejected (getMeta/getLight shape)
  - j(III)I: callee pattern none does not match unsigned-byte chunk getter -> rejected (getMeta/getLight shape)
- **setBlock** verdict=MATCH confidence=HIGH
  - oracle: {"class": "cy", "name": "d", "descriptor": "(IIII)Z"}
  - compiled: {"class": "cy", "name": "d", "descriptor": "(IIII)Z"}
  - candidates: public non-static (IIII)Z on cy = [a, c, d, j, k]
  - a: no sibling (IIII)Z delegation -> silent core / metadata setter
  - c: no sibling (IIII)Z delegation -> silent core / metadata setter
  - d: delegates to sibling (IIII)Z core a -> wrapper
  - j: no sibling (IIII)Z delegation -> silent core / metadata setter
  - k: no sibling (IIII)Z delegation -> silent core / metadata setter
- **saveWorld** verdict=MATCH confidence=HIGH
  - oracle: {"class": "cy", "name": "a", "descriptor": "(Lpu;)V"}
  - compiled: {"class": "cy", "name": "a", "descriptor": "(Lpu;)V"}
  - rule A: public wrapper a(Lpu;)V delegates to core a(ZLpu;)V carrying save evidence 'Saving level'

### b1.2_02 (family G3, World=dn)

- world: probe scan (G3 group: level.dat,SpawnX,RandomSeed,TickNextTick list out of synch,Saving chunks,Saving level) -> shape-validated candidates: dn(hits=6:level.dat,SpawnX,RandomSeed,TickNextTick list out of synch,Saving chunks,Saving level)
- **getBlockId** verdict=MATCH confidence=HIGH
  - oracle: {"class": "dn", "name": "a", "descriptor": "(III)I"}
  - compiled: {"class": "dn", "name": "a", "descriptor": "(III)I"}
  - candidates: public non-static (III)I on dn = [a, e, l]
  - a(III)I: tail-calls chunk getter ib.a(III)I which does direct baload (no nibble delegation) -> block-id
  - e(III)I: callee pattern b(III)I does not match unsigned-byte chunk getter -> rejected (getMeta/getLight shape)
  - l(III)I: callee pattern none does not match unsigned-byte chunk getter -> rejected (getMeta/getLight shape)
- **setBlock** verdict=MATCH confidence=HIGH
  - oracle: {"class": "dn", "name": "e", "descriptor": "(IIII)Z"}
  - compiled: {"class": "dn", "name": "e", "descriptor": "(IIII)Z"}
  - candidates: public non-static (IIII)Z on dn = [a, b, d, e, k, l]
  - a: no sibling (IIII)Z delegation -> silent core / metadata setter
  - b: no sibling (IIII)Z delegation -> silent core / metadata setter
  - d: no sibling (IIII)Z delegation -> silent core / metadata setter
  - e: delegates to sibling (IIII)Z core b -> wrapper
  - k: no sibling (IIII)Z delegation -> silent core / metadata setter
  - l: no sibling (IIII)Z delegation -> silent core / metadata setter
- **saveWorld** verdict=MATCH confidence=HIGH
  - oracle: {"class": "dn", "name": "a", "descriptor": "(Lru;)V"}
  - compiled: {"class": "dn", "name": "a", "descriptor": "(Lru;)V"}
  - rule A: public wrapper a(Lru;)V delegates to core a(ZLru;)V carrying save evidence 'Saving level'

### b1.7.3 (family G3, World=fd)

- world: probe scan (G3 group: level.dat,SpawnX,RandomSeed,TickNextTick list out of synch,Saving chunks,Saving level) -> shape-validated candidates: fd(hits=3:TickNextTick list out of synch,Saving chunks,Saving level)
- **getBlockId** verdict=MATCH confidence=HIGH
  - oracle: {"class": "fd", "name": "a", "descriptor": "(III)I"}
  - compiled: {"class": "fd", "name": "a", "descriptor": "(III)I"}
  - candidates: public non-static (III)I on fd = [a, e, m, n]
  - a(III)I: tail-calls chunk getter lm.a(III)I which does direct baload (no nibble delegation) -> block-id
  - e(III)I: callee pattern b(III)I does not match unsigned-byte chunk getter -> rejected (getMeta/getLight shape)
  - m(III)I: callee pattern none does not match unsigned-byte chunk getter -> rejected (getMeta/getLight shape)
  - n(III)I: callee pattern none does not match unsigned-byte chunk getter -> rejected (getMeta/getLight shape)
- **setBlock** verdict=MATCH confidence=HIGH
  - oracle: {"class": "fd", "name": "f", "descriptor": "(IIII)Z"}
  - compiled: {"class": "fd", "name": "f", "descriptor": "(IIII)Z"}
  - candidates: public non-static (IIII)Z on fd = [b, c, e, f, j, k]
  - b: no sibling (IIII)Z delegation -> silent core / metadata setter
  - c: no sibling (IIII)Z delegation -> silent core / metadata setter
  - e: no sibling (IIII)Z delegation -> silent core / metadata setter
  - f: delegates to sibling (IIII)Z core c -> wrapper
  - j: no sibling (IIII)Z delegation -> silent core / metadata setter
  - k: no sibling (IIII)Z delegation -> silent core / metadata setter
- **saveWorld** verdict=MATCH confidence=HIGH
  - oracle: {"class": "fd", "name": "a", "descriptor": "(Lyb;)V"}
  - compiled: {"class": "fd", "name": "a", "descriptor": "(Lyb;)V"}
  - rule A: public wrapper a(Lyb;)V delegates to core a(ZLyb;)V carrying save evidence 'Saving level'

### b1.8.1 (family G3, World=rv)

- world: probe scan (G3 group: level.dat,SpawnX,RandomSeed,TickNextTick list out of synch,Saving chunks,Saving level) -> shape-validated candidates: rv(hits=3:TickNextTick list out of synch,Saving chunks,Saving level)
- **getBlockId** verdict=MATCH confidence=HIGH
  - oracle: {"class": "rv", "name": "a", "descriptor": "(III)I"}
  - compiled: {"class": "rv", "name": "a", "descriptor": "(III)I"}
  - candidates: public non-static (III)I on rv = [a, e, m, n]
  - a(III)I: tail-calls chunk getter vy.a(III)I which does direct baload (no nibble delegation) -> block-id
  - e(III)I: callee pattern b(III)I does not match unsigned-byte chunk getter -> rejected (getMeta/getLight shape)
  - m(III)I: callee pattern none does not match unsigned-byte chunk getter -> rejected (getMeta/getLight shape)
  - n(III)I: callee pattern none does not match unsigned-byte chunk getter -> rejected (getMeta/getLight shape)
- **setBlock** verdict=MATCH confidence=HIGH
  - oracle: {"class": "rv", "name": "g", "descriptor": "(IIII)Z"}
  - compiled: {"class": "rv", "name": "g", "descriptor": "(IIII)Z"}
  - candidates: public non-static (IIII)Z on rv = [e, d, c, g, k, l]
  - e: no sibling (IIII)Z delegation -> silent core / metadata setter
  - d: no sibling (IIII)Z delegation -> silent core / metadata setter
  - c: no sibling (IIII)Z delegation -> silent core / metadata setter
  - g: delegates to sibling (IIII)Z core d -> wrapper
  - k: no sibling (IIII)Z delegation -> silent core / metadata setter
  - l: no sibling (IIII)Z delegation -> silent core / metadata setter
- **saveWorld** verdict=MATCH confidence=HIGH
  - oracle: {"class": "rv", "name": "a", "descriptor": "(Lny;)V"}
  - compiled: {"class": "rv", "name": "a", "descriptor": "(Lny;)V"}
  - rule A: public wrapper a(Lny;)V delegates to core a(ZLny;)V carrying save evidence 'Saving level'

### c0.30-c (family G1, World=com/mojang/minecraft/level/Level)

- world: probe scan (G1 group: The level is corrupt!) -> shape-validated candidates: com/mojang/minecraft/level/Level(hits=1:The level is corrupt!)
- world: single probe hit on com/mojang/minecraft/level/Level (weak but unique after shape filter)
- **getBlockId** verdict=MATCH confidence=HIGH
  - oracle: {"class": "com/mojang/minecraft/level/Level", "name": "getTile", "descriptor": "(III)I"}
  - compiled: {"class": "com/mojang/minecraft/level/Level", "name": "getTile", "descriptor": "(III)I"}
  - candidates: public non-static (III)I on com/mojang/minecraft/level/Level = [getTile]
  - unique (III)I candidate -> getTile
- **setBlock** verdict=MATCH confidence=HIGH
  - oracle: {"class": "com/mojang/minecraft/level/Level", "name": "setTile", "descriptor": "(IIII)Z"}
  - compiled: {"class": "com/mojang/minecraft/level/Level", "name": "setTile", "descriptor": "(IIII)Z"}
  - candidates: public non-static (IIII)Z on com/mojang/minecraft/level/Level = [setTileNoNeighborChange, netSetTileNoNeighborChange, setTile, netSetTile, setTileNoUpdate]
  - setTileNoNeighborChange: delegates to sibling (IIII)Z core netSetTileNoNeighborChange -> wrapper
  - netSetTileNoNeighborChange: no sibling (IIII)Z delegation -> silent core / metadata setter
  - setTile: delegates to sibling (IIII)Z core setTileNoNeighborChange -> wrapper
  - netSetTile: delegates to sibling (IIII)Z core netSetTileNoNeighborChange -> wrapper
  - setTileNoUpdate: no sibling (IIII)Z delegation -> silent core / metadata setter
  - tiebreak: also calls sibling (IIII)V notification -> [setTile, netSetTile]
  - tiebreak: networkMode boolean gate (getfield :Z + early false return) -> setTile
- **saveWorld** verdict=MATCH confidence=HIGH
  - oracle: {"class": "com/mojang/minecraft/level/a", "name": "a", "descriptor": "(Lcom/mojang/minecraft/level/Level;Ljava/io/File;)Z"}
  - compiled: {"class": "com/mojang/minecraft/level/a", "name": "a", "descriptor": "(Lcom/mojang/minecraft/level/Level;Ljava/io/File;)Z"}
  - rule D: serializer method com/mojang/minecraft/level/a.a(Lcom/mojang/minecraft/level/Level;Ljava/io/File;)Z found on com/mojang/minecraft/level/a (storage manager for this era)

### in-20100223 (family G2, World=net/minecraft/a/a/g)

- world: probe scan (G2 group: level.dat,SpawnX,RandomSeed,MinecraftLevel,The level is corrupt!,TickNextTick list out of synch) -> shape-validated candidates: net/minecraft/a/a/g(hits=1:The level is corrupt!)
- world: single probe hit on net/minecraft/a/a/g (weak but unique after shape filter)
- **getBlockId** verdict=MATCH confidence=HIGH
  - oracle: {"class": "net/minecraft/a/a/g", "name": "a", "descriptor": "(III)I"}
  - compiled: {"class": "net/minecraft/a/a/g", "name": "a", "descriptor": "(III)I"}
  - candidates: public non-static (III)I on net/minecraft/a/a/g = [a]
  - unique (III)I candidate -> a
- **setBlock** verdict=MATCH confidence=HIGH
  - oracle: {"class": "net/minecraft/a/a/g", "name": "b", "descriptor": "(IIII)Z"}
  - compiled: {"class": "net/minecraft/a/a/g", "name": "b", "descriptor": "(IIII)Z"}
  - candidates: public non-static (IIII)Z on net/minecraft/a/a/g = [a, b, d]
  - a: no sibling (IIII)Z delegation -> silent core / metadata setter
  - b: delegates to sibling (IIII)Z core a -> wrapper
  - d: no sibling (IIII)Z delegation -> silent core / metadata setter
- **saveWorld** verdict=MATCH confidence=HIGH
  - oracle: {"class": "net/minecraft/a/a/a", "name": "a", "descriptor": "(Lnet/minecraft/a/a/g;Ljava/io/OutputStream;)V"}
  - compiled: {"class": "net/minecraft/a/a/a", "name": "a", "descriptor": "(Lnet/minecraft/a/a/g;Ljava/io/OutputStream;)V"}
  - rule D: serializer method net/minecraft/a/a/a.a(Lnet/minecraft/a/a/g;Ljava/io/OutputStream;)V found on net/minecraft/a/a/a (storage manager for this era)

### inf-20100327 (family G2, World=net/minecraft/a/a/f)

- world: probe scan (G2 group: level.dat,SpawnX,RandomSeed,MinecraftLevel,The level is corrupt!,TickNextTick list out of synch) -> shape-validated candidates: net/minecraft/a/a/f(hits=3:level.dat,SpawnX,RandomSeed)
- **getBlockId** verdict=MATCH confidence=HIGH
  - oracle: {"class": "net/minecraft/a/a/f", "name": "a", "descriptor": "(III)I"}
  - compiled: {"class": "net/minecraft/a/a/f", "name": "a", "descriptor": "(III)I"}
  - candidates: public non-static (III)I on net/minecraft/a/a/f = [a, d, f]
  - a(III)I: tail-calls chunk getter net/minecraft/a/a/e/d.a(III)I which does direct baload (no nibble delegation) -> block-id
  - d(III)I: callee pattern b(III)I does not match unsigned-byte chunk getter -> rejected (getMeta/getLight shape)
  - f(III)I: callee pattern none does not match unsigned-byte chunk getter -> rejected (getMeta/getLight shape)
- **setBlock** verdict=MATCH confidence=HIGH
  - oracle: {"class": "net/minecraft/a/a/f", "name": "c", "descriptor": "(IIII)Z"}
  - compiled: {"class": "net/minecraft/a/a/f", "name": "c", "descriptor": "(IIII)Z"}
  - candidates: public non-static (IIII)Z on net/minecraft/a/a/f = [a, c]
  - a: no sibling (IIII)Z delegation -> silent core / metadata setter
  - c: delegates to sibling (IIII)Z core a -> wrapper
- **saveWorld** verdict=MATCH confidence=HIGH
  - oracle: {"class": "net/minecraft/a/a/f", "name": "a", "descriptor": "(Z)V"}
  - compiled: {"class": "net/minecraft/a/a/f", "name": "a", "descriptor": "(Z)V"}
  - rule B: (Z)V core a carries save evidence 'level.dat'

### inf-20100618 (family G2, World=net/minecraft/a/a/g)

- world: probe scan (G2 group: level.dat,SpawnX,RandomSeed,MinecraftLevel,The level is corrupt!,TickNextTick list out of synch) -> shape-validated candidates: net/minecraft/a/a/g(hits=4:level.dat,SpawnX,RandomSeed,TickNextTick list out of synch)
- **getBlockId** verdict=MATCH confidence=HIGH
  - oracle: {"class": "net/minecraft/a/a/g", "name": "a", "descriptor": "(III)I"}
  - compiled: {"class": "net/minecraft/a/a/g", "name": "a", "descriptor": "(III)I"}
  - candidates: public non-static (III)I on net/minecraft/a/a/g = [a, d, f]
  - a(III)I: tail-calls chunk getter net/minecraft/a/a/e/e.a(III)I which does direct baload (no nibble delegation) -> block-id
  - d(III)I: callee pattern b(III)I does not match unsigned-byte chunk getter -> rejected (getMeta/getLight shape)
  - f(III)I: callee pattern none does not match unsigned-byte chunk getter -> rejected (getMeta/getLight shape)
- **setBlock** verdict=MATCH confidence=HIGH
  - oracle: {"class": "net/minecraft/a/a/g", "name": "c", "descriptor": "(IIII)Z"}
  - compiled: {"class": "net/minecraft/a/a/g", "name": "c", "descriptor": "(IIII)Z"}
  - candidates: public non-static (IIII)Z on net/minecraft/a/a/g = [a, c]
  - a: no sibling (IIII)Z delegation -> silent core / metadata setter
  - c: delegates to sibling (IIII)Z core a -> wrapper
- **saveWorld** verdict=MATCH confidence=HIGH
  - oracle: {"class": "net/minecraft/a/a/g", "name": "b", "descriptor": "(Z)V"}
  - compiled: {"class": "net/minecraft/a/a/g", "name": "b", "descriptor": "(Z)V"}
  - rule B: (Z)V core b carries save evidence 'level.dat'

### inf-20100629 (family G3, World=ez)

- world: probe scan (G3 group: level.dat,SpawnX,RandomSeed,TickNextTick list out of synch,Saving chunks,Saving level) -> shape-validated candidates: ez(hits=5:level.dat,SpawnX,RandomSeed,TickNextTick list out of synch,Saving chunks)
- **getBlockId** verdict=MATCH confidence=HIGH
  - oracle: {"class": "ez", "name": "a", "descriptor": "(III)I"}
  - compiled: {"class": "ez", "name": "a", "descriptor": "(III)I"}
  - candidates: public non-static (III)I on ez = [a, d, i]
  - a(III)I: tail-calls chunk getter jr.b(III)I which does direct baload (no nibble delegation) -> block-id
  - d(III)I: callee pattern c(III)I does not match unsigned-byte chunk getter -> rejected (getMeta/getLight shape)
  - i(III)I: callee pattern none does not match unsigned-byte chunk getter -> rejected (getMeta/getLight shape)
- **setBlock** verdict=MATCH confidence=HIGH
  - oracle: {"class": "ez", "name": "c", "descriptor": "(IIII)Z"}
  - compiled: {"class": "ez", "name": "c", "descriptor": "(IIII)Z"}
  - candidates: public non-static (IIII)Z on ez = [a, c]
  - a: no sibling (IIII)Z delegation -> silent core / metadata setter
  - c: delegates to sibling (IIII)Z core a -> wrapper
- **saveWorld** verdict=MATCH confidence=HIGH
  - oracle: {"class": "ez", "name": "a", "descriptor": "(Liw;)V"}
  - compiled: {"class": "ez", "name": "a", "descriptor": "(Liw;)V"}
  - rule A: public wrapper a(Liw;)V delegates to core a(ZLiw;)V carrying save evidence 'Saving chunks'
