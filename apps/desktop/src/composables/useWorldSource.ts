import type { WorldSource } from '@/mock/types'
import { createMockSource } from '@/mock/source'

let singleton: WorldSource | null = null

/**
 * 展示组件唯一的数据入口。当前返回 mock 实现；
 * 接真实 agent / Tauri 通道时在此切换实现，接口形状不变。
 */
export function useWorldSource(): WorldSource {
  if (!singleton) singleton = createMockSource()
  return singleton
}
