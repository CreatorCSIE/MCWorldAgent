<script setup lang="ts">
import { Button } from '@/components/ui/button'
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from '@/components/ui/tooltip'
import { useWorldSource } from '@/composables/useWorldSource'
import type { WorkspaceId } from '@/mock/types'
import { cn } from '@/lib/utils'

defineProps<{ workspace: WorkspaceId }>()
const emit = defineEmits<{ 'update:workspace': [WorkspaceId] }>()

const source = useWorldSource()

const MENUS = ['File', 'Edit', 'View', 'Tools', 'Help'] as const

const WORKSPACES: { id: WorkspaceId; label: string }[] = [
  { id: 'explorer', label: 'World Explorer' },
  { id: 'analysis', label: 'World Analysis' },
  { id: 'runtime', label: 'Runtime' },
]
</script>

<template>
  <header class="flex h-10 shrink-0 items-center gap-2 border-b border-border bg-panel px-2">
    <span class="px-1 text-sm font-semibold tracking-tight">MCWorldAgent</span>

    <nav class="flex items-center gap-0.5">
      <Tooltip v-for="menu in MENUS" :key="menu">
        <TooltipTrigger as-child>
          <Button variant="ghost" size="xs" class="text-muted-foreground">
            {{ menu }}
          </Button>
        </TooltipTrigger>
        <TooltipContent>Placeholder — no real menu wired yet</TooltipContent>
      </Tooltip>
    </nav>

    <div class="flex-1" />

    <div class="flex items-center gap-0.5 rounded-lg border border-border bg-background p-0.5">
      <Button
        v-for="ws in WORKSPACES"
        :key="ws.id"
        size="xs"
        :variant="workspace === ws.id ? 'secondary' : 'ghost'"
        :class="cn(
          'text-muted-foreground',
          workspace === ws.id && 'text-foreground font-medium',
        )"
        @click="emit('update:workspace', ws.id)"
      >
        {{ ws.label }}
      </Button>
    </div>

    <Tooltip>
      <TooltipTrigger as-child>
        <span
          class="flex items-center gap-1.5 rounded-full border border-border bg-elevated px-2.5 py-1 text-xs font-medium"
        >
          <span
            class="size-1.5 rounded-full"
            :class="source.mode.value === 'agent' ? 'bg-primary' : 'bg-muted-foreground'"
          />
          {{ source.mode.value === 'agent' ? 'Agent' : 'Standalone' }}
        </span>
      </TooltipTrigger>
      <TooltipContent>Execution mode (mock) — Agent = live client attached</TooltipContent>
    </Tooltip>
  </header>
</template>
