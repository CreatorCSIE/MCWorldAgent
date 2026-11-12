import type { Ref } from 'vue'

export type WorkspaceId = 'explorer' | 'analysis' | 'runtime'
export type ExecMode = 'standalone' | 'agent'
export type ConnectionState = 'connected' | 'disconnected' | 'error'
export type ConsoleLevel = 'info' | 'event' | 'warn'

/** Inspector 字段行。 */
export interface InspectorField {
  label: string
  value: string
  /** 等宽字体渲染（路径 / id / 计数等）。 */
  mono?: boolean
}

/** 任何可被 Inspector 展示的 mock 对象。 */
export interface Inspectable {
  id: string
  kind: 'world' | 'chunk-dir' | 'chunk' | 'layer' | 'hook'
  title: string
  subtitle?: string
  fields: InspectorField[]
}

export interface WorldTreeNode extends Inspectable {
  kind: 'world' | 'chunk-dir' | 'chunk'
  children?: WorldTreeNode[]
}

export interface AnalysisLayer extends Inspectable {
  kind: 'layer'
  status: 'available' | 'placeholder'
}

export interface HookInfo extends Inspectable {
  kind: 'hook'
  hookId: string
  /** 混淆实名目标（class.method + descriptor），对齐 HookTargets.java。 */
  target: string
  injectMode: 'entry' | 'entry_and_result'
}

export interface AgentStatus {
  versionId: string
  jarSha1: string
  premainAttached: boolean
  agentArgs: string
  process: {
    javaVersion: string
    pid: number
    launcher: string
    runtime: string
  }
  /** 把手①两半：world-ctor 记存档目录，save-world 捕获活体 World。 */
  handle1: {
    capturedWorld: string | null
    capturedSaveDir: string | null
  }
  hooks: HookInfo[]
  /** 已验证会话的基线计数（live tick 由 source.tick 叠加）。 */
  baseline: {
    ticks: number
    chunkEnter: number
    chunkHit: number
    chunkMiss: number
    lightRecalc: number
    saveCalls: number
  }
  autosave: {
    vanillaIntervalTicks: number
    legacyfixInterval: string
    note: string
  }
}

export interface ConsoleLine {
  id: number
  /** 距 source 启动的毫秒数，模拟 agent 日志前缀。 */
  elapsedMs: number
  level: ConsoleLevel
  text: string
}

export interface PlayerPos {
  x: number
  y: number
  z: number
}

/**
 * 数据源与展示分离的接口。当前实现是 mock（createMockSource），
 * 后续接 Tauri/真实 agent 时替换实现、接口形状不变。
 */
export interface WorldSource {
  getWorldTree(): WorldTreeNode[]
  getAnalysisLayers(): AnalysisLayer[]
  getAgentStatus(): AgentStatus
  /** 订阅时的历史回放（新面板挂载用）。 */
  getConsoleHistory(): ConsoleLine[]
  /** 返回取消订阅函数。 */
  subscribeConsole(listener: (line: ConsoleLine) => void): () => void
  readonly tick: Readonly<Ref<number>>
  readonly playerPos: Readonly<Ref<PlayerPos>>
  readonly connection: Readonly<Ref<ConnectionState>>
  readonly mode: Readonly<Ref<ExecMode>>
}
