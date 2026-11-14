import type { WorldSource } from '@/mock/types'
import { createMockSource } from '@/mock/source'
import { inTauri } from '@/channel/ipc'
import { useChannel } from '@/channel/useChannel'

let singleton: WorldSource | null = null

/**
 * 展示组件唯一的数据入口。Tauri 环境=真实通道 ChannelSource（T1 接线），
 * 浏览器 dev=mock（原型走查通路保留）。接口形状不变判决兑现。
 */
export function useWorldSource(): WorldSource {
  if (!singleton) {
    singleton = inTauri() ? useChannel().source : createMockSource()
  }
  return singleton
}
