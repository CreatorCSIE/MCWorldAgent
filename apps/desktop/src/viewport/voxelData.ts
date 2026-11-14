/**
 * voxelData：体素解码纯函数层（T2-C1 垂直切片首步）。
 * 输入=16×16×16 方块 ID 数组（Uint8Array，正好一个 getblocks 上限区），
 * 输出=可直接喂给 THREE.BufferGeometry 的顶点/颜色/索引 TypedArray。
 * 零 three 依赖、零 vue 依赖，无头可测。
 *
 * 坐标映射判决：Minecraft 世界坐标 y 轴向上，Three.js 默认同为 y-up，
 * 故采用直接映射不翻转——方块 (x,y,z) 占据 three 空间立方体
 * [x,x+1]×[y,y+1]×[z,z+1]，MC 的 y=0（基岩底）对应 three 的 y=0。
 *
 * 数组索引布局：idx = x + SIZE_X * (z + SIZE_Z * y)（y 最外层，逐层扫描）。
 *
 * 面剔除规则（6 邻域）：某面生成几何 ⇔ 相邻格为空气(id=0) 或 越出区域边界；
 * 相邻两实心块共享面不生成；空气块零几何。
 */

/** 区域尺寸（与 getblocks 上限区一致）。 */
export const CHUNK_SIZE = 16

/** 方块 ID → sRGB 颜色（0-1 浮点三分量）。第一版无纹理，纯色区分即验收点①。 */
export const BLOCK_COLORS: Record<number, readonly [number, number, number]> = {
  1: [0.5, 0.5, 0.52], // 石头 stone
  2: [0.36, 0.62, 0.24], // 草方块 grass
  3: [0.44, 0.3, 0.19], // 泥土 dirt
  7: [0.28, 0.28, 0.3], // 基岩 bedrock
  9: [0.2, 0.42, 0.8], // 水 water
  16: [0.72, 0.5, 0.35], // 铁矿 iron ore
}

/** 未知非空 ID 的兜底色（品红，肉眼可辨"配色表漏了"）。 */
export const UNKNOWN_COLOR: readonly [number, number, number] = [1, 0, 1]

/** id=0 为空气；其余一律实心（水也按实心参与面剔除，v1 判决）。 */
export function isSolid(id: number): boolean {
  return id !== 0
}

export function colorForId(id: number): readonly [number, number, number] {
  return BLOCK_COLORS[id] ?? UNKNOWN_COLOR
}

/** 按布局取 ID；越界返回 -1（调用侧按"区域外=无方块=该出边界面"处理）。 */
export function blockIdAt(ids: Uint8Array, x: number, y: number, z: number): number {
  if (x < 0 || y < 0 || z < 0 || x >= CHUNK_SIZE || y >= CHUNK_SIZE || z >= CHUNK_SIZE) return -1
  return ids[x + CHUNK_SIZE * (z + CHUNK_SIZE * y)]
}

export interface VoxelMeshData {
  /** 每面 4 顶点 × xyz。 */
  positions: Float32Array
  /** 与顶点对齐的 rgb（sRGB 0-1）。 */
  colors: Float32Array
  /** 每面 2 三角形 × 3 索引（CCW 朝外，配合 FrontSide）。 */
  indices: Uint32Array
  /** 生成的面数（全实心 16³ 应为 6×16×16=1536，即只剩外壳）。 */
  faceCount: number
}

/** 6 面定义：[邻域偏移, 单位立方体 4 角(相对块角, CCW-from-outside)]。 */
const FACES: ReadonlyArray<{
  n: readonly [number, number, number]
  c: readonly (readonly [number, number, number])[]
}> = [
  { n: [1, 0, 0], c: [[1, 0, 1], [1, 0, 0], [1, 1, 0], [1, 1, 1]] }, // +X
  { n: [-1, 0, 0], c: [[0, 0, 0], [0, 0, 1], [0, 1, 1], [0, 1, 0]] }, // -X
  { n: [0, 1, 0], c: [[0, 1, 1], [1, 1, 1], [1, 1, 0], [0, 1, 0]] }, // +Y
  { n: [0, -1, 0], c: [[0, 0, 0], [1, 0, 0], [1, 0, 1], [0, 0, 1]] }, // -Y
  { n: [0, 0, 1], c: [[0, 0, 1], [1, 0, 1], [1, 1, 1], [0, 1, 1]] }, // +Z
  { n: [0, 0, -1], c: [[1, 0, 0], [0, 0, 0], [0, 1, 0], [1, 1, 0]] }, // -Z
]

/**
 * 手写几何合并：单遍扫描 16³，实心块逐面查 6 邻域，
 * 需要出几何的面直接追加进扁平数组，最后一次性转 TypedArray。
 * 全空气输入产出零几何（faceCount=0、三数组皆空）。
 */
export function buildChunk(ids: Uint8Array): VoxelMeshData {
  if (ids.length !== CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE) {
    throw new Error(`体数据长度须为 ${CHUNK_SIZE}^3=${CHUNK_SIZE ** 3}，实得 ${ids.length}`)
  }

  const pos: number[] = []
  const col: number[] = []
  const idx: number[] = []
  let faceCount = 0

  for (let y = 0; y < CHUNK_SIZE; y++) {
    for (let z = 0; z < CHUNK_SIZE; z++) {
      for (let x = 0; x < CHUNK_SIZE; x++) {
        const id = ids[x + CHUNK_SIZE * (z + CHUNK_SIZE * y)]
        if (!isSolid(id)) continue
        const [r, g, b] = colorForId(id)
        for (const face of FACES) {
          const nb = blockIdAt(ids, x + face.n[0], y + face.n[1], z + face.n[2])
          // 邻域实心→共享面剔除；邻域空气(-1 越界视同无方块)→出边界/外露面
          if (nb !== -1 && isSolid(nb)) continue
          const base = faceCount * 4
          for (const [cx, cy, cz] of face.c) {
            pos.push(x + cx, y + cy, z + cz)
            col.push(r, g, b)
          }
          idx.push(base, base + 1, base + 2, base, base + 2, base + 3)
          faceCount++
        }
      }
    }
  }

  return {
    positions: new Float32Array(pos),
    colors: new Float32Array(col),
    indices: new Uint32Array(idx),
    faceCount,
  }
}
