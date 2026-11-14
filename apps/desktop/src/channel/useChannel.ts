/**
 * useChannel：T1 命令台核心 composable（shell-t1 spec §3/§5）。
 * - 配置：mca 通道目录 + hooks.log 路径（localStorage 记忆）；
 * - 双源轮询（250ms，前端节奏，Rust 零轮询逻辑）：hooks.log 增量 tail + outbox 新终态；
 * - 命令台状态机：输入→投递(posted)→ACCEPTED→终态回声；
 * - 证据链：每条投递 append cmds-shell.txt（与 CLI 归档同等待遇）；
 * - listops 回声校验映射表（spec §7：不符=状态栏黄字，不静默）；
 * - 非 Tauri 环境=mock 假投递回声（命令台走查通路不杀）。
 */
import { computed, reactive, ref, watch } from 'vue'
import { chan, inTauri } from './ipc'
import { buildEnvelope, nextId, OPS, parseShorthand } from './shorthand'
import { inferProcessState, parseHookLine, parseOutbox, type ProcessState } from './lines'
import { createChannelSource, levelFor, type ChannelSource } from './ChannelSource'

export interface CommandEntry {
  id: string
  line: string
  phase: 'posted' | 'accepted' | 'terminal' | 'rejected-shell'
  status?: string
  detail?: string
  at: number
}

const LS_DIR = 'mca.channelDir'
const LS_LOG = 'mca.hooksLog'
const POLL_MS = 250

let singleton: ReturnType<typeof build> | null = null

export function useChannel() {
  if (!singleton) singleton = build()
  return singleton
}

