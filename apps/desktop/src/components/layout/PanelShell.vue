<script setup lang="ts">
import { Button } from '@/components/ui/button'
import {
  ChevronsDown,
  ChevronsLeft,
  ChevronsRight,
  ChevronsUp,
} from '@lucide/vue'
import { computed } from 'vue'

const props = defineProps<{
  title: string
  side: 'left' | 'right' | 'bottom'
  collapsed: boolean
}>()

const emit = defineEmits<{ toggle: [] }>()

const collapseIcon = computed(() =>
  props.side === 'left'
    ? ChevronsLeft
    : props.side === 'right'
      ? ChevronsRight
      : ChevronsDown,
)

const expandIcon = computed(() =>
  props.side === 'left'
    ? ChevronsRight
    : props.side === 'right'
      ? ChevronsLeft
      : ChevronsUp,
)
</script>

<template>
  <div class="flex h-full min-h-0 flex-col bg-panel">
    <!-- 折叠后：窄条，只留展开把手 -->
    <div
      v-if="collapsed"
      class="flex size-full items-center justify-center py-2"
      :class="side === 'bottom' ? 'flex-row px-2' : 'flex-col'"
    >
      <Button variant="ghost" size="icon-xs" :title="`Expand ${title}`" @click="emit('toggle')">
        <component :is="expandIcon" />
      </Button>
    </div>

    <template v-else>
      <div class="flex h-8 shrink-0 items-center justify-between gap-2 border-b border-border px-2">
        <span class="truncate text-xs font-medium uppercase tracking-wider text-muted-foreground">
          {{ title }}
        </span>
        <div class="flex shrink-0 items-center gap-1">
          <slot name="extra" />
          <Button variant="ghost" size="icon-xs" :title="`Collapse ${title}`" @click="emit('toggle')">
            <component :is="collapseIcon" />
          </Button>
        </div>
      </div>
      <div class="min-h-0 flex-1">
        <slot />
      </div>
    </template>
  </div>
</template>
