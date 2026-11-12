import { ref, type Ref } from 'vue'
import type { Inspectable } from '@/mock/types'

const selected: Ref<Inspectable | null> = ref(null)

/** 左面板点选 → Inspector 展示的共享选中态。 */
export function useSelection() {
  function select(item: Inspectable | null) {
    selected.value = item
  }
  return { selected, select }
}
