<script setup lang="ts">
/**
 * VoxelViewport：T2-C1 静态体素渲染最小片。
 * 数据流（判决）：mockWorld 4096 方块 ID → voxelData.buildChunk 解码
 * → 单个 BufferGeometry → MeshBasicMaterial(顶点色) → Three.js 场景。
 * 第一版不含 Entity/光照/tick/chunk 流式。浏览器与 Tauri 同一通路（纯 mock）。
 */
import * as THREE from 'three'
import { OrbitControls } from 'three/addons/controls/OrbitControls.js'
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { buildChunk } from './voxelData'
import { makeMockChunkIds } from './mockWorld'

// 顶点色按 sRGB 直通：关掉 three 的颜色管理，避免二次编码失真
THREE.ColorManagement.enabled = false

const container = ref<HTMLElement | null>(null)
const faceCount = ref(0)

let renderer: THREE.WebGLRenderer | null = null
let scene: THREE.Scene | null = null
let camera: THREE.PerspectiveCamera | null = null
let controls: OrbitControls | null = null
let geometry: THREE.BufferGeometry | null = null
let material: THREE.MeshBasicMaterial | null = null
let rafId = 0
let ro: ResizeObserver | null = null

onMounted(() => {
  const el = container.value
  if (!el) return

  renderer = new THREE.WebGLRenderer({ antialias: true })
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  renderer.setSize(el.clientWidth, el.clientHeight)
  el.appendChild(renderer.domElement)

  scene = new THREE.Scene()
  scene.background = new THREE.Color(0x0d1117)

  // 坐标映射：MC y-up → three y-up 直接映射（见 voxelData 头注），
  // 相机对准区域中心 (8,4,8)（实体层集中在 y=0..8）
  camera = new THREE.PerspectiveCamera(
    55,
    Math.max(el.clientWidth / Math.max(el.clientHeight, 1), 0.0001),
    0.1,
    1000,
  )
  camera.position.set(26, 20, 26)

  controls = new OrbitControls(camera, renderer.domElement)
  controls.target.set(8, 4, 8)
  controls.enableDamping = true
  controls.update()

  const mesh = buildChunk(makeMockChunkIds())
  faceCount.value = mesh.faceCount
  geometry = new THREE.BufferGeometry()
  geometry.setAttribute('position', new THREE.BufferAttribute(mesh.positions, 3))
  geometry.setAttribute('color', new THREE.BufferAttribute(mesh.colors, 3))
  geometry.setIndex(new THREE.BufferAttribute(mesh.indices, 1))
  material = new THREE.MeshBasicMaterial({ vertexColors: true })
  scene.add(new THREE.Mesh(geometry, material))

  ro = new ResizeObserver(() => {
    if (!renderer || !camera) return
    const w = el.clientWidth
    const h = Math.max(el.clientHeight, 1)
    renderer.setSize(w, h)
    camera.aspect = w / h
    camera.updateProjectionMatrix()
  })
  ro.observe(el)

  const loop = () => {
    controls?.update()
    if (renderer && scene && camera) renderer.render(scene, camera)
    rafId = requestAnimationFrame(loop)
  }
  loop()
})

onBeforeUnmount(() => {
  cancelAnimationFrame(rafId)
  ro?.disconnect()
  ro = null
  controls?.dispose()
  controls = null
  geometry?.dispose()
  geometry = null
  material?.dispose()
  material = null
  if (renderer) {
    renderer.dispose()
    renderer.domElement.remove()
    renderer = null
  }
  scene = null
})
</script>

<template>
  <div class="relative h-full min-h-0 w-full overflow-hidden bg-background">
    <div ref="container" class="absolute inset-0" />
    <div
      class="pointer-events-none absolute left-2 top-2 rounded border border-border bg-panel/80 px-2 py-0.5 font-mono text-xs text-muted-foreground"
    >
      体素视口 · mock 16³ · 面数 {{ faceCount }}
    </div>
    <div
      class="pointer-events-none absolute bottom-2 left-2 font-mono text-[10px] text-muted-foreground/60"
    >
      拖拽旋转 · 滚轮缩放 · 右键平移
    </div>
  </div>
</template>
