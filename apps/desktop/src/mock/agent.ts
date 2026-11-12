import type { AgentStatus, HookInfo } from './types'

/**
 * 钩名与目标实名直接对齐 java/agent/src/com/mcworldagent/agent/HookTargets.java
 * （infdev 20100327 原始 jar，SHA1 47b1b32430a211520993552ba0a5e00c1af44724）：
 *   world-ctor / save-world / run-tick / load-chunk / gen-height
 * 注入方式 ENTRY / ENTRY_AND_RESULT、access flags、期望命中数均取自该表。
 */
function hook(
  hookId: string,
  label: string,
  target: string,
  injectMode: HookInfo['injectMode'],
  access: string,
  role: string,
): HookInfo {
  return {
    id: `hook:${hookId}`,
    kind: 'hook',
    hookId,
    target,
    injectMode,
    title: hookId,
    subtitle: label,
    fields: [
      { label: 'hook id', value: hookId, mono: true },
      { label: 'readable', value: label },
      { label: 'obfuscated target', value: target, mono: true },
      { label: 'inject mode', value: injectMode === 'entry' ? 'ENTRY' : 'ENTRY_AND_RESULT', mono: true },
      { label: 'access flags', value: access, mono: true },
      { label: 'expected hits', value: '1', mono: true },
      { label: 'role', value: role },
    ],
  }
}

export const MOCK_HOOKS: HookInfo[] = [
  hook(
    'world-ctor',
    'World.<init>(File,String)',
    'net/minecraft/a/a/f.<init>(Ljava/io/File;Ljava/lang/String;)V',
    'entry',
    '0x0001',
    '把手①：构造器入口记录存档目录（capturedSaveDir）',
  ),
  hook(
    'save-world',
    'World.saveWorld(boolean)',
    'net/minecraft/a/a/f.a(Z)V',
    'entry',
    '0x0002',
    '把手①：saveWorld 入口捕获活体 World（capturedWorld）；G1 autosave 证据',
  ),
  hook(
    'run-tick',
    'Minecraft.runTick()',
    'net/minecraft/client/d.h()V',
    'entry',
    '0x0002',
    'tick 汇线：游戏线程 tick 边界 drain 提交队列',
  ),
  hook(
    'load-chunk',
    'ChunkProviderLoadOrGenerate.loadChunk(int,int)',
    'net/minecraft/a/a/e/c.d(II)Lnet/minecraft/a/a/e/d;',
    'entry_and_result',
    '0x0002',
    'G2：磁盘命中 / 未命中计数',
  ),
  hook(
    'gen-height',
    'Chunk.generateHeightMap()',
    'net/minecraft/a/a/e/d.a()V',
    'entry',
    '0x0011',
    'G2：light/height 回退重算事件计数',
  ),
]

export const MOCK_AGENT_STATUS: AgentStatus = {
  versionId: 'infdev 20100327',
  jarSha1: '47b1b32430a211520993552ba0a5e00c1af44724',
  premainAttached: true,
  agentArgs: 'log=mcworldagent-transform.log;hooks=mcworldagent-hooks.log',
  process: {
    javaVersion: '1.8.0_431 (mock)',
    pid: 17290,
    launcher: 'Attic (mock)',
    runtime: 'legacyfix javaagent mounted',
  },
  handle1: {
    capturedWorld: 'net.minecraft.a.a.f@3f1a2c (mock)',
    capturedSaveDir: 'saves/World2',
  },
  hooks: MOCK_HOOKS,
  baseline: {
    ticks: 18422,
    chunkEnter: 2368,
    chunkHit: 1729,
    chunkMiss: 639,
    lightRecalc: 214,
    saveCalls: 41,
  },
  autosave: {
    vanillaIntervalTicks: 200,
    legacyfixInterval: '5 min (AutoSavePatch default)',
    note: 'G1: no in-session autosave to rely on — Agent Mode must explicitly invoke saveWorld through handle-① after mutations.',
  },
}
