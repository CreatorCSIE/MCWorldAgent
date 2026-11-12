import { ref } from 'vue'
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
} from './types'
import { MOCK_AGENT_STATUS } from './agent'
import { MOCK_ANALYSIS_LAYERS, MOCK_WORLD_TREE } from './world'

const HISTORY_CAP = 500

function randStep(): number {
  return Math.floor(Math.random() * 3) - 1
}

/**
 * mock 实现：setInterval 产生 tick 递增与 hook 风格日志流。
 * 日志文本刻意模仿 MCAHooks.log 的行式（TICK / WORLD_DIR / SAVE_WORLD #n /
 * CHUNK_LOAD_ENTER / LIGHT_RECALC / SUBMIT / PREMAIN / TRANSFORM），
 * 转正为真实 agent 通道时只换实现、不改 WorldSource 接口。
 */
export function createMockSource(): WorldSource {
  const tick = ref(0)
  const playerPos = ref<PlayerPos>({ x: 2, y: 64, z: -4 })
  const connection = ref<ConnectionState>('connected')
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

  // 启动序列（同步发出，晚挂载的面板经 getConsoleHistory 回放）
  emit('info', 'PREMAIN mcworldagent-agent args=[log=mcworldagent-transform.log;hooks=mcworldagent-hooks.log]')
  emit('info', 'TRANSFORM woven 5 hooks (strict fingerprint: class+method+desc+access+hits)')
  emit('event', 'WORLD_DIR dir=saves/World2 level.dat=true')
  emit('event', 'WORLD_CREATED world=net.minecraft.a.a.f@3f1a2c dir=saves/World2')
  emit('warn', 'AUTOSAVE legacyfix AutoSavePatch active: interval 5 min (vanilla 0327 = 200 ticks)')

  let lightSeq = MOCK_AGENT_STATUS.baseline.lightRecalc
  let saveSeq = MOCK_AGENT_STATUS.baseline.saveCalls

  const tickTimer = window.setInterval(() => {
    tick.value += 1
    if (tick.value % 20 === 0) emit('event', `TICK ${tick.value} (live)`)
    if (tick.value % 10 === 0) {
      const p = playerPos.value
      playerPos.value = {
        x: p.x + randStep(),
        y: Math.min(70, Math.max(64, p.y + randStep())),
        z: p.z + randStep(),
      }
    }
  }, 50)

  const eventTimer = window.setInterval(() => {
    const r = Math.random()
    if (r < 0.4) {
      const cx = Math.floor(playerPos.value.x / 16) + randStep()
      const cz = Math.floor(playerPos.value.z / 16) + randStep()
      emit('event', `CHUNK_LOAD_ENTER x=${cx} z=${cz}`)
    } else if (r < 0.62) {
      emit('event', `LIGHT_RECALC #${++lightSeq}`)
    } else if (r < 0.8) {
      emit('info', 'SUBMIT queued (runs on game thread at next tick)')
    } else if (r < 0.93) {
      emit('event', `SAVE_WORLD #${++saveSeq} id=3f1a2c indirect=true`)
    } else {
      emit('warn', 'G2 note: disk chunks (1729) exceed loadChunk miss (639) — generation bypasses provider layer')
    }
  }, 1500)

  void tickTimer
  void eventTimer

  return {
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
  }
}
