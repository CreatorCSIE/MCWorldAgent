<script setup lang="ts">
import { useWorldSource } from '@/composables/useWorldSource'
import { useChannel } from '@/channel/useChannel'

const source = useWorldSource()
const channel = useChannel()

const STATE_LABEL: Record<string, string> = {
  unknown: 'CHANNEL UNKNOWN',
  'alive-title': 'ALIVE @TITLE',
  'alive-inworld': 'ALIVE @WORLD — 停止前先 Esc 存退',
  dead: 'PROCESS EXITED',
}
const STATE_CLASS: Record<string, string> = {
  unknown: 'bg-muted text-muted-foreground',
  'alive-title': 'bg-primary/15 text-primary',
  'alive-inworld': 'bg-destructive/20 text-destructive font-semibold',
  dead: 'bg-muted text-muted-foreground',
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
    <span class="flex items-center gap-1.5 rounded px-1.5" :class="STATE_CLASS[channel.processState.value]">
      {{ STATE_LABEL[channel.processState.value] }}
    </span>
    <span class="tabular-nums">TICK {{ source.tick.value }}</span>
    <span class="ml-auto truncate">
      {{ channel.worldInfo.value || 'World2 · infdev 20100327' }}
    </span>
  </footer>
</template>
