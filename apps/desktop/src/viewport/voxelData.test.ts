/**
 * voxelData 无头测试（C1 验收点①-⑤）：
 * 坐标映射、空气零几何、面剔除计数（全实心 16³=外壳 1536 面）、区域边界、配色。
 */
import { describe, expect, it } from 'vitest'
import {
  BLOCK_COLORS,
  CHUNK_SIZE,
  UNKNOWN_COLOR,
  blockIdAt,
  buildChunk,
  colorForId,
} from './voxelData'
import { BID, makeMockChunkIds } from './mockWorld'

const N = CHUNK_SIZE

function fullSolid(id: number): Uint8Array {
  return new Uint8Array(N ** 3).fill(id)
}

function withBlocks(blocks: Array<[x: number, y: number, z: number, id: number]>): Uint8Array {
  const ids = new Uint8Array(N ** 3)
  for (const [x, y, z, id] of blocks) ids[x + N * (z + N * y)] = id
  return ids
}

describe('buildChunk 面剔除计数', () => {
  it('全实心 16³ 只出外壳面：6×16×16=1536 面', () => {
    const m = buildChunk(fullSolid(BID.STONE))
    expect(m.faceCount).toBe(6 * N * N) // 1536
    expect(m.positions.length).toBe(1536 * 4 * 3)
    expect(m.colors.length).toBe(1536 * 4 * 3)
    expect(m.indices.length).toBe(1536 * 6)
  })

  it('全空气零几何', () => {
    const m = buildChunk(new Uint8Array(N ** 3))
    expect(m.faceCount).toBe(0)
    expect(m.positions.length).toBe(0)
    expect(m.colors.length).toBe(0)
    expect(m.indices.length).toBe(0)
  })

  it('相邻两实心块共享面被剔除：2 块=12 面 - 2 共享=10 面', () => {
    const m = buildChunk(withBlocks([[5, 5, 5, BID.STONE], [6, 5, 5, BID.STONE]]))
    expect(m.faceCount).toBe(10)
  })

  it('单块六面齐全', () => {
    const m = buildChunk(withBlocks([[7, 7, 7, BID.STONE]]))
    expect(m.faceCount).toBe(6)
  })
})

describe('区域边界', () => {
  it('角块 (0,0,0)：3 个越界邻域按边界出面，共 6 面', () => {
    const m = buildChunk(withBlocks([[0, 0, 0, BID.STONE]]))
    expect(m.faceCount).toBe(6)
  })

  it('顶面块 (8,15,8) 上表面越界仍出几何（bbox 达 y=16）', () => {
    const m = buildChunk(withBlocks([[8, 15, 8, BID.STONE]]))
    let maxY = -Infinity
    for (let i = 1; i < m.positions.length; i += 3) maxY = Math.max(maxY, m.positions[i])
    expect(maxY).toBe(16)
  })

  it('越界查询返回 -1（区域外=无方块）', () => {
    const ids = fullSolid(BID.STONE)
    expect(blockIdAt(ids, -1, 0, 0)).toBe(-1)
    expect(blockIdAt(ids, N, 0, 0)).toBe(-1)
    expect(blockIdAt(ids, 0, 0, 0)).toBe(BID.STONE)
  })

  it('非法长度直接抛错', () => {
    expect(() => buildChunk(new Uint8Array(100))).toThrow()
  })
})

