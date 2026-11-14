/**
 * T1 命令台速记解析器（shell-t1-command-panel spec §3/§4）。
 * 纯函数零依赖（vue-free，无头可测）。映射表=通道案卷 §6 op 表镜像，
 * 键名逐一对榫 CommandChannel.runOp（agent 侧是唯一权威；启动时 listops 回声校验）。
 */

export type ArgKind = 'int' | 'long' | 'str' | 'flag'

export interface ArgSpec {
  key: string
  kind: ArgKind
  optional?: boolean
}

export type SaveMode = 'true' | 'false' | 'ignore'

export interface OpSpec {
  name: string
  args: ArgSpec[]
  /** true=写侧信封带 save:true；false=tp 类默认不落盘（尾缀 save 关键字开启）；ignore=只读/瞬态不发 save 字段。 */
  save: SaveMode
}

/** 35 op 全量（KNOWN_OPS 对榫）。 */
export const OPS: OpSpec[] = [
  { name: 'place', args: [{ key: 'x', kind: 'int' }, { key: 'y', kind: 'int' }, { key: 'z', kind: 'int' }, { key: 'blockId', kind: 'int' }], save: 'true' },
  { name: 'dig', args: [{ key: 'x', kind: 'int' }, { key: 'y', kind: 'int' }, { key: 'z', kind: 'int' }], save: 'true' },
  { name: 'getblock', args: [{ key: 'x', kind: 'int' }, { key: 'y', kind: 'int' }, { key: 'z', kind: 'int' }], save: 'ignore' },
  { name: 'getblocks', args: [{ key: 'x1', kind: 'int' }, { key: 'y1', kind: 'int' }, { key: 'z1', kind: 'int' }, { key: 'x2', kind: 'int' }, { key: 'y2', kind: 'int' }, { key: 'z2', kind: 'int' }], save: 'ignore' },
  { name: 'fill', args: [{ key: 'x1', kind: 'int' }, { key: 'y1', kind: 'int' }, { key: 'z1', kind: 'int' }, { key: 'x2', kind: 'int' }, { key: 'y2', kind: 'int' }, { key: 'z2', kind: 'int' }, { key: 'blockId', kind: 'int' }], save: 'true' },
  { name: 'replace', args: [{ key: 'x1', kind: 'int' }, { key: 'y1', kind: 'int' }, { key: 'z1', kind: 'int' }, { key: 'x2', kind: 'int' }, { key: 'y2', kind: 'int' }, { key: 'z2', kind: 'int' }, { key: 'oldId', kind: 'int' }, { key: 'newId', kind: 'int' }], save: 'true' },
  { name: 'setmeta', args: [{ key: 'x', kind: 'int' }, { key: 'y', kind: 'int' }, { key: 'z', kind: 'int' }, { key: 'meta', kind: 'int' }], save: 'true' },
  { name: 'getmeta', args: [{ key: 'x', kind: 'int' }, { key: 'y', kind: 'int' }, { key: 'z', kind: 'int' }], save: 'ignore' },
  { name: 'getlight', args: [{ key: 'x', kind: 'int' }, { key: 'y', kind: 'int' }, { key: 'z', kind: 'int' }], save: 'ignore' },
  { name: 'genscatter', args: [{ key: 'x', kind: 'int' }, { key: 'y', kind: 'int' }, { key: 'z', kind: 'int' }, { key: 'blockId', kind: 'int' }, { key: 'seed', kind: 'long', optional: true }], save: 'true' },
  { name: 'getinfo', args: [], save: 'ignore' },
  { name: 'settime', args: [{ key: 'time', kind: 'long' }], save: 'true' },
  { name: 'setspawn', args: [{ key: 'x', kind: 'int' }, { key: 'y', kind: 'int' }, { key: 'z', kind: 'int' }], save: 'true' },
  { name: 'tp', args: [{ key: 'x', kind: 'int' }, { key: 'y', kind: 'int' }, { key: 'z', kind: 'int' }], save: 'false' },
  { name: 'getplayer', args: [], save: 'ignore' },
  { name: 'setlook', args: [{ key: 'yaw', kind: 'int' }, { key: 'pitch', kind: 'int' }], save: 'true' },
  { name: 'setmotion', args: [{ key: 'vx', kind: 'int' }, { key: 'vy', kind: 'int' }, { key: 'vz', kind: 'int' }], save: 'true' },
  { name: 'sethealth', args: [{ key: 'health', kind: 'int' }], save: 'true' },
  { name: 'invulnerable', args: [{ key: 'ticks', kind: 'int' }], save: 'true' },
  { name: 'give', args: [{ key: 'itemId', kind: 'int' }, { key: 'count', kind: 'int' }, { key: 'dmg', kind: 'int', optional: true }], save: 'true' },
  { name: 'inv', args: [], save: 'ignore' },
  { name: 'setarmor', args: [{ key: 'slot', kind: 'int' }, { key: 'itemId', kind: 'int' }, { key: 'count', kind: 'int' }, { key: 'dmg', kind: 'int', optional: true }], save: 'true' },
  { name: 'clearinv', args: [], save: 'true' },
  { name: 'spawn', args: [{ key: 'kind', kind: 'str' }, { key: 'x', kind: 'int' }, { key: 'y', kind: 'int' }, { key: 'z', kind: 'int' }], save: 'true' },
  { name: 'drop', args: [{ key: 'itemId', kind: 'int' }, { key: 'count', kind: 'int' }, { key: 'dmg', kind: 'int', optional: true }], save: 'true' },
  { name: 'explode', args: [{ key: 'x', kind: 'int' }, { key: 'y', kind: 'int' }, { key: 'z', kind: 'int' }, { key: 'size', kind: 'int' }], save: 'true' },
  { name: 'entitylist', args: [], save: 'ignore' },
  { name: 'playsound', args: [{ key: 'name', kind: 'str' }, { key: 'x', kind: 'int' }, { key: 'y', kind: 'int' }, { key: 'z', kind: 'int' }, { key: 'vol', kind: 'int' }, { key: 'pitch', kind: 'int' }], save: 'ignore' },
  { name: 'getchest', args: [{ key: 'x', kind: 'int' }, { key: 'y', kind: 'int' }, { key: 'z', kind: 'int' }], save: 'ignore' },
  { name: 'setchest', args: [{ key: 'x', kind: 'int' }, { key: 'y', kind: 'int' }, { key: 'z', kind: 'int' }, { key: 'slot', kind: 'int' }, { key: 'itemId', kind: 'int', optional: true }, { key: 'count', kind: 'int', optional: true }, { key: 'dmg', kind: 'int', optional: true }], save: 'true' },
  { name: 'getfurnace', args: [{ key: 'x', kind: 'int' }, { key: 'y', kind: 'int' }, { key: 'z', kind: 'int' }], save: 'ignore' },
  { name: 'grow', args: [{ key: 'x', kind: 'int' }, { key: 'y', kind: 'int' }, { key: 'z', kind: 'int' }], save: 'true' },
  { name: 'clear', args: [{ key: 'x', kind: 'int', optional: true }, { key: 'y', kind: 'int', optional: true }, { key: 'z', kind: 'int', optional: true }, { key: 'r', kind: 'int', optional: true }], save: 'ignore' },
  { name: 'save', args: [], save: 'true' },
  { name: 'listops', args: [], save: 'ignore' },
]

