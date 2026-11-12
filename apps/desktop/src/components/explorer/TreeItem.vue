<script setup lang="ts">
import { useSelection } from '@/composables/useSelection'
import { cn } from '@/lib/utils'
import type { WorldTreeNode } from '@/mock/types'
import { Boxes, ChevronDown, ChevronRight, File, Folder, FolderOpen } from '@lucide/vue'
import { computed, ref } from 'vue'

const props = defineProps<{ node: WorldTreeNode; depth: number }>()

const { selected, select } = useSelection()
const expanded = ref(props.node.kind !== 'chunk')

const isSelected = computed(() => selected.value?.id === props.node.id)

const nodeIcon = computed(() => {
  if (props.node.kind === 'world') return Boxes
  if (props.node.kind === 'chunk-dir') return expanded.value ? FolderOpen : Folder
  return File
})
</script>

<template>
  <div>
    <div
      role="treeitem"
      :aria-expanded="node.children ? expanded : undefined"
      :aria-selected="isSelected"
      :style="{ paddingLeft: `${depth * 12 + 4}px` }"
      :class="cn(
        'flex h-6 cursor-default items-center gap-1.5 rounded-sm pr-2 text-xs hover:bg-elevated',
        isSelected && 'bg-elevated ring-1 ring-inset ring-primary/40',
      )"
      @click="select(node)"
    >
      <button
        v-if="node.children?.length"
        class="flex size-4 shrink-0 items-center justify-center rounded-sm text-muted-foreground hover:text-foreground"
        @click.stop="expanded = !expanded"
      >
        <ChevronDown v-if="expanded" :size="12" />
        <ChevronRight v-else :size="12" />
      </button>
      <span v-else class="size-4 shrink-0" />

      <component :is="nodeIcon" :size="13" class="shrink-0 text-muted-foreground" />
      <span class="truncate">{{ node.title }}</span>
      <span v-if="node.subtitle" class="ml-auto truncate font-mono text-[10px] text-muted-foreground/70">
        {{ node.subtitle }}
      </span>
    </div>

    <div v-if="expanded && node.children?.length" role="group">
      <TreeItem
        v-for="child in node.children"
        :key="child.id"
        :node="child"
        :depth="depth + 1"
      />
    </div>
  </div>
</template>
