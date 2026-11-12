<script setup lang="ts">
import { Button } from '@/components/ui/button'
import { useWorldSource } from '@/composables/useWorldSource'
import type { ConsoleLine } from '@/mock/types'
import { Pause, Play } from '@lucide/vue'
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'

const LINE_CAP = 300

const source = useWorldSource()
const lines = ref<ConsoleLine[]>(source.getConsoleHistory().slice(-LINE_CAP))
const paused = ref(false)
const scroller = ref<HTMLElement | null>(null)
const buffer: ConsoleLine[] = []
let unsub: (() => void) | null = null

function append(line: ConsoleLine) {
  lines.value.push(line)
  if (lines.value.length > LINE_CAP) lines.value.splice(0, lines.value.length - LINE_CAP)
}

function scrollToBottom() {
  const el = scroller.value
  if (el) el.scrollTop = el.scrollHeight
}

function onLine(line: ConsoleLine) {
  if (paused.value) {
    buffer.push(line)
    if (buffer.length > LINE_CAP) buffer.splice(0, buffer.length - LINE_CAP)
    return
  }
  append(line)
  void nextTick(scrollToBottom)
}

function togglePause() {
  paused.value = !paused.value
  if (!paused.value && buffer.length) {
    for (const line of buffer) append(line)
    buffer.length = 0
    void nextTick(scrollToBottom)
  }
}

onMounted(() => {
  unsub = source.subscribeConsole(onLine)
  scrollToBottom()
})

onBeforeUnmount(() => {
  unsub?.()
})
</script>

<template>
  <div class="flex h-full min-h-0 flex-col bg-background">
    <div class="flex h-6 shrink-0 items-center justify-between border-b border-border px-2">
      <span class="font-mono text-[10px] text-muted-foreground">
        mock hook stream · {{ lines.length }} lines
      </span>
      <Button
        variant="ghost"
        size="icon-xs"
        :title="paused ? 'Resume console scroll' : 'Pause console scroll'"
        @click="togglePause"
      >
        <Play v-if="paused" />
        <Pause v-else />
      </Button>
    </div>

    <div ref="scroller" class="min-h-0 flex-1 overflow-y-auto py-1">
      <p
        v-for="line in lines"
        :key="line.id"
        class="flex gap-2 px-2 font-mono text-[11px] leading-5"
      >
        <span class="w-16 shrink-0 text-right tabular-nums text-muted-foreground/50">
          {{ line.elapsedMs }}ms
        </span>
        <span
          class="min-w-0 break-all"
          :class="{
            'text-muted-foreground': line.level === 'info',
            'text-foreground': line.level === 'event',
            'text-destructive': line.level === 'warn',
          }"
        >
          {{ line.text }}
        </span>
      </p>
    </div>
  </div>
</template>