const OP_MAP = new Map<string, OpSpec>()
for (const o of OPS) OP_MAP.set(o.name, o)
export function opSpec(name: string): OpSpec | undefined {
  return OP_MAP.get(name)
}

export interface ParsedCommand {
  op: string
  args: Record<string, number | string | boolean>
  /** 信封 save 字段值；undefined=不发 save（ignore 面）。 */
  save: boolean | undefined
}

export type ParseResult = { ok: true; cmd: ParsedCommand } | { ok: false; error: string }

function parseInt(v: string, key: string): number | string {
  // MiniJson 整数子集：拒绝浮点/非数字（返回错误串由调用方处理）
  if (!/^-?\d+$/.test(v)) {
    return `bad-${key}: "${v}" not an integer`
  }
  return Number(v)
}

/**
 * 速记行 → 命令。规则（spec §3/§4）：
 * - 首 token=op 名；其余按 OpSpec.args 位置序；
 * - setchest 特例：第 5 个位置参="clear" → {clear:true}（itemId/count 不发）；
 * - 尾缀关键字 `save`：仅对 save:'false' 面（tp）生效 → 信封 save:true；
 * - 可选参按剩余 token 顺次填充（give dmg / genscatter seed / drop dmg / setarmor dmg）。
 */
export function parseShorthand(line: string): ParseResult {
  const tokens = line.trim().split(/\s+/).filter((t) => t.length > 0)
  if (tokens.length === 0) {
    return { ok: false, error: 'empty command' }
  }
  const name = tokens[0]
  const spec = OP_MAP.get(name)
  if (!spec) {
    return { ok: false, error: `unknown op: ${name} (listops = authority)` }
  }
  let rest = tokens.slice(1)

  // 尾缀 save 关键字（tp 类）
  let saveOverride = false
  if (rest.length > 0 && rest[rest.length - 1] === 'save' && spec.save === 'false') {
    saveOverride = true
    rest = rest.slice(0, -1)
  }

  const args: Record<string, number | string | boolean> = {}

  // setchest clear 特例：slot 后跟 "clear"
  if (name === 'setchest' && rest.length === 5 && rest[4] === 'clear') {
    for (let i = 0; i < 4; i++) {
      const v = parseInt(rest[i], spec.args[i].key)
      if (typeof v === 'string') return { ok: false, error: v }
      args[spec.args[i].key] = v
    }
    args.clear = true
    return { ok: true, cmd: { op: name, args, save: true } }
  }

  const required = spec.args.filter((a) => !a.optional)
  if (rest.length < required.length) {
    return { ok: false, error: `${name} needs ${required.map((a) => a.key).join(' ')} (got ${rest.length})` }
  }
  if (rest.length > spec.args.length) {
    return { ok: false, error: `${name} takes at most ${spec.args.length} args (got ${rest.length})` }
  }
  for (let i = 0; i < rest.length; i++) {
    const a = spec.args[i]
    if (a.kind === 'str') {
      args[a.key] = rest[i]
    } else {
      const v = parseInt(rest[i], a.key)
      if (typeof v === 'string') return { ok: false, error: v }
      args[a.key] = v
    }
  }
  const save = spec.save === 'ignore' ? undefined : spec.save === 'true' ? true : saveOverride ? true : undefined
  return { ok: true, cmd: { op: name, args, save } }
}

let seq = 0
/** 信封 id：`<ms>-<seq>`（幂等面=案卷协议）。 */
export function nextId(now: number): string {
  seq = (seq + 1) % 1000
  return `${now}-${seq}`
}

/**
 * 命令 → 协议信封 JSON（严格子集：整数=Long、字符串、布尔；无浮点）。
 * tmp 文件名=`<id>.json`（与 CLI 投递同形）。
 */
export function buildEnvelope(cmd: ParsedCommand, id: string): string {
  const parts: string[] = [`"protocolVersion":1`, `"id":${JSON.stringify(id)}`, `"op":${JSON.stringify(cmd.op)}`]
  const a = Object.keys(cmd.args).map((k) => {
    const v = cmd.args[k]
    return `${JSON.stringify(k)}:${typeof v === 'number' ? String(v) : JSON.stringify(v)}`
  })
  parts.push(`"args":{${a.join(',')}}`)
  if (cmd.save !== undefined) {
    parts.push(`"save":${cmd.save ? 'true' : 'false'}`)
  }
  return `{${parts.join(',')}}`
}