function build() {
  const source: ChannelSource = createChannelSource()
  const isTauri = inTauri()

  const mcaDir = ref<string>(localStorage.getItem(LS_DIR) ?? '')
  const hooksLog = ref<string>(localStorage.getItem(LS_LOG) ?? '')
  watch(mcaDir, (v) => localStorage.setItem(LS_DIR, v))
  watch(hooksLog, (v) => localStorage.setItem(LS_LOG, v))

  const commands = reactive<CommandEntry[]>([])
  const dirValid = ref(false)
  const lastTickAt = ref(0)
  const sawWorldEvidence = ref(false)
  const mappingDrift = ref<string | null>(null)
  const worldInfo = ref<string>('')
  const now = ref(Date.now())

  let hooksOffset = 0
  const knownOutbox = new Set<string>()
  const pendingIds = new Map<string, CommandEntry>()
  let timer: number | null = null

  const processState = computed<ProcessState>(() =>
    inferProcessState({
      now: now.value,
      lastTickAt: lastTickAt.value,
      sawWorldEvidence: sawWorldEvidence.value,
      dirValid: dirValid.value,
    }),
  )

  watch(
    processState,
    (s) => {
      source.connectionRef.value =
        s === 'alive-title' || s === 'alive-inworld' ? 'connected' : s === 'dead' ? 'disconnected' : 'error'
    },
    { immediate: true },
  )

  function join(p: string, ...rest: string[]): string {
    return p.replace(/[\\/]+$/, '') + '\\' + rest.join('\\')
  }

  function outboxDir(): string {
    return join(mcaDir.value, 'outbox')
  }

  async function pollOnce() {
    if (!isTauri || mcaDir.value.length === 0) return
    now.value = Date.now()
    try {
      // 源一：hooks.log 增量
      if (hooksLog.value.length > 0) {
        const r = await chan.readLines(hooksLog.value, hooksOffset)
        if (!r.missing) {
          dirValid.value = true
          hooksOffset = r.offset
          for (const raw of r.lines) {
            const ev = parseHookLine(raw)
            if (!ev) continue
            if (ev.name === 'TICK' && ev.tick !== undefined) {
              lastTickAt.value = now.value
              source.feedTick(ev.tick)
            }
            if (
              ev.name === 'WORLD_CREATED' ||
              ev.name === 'SAVE_WORLD' ||
              ev.name === 'WORLD_SWITCHED'
            ) {
              sawWorldEvidence.value = true
            }
            source.feed(levelFor(ev.name), raw)
          }
        }
      }
      // 源二：outbox 新文件
      const names = await chan.listDir(outboxDir())
      dirValid.value = true
      for (const name of names) {
        if (knownOutbox.has(name)) continue
        knownOutbox.add(name)
        const json = await chan.readFile(join(outboxDir(), name))
        const res = parseOutbox(json)
        if (!res) continue
        const entry = pendingIds.get(res.id)
        if (entry) {
          if (res.status === 'ACCEPTED') {
            entry.phase = 'accepted'
          } else {
            entry.phase = 'terminal'
            entry.status = res.status
            entry.detail = res.detail
          }
        }
        source.feed(levelFor('', res.status), `OUTBOX ${res.id} ${res.status}${res.detail ? ' ' + res.detail : ''}`)
        if (entry && entry.line.startsWith('listops') && res.status === 'OK' && res.detail) {
          verifyMapping(res.detail)
        }
        // getinfo 回声喂世界名（状态栏）
        if (entry && entry.line.startsWith('getinfo') && res.detail) {
          worldInfo.value = res.detail
        }
      }
    } catch (e) {
      // ENOENT=通道未就绪（游戏没起/目录没建），不记错误不刷屏
      const msg = String(e)
      if (!msg.includes('ENOENT')) {
        source.feed('warn', 'CHANNEL_POLL_ERROR ' + msg)
      }
    }
  }

  function start() {
    if (timer !== null) return
    timer = window.setInterval(pollOnce, POLL_MS)
  }

  function stop() {
    if (timer !== null) {
      window.clearInterval(timer)
      timer = null
    }
  }

  /** listops 回声校验：detail 串含 `name(` 形态 op 集，对映射表双向差集。 */
  function verifyMapping(detail: string) {
    const agentOps = new Set<string>()
    const re = /([a-z][a-z0-9]*)\(/g
    let m: RegExpExecArray | null
    while ((m = re.exec(detail)) !== null) agentOps.add(m[1])
    if (agentOps.size === 0) return
    const shell = new Set(OPS.map((o) => o.name))
    const missing = [...agentOps].filter((o) => !shell.has(o))
    const extra = [...shell].filter((o) => !agentOps.has(o))
    mappingDrift.value =
      missing.length + extra.length === 0 ? null : `drift: agent-only[${missing}] shell-only[${extra}]`
  }

  /** 命令台入口。返回错误串（壳侧预检）或空串（已投递）。 */
  async function send(line: string): Promise<string> {
    const parsed = parseShorthand(line)
    const id = nextId(Date.now())
    if (!parsed.ok) {
      // 壳侧预检拒绝=命令台历史可见（rejected-shell 相位，两种渲染通路都吃到）
      const rej: CommandEntry = {
        id,
        line,
        phase: 'rejected-shell',
        status: 'SHELL-REJECT',
        detail: parsed.error,
        at: Date.now(),
      }
      commands.push(rej)
      if (commands.length > 500) commands.splice(0, commands.length - 500)
      source.feed('warn', `SHELL ${line} -> ${parsed.error}`)
      return parsed.error
    }
    const entry: CommandEntry = { id, line, phase: 'posted', at: Date.now() }
    commands.push(entry)
    if (commands.length > 500) commands.splice(0, commands.length - 500)
    pendingIds.set(id, entry)
    const envelope = buildEnvelope(parsed.cmd, id)
    if (!isTauri) {
      // mock 假投递回声（走查通路）：ACCEPTED→OK 两段
      window.setTimeout(() => {
        entry.phase = 'accepted'
        source.feed('info', `MOCK OUTBOX ${id} ACCEPTED`)
      }, 150)
      window.setTimeout(() => {
        entry.phase = 'terminal'
        entry.status = 'OK'
        entry.detail = 'mock echo'
        source.feed('event', `MOCK OUTBOX ${id} OK mock echo`)
      }, 400)
      return ''
    }
    try {
      const tmpName = `${id}.json`
      await chan.post(mcaDir.value, tmpName, envelope)
      await chan.append(join(mcaDir.value, '..', 'cmds-shell.txt'), `${new Date().toISOString()} ${envelope}`)
      source.feed('info', `SHELL POST ${id} ${line}`)
    } catch (e) {
      entry.phase = 'terminal'
      entry.status = 'POST-ERROR'
      entry.detail = String(e)
      return String(e)
    }
    return ''
  }

  /** 重连接（配置变更后）：清偏移与已知集。 */
  function reconnect() {
    hooksOffset = 0
    knownOutbox.clear()
    pendingIds.clear()
    dirValid.value = false
    lastTickAt.value = 0
    sawWorldEvidence.value = false
    mappingDrift.value = null
  }

  return {
    source,
    isTauri,
    mcaDir,
    hooksLog,
    commands,
    processState,
    dirValid,
    mappingDrift,
    worldInfo,
    send,
    start,
    stop,
    reconnect,
  }
}
