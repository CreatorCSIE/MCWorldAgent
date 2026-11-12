<script setup lang="ts">
import { useWorldSource } from '@/composables/useWorldSource'

const source = useWorldSource()

const CONNECTION_LABEL: Record<string, string> = {
  connected: 'AGENT CONNECTED',
  disconnected: 'AGENT OFFLINE',
  error: 'AGENT ERROR',
}
</script>

<template>
  <footer
    class="flex h-6 shrink-0 items-center gap-4 overflow-hidden whitespace-nowrap border-t border-border bg-panel px-2 font-mono text-[11px] text-muted-foreground"
  >
    <span>
      X {{ source.playerPos.value.x }}  Y {{ source.playerPos.value.y }}  Z {{ source.playerPos.value.z }}
    </span>
    <span class="uppercase">{{ source.mode.value === 'agent' ? 'mode agent' : 'mode standalone' }}</span>
    <span class="flex items-center gap-1.5">
      <span
        class="size-1.5 rounded-full"
        :class="source.connection.value === 'connected' ? 'bg-primary' : 'bg-destructive'"
      />
      {{ CONNECTION_LABEL[source.connection.value] }}
    </span>
    <span class="tabular-nums">TICK {{ source.tick.value }}</span>
    <span class="ml-auto">World2 · 1729 chunks · infdev 20100327</span>
  </footer>
</template>
