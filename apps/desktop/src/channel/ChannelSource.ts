/**
 * ChannelSource：WorldSource 的真实通道实现（shell-t1 spec §3——
 * "转正为真实 agent 通道时只换实现、不改接口"判决兑现）。
 * 事件流=hooks.log tail + outbox 回声合流；tick/世界证据由行解析喂。
 * 世界树/分析层/AgentStatus 三面仍复用 mock 数据（T2 Editor 域，注释标记）。
 */
import { ref, type Ref } from 'vue'
import type {
  AgentStatus,
  AnalysisLayer,
  ConsoleLevel,
  ConsoleLine,
  ConnectionState,
  ExecMode,
  PlayerPos,
  WorldSource,
  WorldTreeNode,
} from '@/mock/types'
import { MOCK_AGENT_STATUS } from '@/mock/agent'
import { MOCK_ANALYSIS_LAYERS, MOCK_WORLD_TREE } from '@/mock/world'

const HISTORY_CAP = 500

/** hooks/outbox 行 → 展示级。warn 面=故障/拒绝/错误。 */
export function levelFor(name: string, status?: string): ConsoleLevel {
  if (status !== undefined) {
    return status === 'OK' || status === 'ACCEPTED' || status === 'NO_CHANGE' ? 'event' : 'warn'
  }
  if (name === 'TICK' || name === 'SUBMIT' || name === 'CHANNEL_ACCEPT') return 'info'
  if (
    name === 'HUD_HOOK_DISABLED' ||
    name === 'TICK_TASK_ERROR' ||
    name === 'UNSUPPORTED' ||
    name === 'ERROR'
  )
    return 'warn'
  return 'event'
}

export interface ChannelSource extends WorldSource {
  /** 通道侧注入行（poller/outbox 回声用）。 */
  feed(level: ConsoleLevel, text: string, raw?: string): void
  /** 由 TICK 行喂 tick。 */
  feedTick(n: number): void
  /** 连接态（状态机映射）。 */
  connectionRef: Ref<ConnectionState>
  /** getinfo/getplayer 回声解析后喂坐标（v1 尽力面，无数据保持前值）。 */
  feedPos(p: Partial<PlayerPos>): void
}

export function createChannelSource(): ChannelSource {
  const tick = ref(0)
  const playerPos = ref<PlayerPos>({ x: 0, y: 64, z: 0 })
  const connection = ref<ConnectionState>('disconnected')
  const mode = ref<ExecMode>('agent')

  const listeners = new Set<(line: ConsoleLine) => void>()
  const history: ConsoleLine[] = []
  let lineId = 0
  const t0 = Date.now()

  function emit(level: ConsoleLevel, text: string) {
    const line: ConsoleLine = { id: ++lineId, elapsedMs: Date.now() - t0, level, text }
    history.push(line)
    if (history.length > HISTORY_CAP) history.splice(0, history.length - HISTORY_CAP)
    for (const l of listeners) l(line)
  }

  return {
    // T2 域：树/层/状态三面暂复用 mock 形状（接口不变判决）。
    getWorldTree: (): WorldTreeNode[] => MOCK_WORLD_TREE,
    getAnalysisLayers: (): AnalysisLayer[] => MOCK_ANALYSIS_LAYERS,
    getAgentStatus: (): AgentStatus => MOCK_AGENT_STATUS,
    getConsoleHistory: () => history.slice(),
    subscribeConsole(listener: (line: ConsoleLine) => void) {
      listeners.add(listener)
      return () => listeners.delete(listener)
    },
    tick,
    playerPos,
    connection,
    mode,
    feed(level, text) {
      emit(level, text)
    },
    feedTick(n) {
      tick.value = n
    },
    feedPos(p) {
      playerPos.value = { ...playerPos.value, ...p }
    },
    connectionRef: connection,
  }
}
