// D8/D5 批守卫：别名归一（零协议）+ 新四 op + flag 解析 + clear≠clearinv 陷阱
import { describe, expect, it } from 'vitest'
import { OPS, buildEnvelope, opSpec, parseShorthand } from './shorthand'

const okCmd = (line: string) => {
  const r = parseShorthand(line)
  if (!r.ok) throw new Error(r.error)
  return r.cmd
}

describe('D5 别名表（体验层，零协议）', () => {
  it('单词别名 → canonical op', () => {
    expect(okCmd('setblock 1 64 1 4').op).toBe('place')
    expect(okCmd('summon pig 1 64 1').op).toBe('spawn')
    expect(okCmd('spawnpoint 1 2 3').op).toBe('setspawn')
    expect(okCmd('teleport 1 64 1').op).toBe('tp')
    expect(okCmd('rotate 90 0').op).toBe('setlook')
    expect(okCmd('save-all').op).toBe('save')
  })
  it('两段式前缀别名', () => {
    const t = okCmd('time set 1000')
    expect(t.op).toBe('settime')
    expect(t.args.time).toBe(1000)
    const d = okCmd('data get 1 64 1')
    expect(d.op).toBe('getblock')
    expect(d.args.x).toBe(1)
  })
  it('别名后 args 按 canonical spec 校验', () => {
    expect(parseShorthand('setblock 1 64 1').ok).toBe(false) // place 缺 blockId
    expect(parseShorthand('summon pig 1 64').ok).toBe(false)
  })
  it('陷阱在案：clear ≠ clearinv（不互为别名，各自 canonical）', () => {
    expect(okCmd('clear').op).toBe('clear')
    expect(okCmd('clear 1 64 1 5').op).toBe('clear')
    expect(okCmd('clearinv').op).toBe('clearinv')
    expect(opSpec('clear')!.args.length).toBe(4)
    expect(opSpec('clearinv')!.args.length).toBe(0)
  })
  it('信封 JSON 恒 canonical op（别名不外泄协议）', () => {
    const env = buildEnvelope(okCmd('setblock 1 64 1 4'), '1-a')
    expect(env).toContain('"op":"place"')
    expect(env).toContain('"blockId":4')
  })
})

describe('D8 新四 op', () => {
  it('kill/clone/setdifficulty positional', () => {
    expect(okCmd('kill 1 64 1 5').args).toEqual({ x: 1, y: 64, z: 1, r: 5 })
    expect(okCmd('clone 1 64 1 2 64 2 10 64 10').args.tx).toBe(10)
    expect(okCmd('setdifficulty 2').args.level).toBe(2)
  })
  it('weather flag 面：true/false 字面量', () => {
    const w = okCmd('weather true false')
    expect(w.args.raining).toBe(true)
    expect(w.args.thunder).toBe(false)
    const w2 = okCmd('weather true') // thunder 可选缺省=不发
    expect(w2.args.thunder).toBeUndefined()
    expect(parseShorthand('weather rain').ok).toBe(false) // 非字面量拒
    expect(parseShorthand('weather 1').ok).toBe(false)
  })
  it('写侧 save 信封', () => {
    expect(okCmd('kill 1 64 1 5').save).toBe(true)
    expect(okCmd('weather true').save).toBe(true)
  })
  it('KNOWN_OPS 对榫：40 op 全量在册', () => {
    expect(OPS.length).toBe(40)
    expect(opSpec('kill')).toBeDefined()
    expect(opSpec('clone')).toBeDefined()
    expect(opSpec('setdifficulty')).toBeDefined()
    expect(opSpec('weather')).toBeDefined()
    expect(opSpec('getbiome')).toBeDefined() // 补洞：agent 侧在册壳侧曾漏
  })
  it('getbiome 只读面（save 不发）', () => {
    const g = okCmd('getbiome 100 100')
    expect(g.op).toBe('getbiome')
    expect(g.args).toEqual({ x: 100, z: 100 })
    expect(g.save).toBeUndefined()
  })
})
