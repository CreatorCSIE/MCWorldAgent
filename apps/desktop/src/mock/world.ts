import type { AnalysisLayer, WorldTreeNode } from './types'

/**
 * mock 形状以真实资产为参照：
 * - 版本 id：infdev 20100327；世界名：World2
 * - 存储布局：saves/<world>/<base36(x)>/<base36(z)>.dat 散件 + level.dat（pre-Anvil）
 * - 磁盘 chunk 总数：1729（agent 案卷 G2 证据）
 * 树本身只展开 mock 子集（5 个 base36 目录 / 12 个 chunk）。
 */

const VERSION_ID = 'infdev 20100327'
const WORLD_NAME = 'World2'

function chunkNode(dirBase36: string, chunkX: number, zBase36: string, chunkZ: number): WorldTreeNode {
  const path = `saves/${WORLD_NAME}/${dirBase36}/${zBase36}.dat`
  return {
    id: `chunk:${path}`,
    kind: 'chunk',
    title: `${zBase36}.dat`,
    subtitle: `chunk (${chunkX}, ${chunkZ})`,
    fields: [
      { label: 'path', value: path, mono: true },
      { label: 'chunkX', value: `${chunkX}  (base36 "${dirBase36}")`, mono: true },
      { label: 'chunkZ', value: `${chunkZ}  (base36 "${zBase36}")`, mono: true },
      { label: 'format', value: 'chunk.dat · gzip NBT' },
      { label: 'source', value: 'disk scan (mock)' },
    ],
  }
}

function dirNode(base36: string, chunkX: number, chunks: WorldTreeNode[]): WorldTreeNode {
  return {
    id: `dir:${base36}`,
    kind: 'chunk-dir',
    title: `${base36}/`,
    subtitle: `base36(x) = ${chunkX}`,
    fields: [
      { label: 'path', value: `saves/${WORLD_NAME}/${base36}/`, mono: true },
      { label: 'chunkX', value: `${chunkX}`, mono: true },
      { label: 'base36 dir', value: `"${base36}"`, mono: true },
      { label: 'listed chunks', value: `${chunks.length} (mock subset)` },
    ],
    children: chunks,
  }
}

export const MOCK_WORLD_TREE: WorldTreeNode[] = [
  {
    id: 'world:World2',
    kind: 'world',
    title: WORLD_NAME,
    subtitle: VERSION_ID,
    fields: [
      { label: 'name', value: WORLD_NAME },
      { label: 'version', value: VERSION_ID },
      { label: 'save layout', value: `saves/<world>/<base36(x)>/<base36(z)>.dat`, mono: true },
      { label: 'level.dat', value: `saves/${WORLD_NAME}/level.dat`, mono: true },
      { label: 'chunks on disk', value: '1729', mono: true },
      { label: 'tree shown', value: '5 dirs / 12 chunks (mock subset)' },
    ],
    children: [
      dirNode('0', 0, [
        chunkNode('0', 0, '0', 0),
        chunkNode('0', 0, '1', 1),
        chunkNode('0', 0, '-1', -1),
      ]),
      dirNode('-1', -1, [
        chunkNode('-1', -1, '0', 0),
        chunkNode('-1', -1, '1', 1),
        chunkNode('-1', -1, '-1', -1),
      ]),
      dirNode('1a', 46, [
        chunkNode('1a', 46, '0', 0),
        chunkNode('1a', 46, '2f', 87),
        chunkNode('1a', 46, '-1', -1),
      ]),
      dirNode('1b', 47, [
        chunkNode('1b', 47, '2f', 87),
        chunkNode('1b', 47, '2e', 86),
      ]),
      dirNode('2f', 87, [
        chunkNode('2f', 87, '1a', 46),
        chunkNode('2f', 87, '1b', 47),
      ]),
    ],
  },
]

export const MOCK_ANALYSIS_LAYERS: AnalysisLayer[] = [
  {
    id: 'layer:slime',
    kind: 'layer',
    title: 'Slime Chunk Mapping',
    subtitle: 'seed-derived deterministic slime chunks',
    status: 'available',
    fields: [
      { label: 'status', value: 'available (mock)' },
      { label: 'input', value: 'World2 seed + chunk grid' },
      { label: 'output', value: 'viewport chunk overlay' },
    ],
  },
  {
    id: 'layer:coverage',
    kind: 'layer',
    title: 'Chunk Coverage Map',
    subtitle: 'disk chunks vs agent load counts',
    status: 'available',
    fields: [
      { label: 'status', value: 'available (mock)' },
      { label: 'input', value: 'disk scan: 1729 chunks', mono: true },
      { label: 'cross-check', value: 'loadChunk miss = 639 (G2 evidence)', mono: true },
      { label: 'note', value: 'disk count exceeds miss count: generation bypasses loadChunk layer' },
    ],
  },
  {
    id: 'layer:monolith',
    kind: 'layer',
    title: 'Monolith Mapping',
    subtitle: 'historical structure corpus',
    status: 'placeholder',
    fields: [
      { label: 'status', value: 'placeholder' },
      { label: 'planned input', value: 'archaeology workspace structure index' },
    ],
  },
  {
    id: 'layer:terrain',
    kind: 'layer',
    title: 'Terrain Overlays',
    subtitle: 'height / biome style overlays',
    status: 'placeholder',
    fields: [
      { label: 'status', value: 'placeholder' },
      { label: 'planned input', value: 'chunk NBT height maps' },
    ],
  },
  {
    id: 'layer:histstruct',
    kind: 'layer',
    title: 'Historical Structure Detection',
    subtitle: 'pattern match over chunk data',
    status: 'placeholder',
    fields: [
      { label: 'status', value: 'placeholder' },
      { label: 'planned input', value: 'structure fingerprints per version case file' },
    ],
  },
]
