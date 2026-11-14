/**
 * mockWorld：确定性测试体数据生成器（C1 用 mock，不接活体通道）。
 * 分层：基岩(y=0) / 石(y=1..5，内含空气洞+铁矿斑) / 土(y=6..7) / 草(y=8) / 空(y=9..15)，
 * 外加一个 6×6 水坑。随机部分用固定种子 mulberry32，绝不用 Math.random，
 * 保证任何机器上产物逐字节一致（面剔除计数断言才可依赖）。
 */
import { CHUNK_SIZE } from './voxelData'

/** 方块 ID 常量（与 voxelData 配色表对齐）。 */
export const BID = {
  AIR: 0,
  STONE: 1,
  GRASS: 2,
  DIRT: 3,
  BEDROCK: 7,
  WATER: 9,
  IRON_ORE: 16,
} as const

/** mulberry32：32 位定种子 PRNG，确定性抖动源。 */
function mulberry32(seed: number): () => number {
  let a = seed >>> 0
  return () => {
    a = (a + 0x6d2b79f5) >>> 0
    let t = a
    t = Math.imul(t ^ (t >>> 15), t | 1)
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61)
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296
  }
}

/** 生成 16×16×16 方块 ID 数组（索引布局见 voxelData：x + 16*(z + 16*y)）。 */
export function makeMockChunkIds(seed = 20100327): Uint8Array {
  const ids = new Uint8Array(CHUNK_SIZE ** 3)
  const rnd = mulberry32(seed)

  const set = (x: number, y: number, z: number, id: number) => {
    ids[x + CHUNK_SIZE * (z + CHUNK_SIZE * y)] = id
  }
  const get = (x: number, y: number, z: number): number =>
    ids[x + CHUNK_SIZE * (z + CHUNK_SIZE * y)]

  // 逐层铺设
  for (let y = 0; y < CHUNK_SIZE; y++) {
    for (let z = 0; z < CHUNK_SIZE; z++) {
      for (let x = 0; x < CHUNK_SIZE; x++) {
        if (y === 0) set(x, y, z, BID.BEDROCK)
        else if (y <= 5) set(x, y, z, BID.STONE)
        else if (y <= 7) set(x, y, z, BID.DIRT)
        else if (y === 8) set(x, y, z, BID.GRASS)
        // y>=9 保持空气
      }
    }
  }

  // 空气洞：以 (8,3,8) 为心的近球腔（欧氏距离 ≤ 2.5），凿穿石层内部
  for (let y = 1; y <= 5; y++) {
    for (let z = 0; z < CHUNK_SIZE; z++) {
      for (let x = 0; x < CHUNK_SIZE; x++) {
        const dx = x - 8
        const dy = y - 3
        const dz = z - 8
        if (dx * dx + dy * dy + dz * dz <= 6.25) set(x, y, z, BID.AIR)
      }
    }
  }

  // 铁矿斑：定种子撒 12 颗，只替换石层实心格（洞内不生成）
  let placed = 0
  let guard = 0
  while (placed < 12 && guard < 500) {
    guard++
    const x = Math.floor(rnd() * CHUNK_SIZE)
    const y = 1 + Math.floor(rnd() * 5)
    const z = Math.floor(rnd() * CHUNK_SIZE)
    if (get(x, y, z) === BID.STONE) {
      set(x, y, z, BID.IRON_ORE)
      placed++
    }
  }

  // 水坑：x/z ∈ [4..9]，草层换水（水面与草地齐平），坑底土保留
  for (let z = 4; z <= 9; z++) {
    for (let x = 4; x <= 9; x++) {
      set(x, 8, z, BID.WATER)
    }
  }

  return ids
}