describe('坐标映射（MC y-up → three y-up 直接映射）', () => {
  it('单块 (3,5,7) 占据立方体 [3,4]×[5,6]×[7,8]', () => {
    const m = buildChunk(withBlocks([[3, 5, 7, BID.STONE]]))
    let minX = Infinity, minY = Infinity, minZ = Infinity
    let maxX = -Infinity, maxY = -Infinity, maxZ = -Infinity
    for (let i = 0; i < m.positions.length; i += 3) {
      minX = Math.min(minX, m.positions[i])
      minY = Math.min(minY, m.positions[i + 1])
      minZ = Math.min(minZ, m.positions[i + 2])
      maxX = Math.max(maxX, m.positions[i])
      maxY = Math.max(maxY, m.positions[i + 1])
      maxZ = Math.max(maxZ, m.positions[i + 2])
    }
    expect([minX, minY, minZ]).toEqual([3, 5, 7])
    expect([maxX, maxY, maxZ]).toEqual([4, 6, 8])
  })

  it('+X 面三角形绕序朝外（叉积 x 分量为正）', () => {
    const m = buildChunk(withBlocks([[0, 0, 0, BID.STONE]]))
    const p = m.positions
    // 找全 3 顶点都在 x=1 平面的三角形
    for (let t = 0; t < m.indices.length; t += 3) {
      const a = m.indices[t] * 3
      const b = m.indices[t + 1] * 3
      const c = m.indices[t + 2] * 3
      if (p[a] !== 1 || p[b] !== 1 || p[c] !== 1) continue
      const ux = p[b] - p[a], uy = p[b + 1] - p[a + 1], uz = p[b + 2] - p[a + 2]
      const vx = p[c] - p[a], vy = p[c + 1] - p[a + 1], vz = p[c + 2] - p[a + 2]
      const nx = uy * vz - uz * vy
      expect(nx).toBeGreaterThan(0)
      return
    }
    throw new Error('未找到 +X 面三角形')
  })
})

describe('方块 ID → 颜色映射', () => {
  it('不同 ID 颜色互不相同（含水/石/草/土/基岩/铁矿）', () => {
    const ids = [1, 2, 3, 7, 9, 16]
    const seen = new Set(ids.map((i) => colorForId(i).join(',')))
    expect(seen.size).toBe(ids.length)
  })

  it('未知非空 ID 用兜底色', () => {
    expect(colorForId(99)).toEqual(UNKNOWN_COLOR)
  })

  it('顶点色与块 ID 对齐：全草块产物颜色恒为草色', () => {
    const m = buildChunk(fullSolid(BID.GRASS))
    const grass = BLOCK_COLORS[BID.GRASS]
    for (let i = 0; i < m.colors.length; i += 3) {
      expect(m.colors[i]).toBeCloseTo(grass[0], 6)
      expect(m.colors[i + 1]).toBeCloseTo(grass[1], 6)
      expect(m.colors[i + 2]).toBeCloseTo(grass[2], 6)
    }
  })
})

describe('mockWorld 确定性体数据', () => {
  it('同种子两次生成逐字节一致', () => {
    expect(makeMockChunkIds()).toEqual(makeMockChunkIds())
  })

  it('分层正确：y=0 全基岩、y=9..15 全空气、洞心为空气', () => {
    const ids = makeMockChunkIds()
    for (let z = 0; z < N; z++) {
      for (let x = 0; x < N; x++) {
        expect(blockIdAt(ids, x, 0, z)).toBe(BID.BEDROCK)
        expect(blockIdAt(ids, x, 10, z)).toBe(BID.AIR)
      }
    }
    expect(blockIdAt(ids, 8, 3, 8)).toBe(BID.AIR)
  })

  it('水坑：水面 6×6=36 面，侧/底被实心邻域剔除', () => {
    const m = buildChunk(makeMockChunkIds())
    const water = BLOCK_COLORS[BID.WATER]
    let waterFaces = 0
    for (let f = 0; f < m.faceCount; f++) {
      const i = f * 4 * 3 // 每面首顶点（colors 为 Float32，按 1e-6 容差比）
      if (
        Math.abs(m.colors[i] - water[0]) < 1e-6 &&
        Math.abs(m.colors[i + 1] - water[1]) < 1e-6 &&
        Math.abs(m.colors[i + 2] - water[2]) < 1e-6
      )
        waterFaces++
    }
    expect(waterFaces).toBe(36)
  })

  it('空气洞在石层内部新增内壳面：总面数 > 同形状实心外壳 1088', () => {
    // 实心区为 y=0..8 共 9 层：外壳=底256+顶256+侧4×16×9=576 → 1088；
    // 洞凿在石层内部，只增面不减（洞壁+土/岩底外露面）。
    const m = buildChunk(makeMockChunkIds())
    expect(m.faceCount).toBeGreaterThan(1088)
  })
})
