<script setup lang="ts">
import { useSelection } from '@/composables/useSelection'
import { useWorldSource } from '@/composables/useWorldSource'
import { cn } from '@/lib/utils'

const source = useWorldSource()
const layers = source.getAnalysisLayers()
const { selected, select } = useSelection()
</script>

<template>
  <div class="flex h-full flex-col overflow-y-auto py-1">
    <button
      v-for="layer in layers"
      :key="layer.id"
      type="button"
      :class="cn(
        'mx-1 flex items-center gap-2 rounded-sm px-2 py-1.5 text-left text-xs hover:bg-elevated',
        selected?.id === layer.id && 'bg-elevated ring-1 ring-inset ring-primary/40',
      )"
      @click="select(layer)"
    >
      <span
        class="size-2 shrink-0 rounded-full"
        :class="layer.status === 'available' ? 'bg-primary' : 'bg-muted-foreground/40'"
      />
      <span class="min-w-0 flex-1">
        <span class="block truncate font-medium">{{ layer.title }}</span>
        <span class="block truncate text-[10px] text-muted-foreground">{{ layer.subtitle }}</span>
      </span>
      <span
        class="shrink-0 rounded-full border border-border px-1.5 py-0.5 font-mono text-[10px]"
        :class="layer.status === 'available' ? 'text-primary' : 'text-muted-foreground'"
      >
        {{ layer.status }}
      </span>
    </button>
  </div>
</template>
