/**
 * hooks.log 行解析 + 进程推断态状态机输入（shell-t1 spec §5）。
 * 纯函数零依赖。行式=MCAHooks.log 实样（`[123ms] EVENT detail`），时间戳为
 * JVM 相对毫秒——新鲜度判据用**墙钟到达时间**（前端记 lastTickAt=Date.now()），
 * 不用行内毫秒（那是进程内相对值）。
 */

export interface HookEvent {
  /** 事件名（TICK/SAVE_WORLD/WORLD_CREATED/WORLD_SWITCHED/CHANNEL 系/HUD_PAINT 等）。 */
  name: string
  /** 行内毫秒时间戳（JVM 相对）。 */
  elapsedMs: number
  /** 事件名后的原文尾串。 */
  tail: string
  /** TICK n 的 n。 */
  tick?: number
}

const LINE_RE = /^\[(\d+)ms\]\s+(\w+)(?:\s+(.*))?$/

export function parseHookLine(raw: string): HookEvent | null {
  const m = LINE_RE.exec(raw)
  if (!m) return null
  const ev: HookEvent = { elapsedMs: Number(m[1]), name: m[2], tail: m[3] ?? '' }
  if (ev.name === 'TICK') {
    const t = /^(\d+)/.exec(ev.tail)
    if (t) ev.tick = Number(t[1])
  }
  return ev
}

export type ProcessState = 'unknown' | 'alive-title' | 'alive-inworld' | 'dead'

/**
 * 状态机纯函数：给定累计证据+时钟，推断进程态（spec §5 表）。
 * - TICK 停更>5s=dead；
 * - TICK 新鲜且见过进世界证据（WORLD_CREATED/SAVE_WORLD/WORLD_SWITCHED 后未回标题）=alive-inworld；
 * - TICK 新鲜但无进世界证据=alive-title。
 * 0327 特性：标题界面 runTick 照常=仅 TICK 新鲜不足以区分世界内外，
 * 进世界证据=SAVE_WORLD（进世界后首存）或 WORLD_CREATED；回标题无独立事件行
 * （判决：v1 保守——见过进世界证据后除非进程死亡，保持 in-world 红色提示，
 * 宁过严勿过松=kill×存盘戒律方向）。
 */
export interface StateInput {
  now: number
  lastTickAt: number
  sawWorldEvidence: boolean
  dirValid: boolean
}

export function inferProcessState(i: StateInput): ProcessState {
  if (!i.dirValid) return 'unknown'
  if (i.lastTickAt === 0) return 'unknown'
  if (i.now - i.lastTickAt > 5000) return 'dead'
  return i.sawWorldEvidence ? 'alive-inworld' : 'alive-title'
}

/** outbox 终态 JSON（协议信封回读）→ 展示行。 */
export interface OutboxResult {
  id: string
  status: string
  detail?: string
  tick?: number
}

export function parseOutbox(json: string): OutboxResult | null {
  try {
    const o = JSON.parse(json) as Record<string, unknown>
    if (typeof o.id !== 'string' || typeof o.status !== 'string') return null
    return {
      id: o.id,
      status: o.status,
      detail: typeof o.detail === 'string' ? o.detail : undefined,
      tick: typeof o.tick === 'number' ? o.tick : undefined,
    }
  } catch {
    return null
  }
}
