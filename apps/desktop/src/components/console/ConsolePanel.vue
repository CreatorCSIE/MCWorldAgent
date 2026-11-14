<script setup lang="ts">
import { Button } from '@/components/ui/button'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { useWorldSource } from '@/composables/useWorldSource'
import { useChannel } from '@/channel/useChannel'
import type { ConsoleLine } from '@/mock/types'
import { Pause, Play } from '@lucide/vue'
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'

const LINE_CAP = 300

const source = useWorldSource()
const channel = useChannel()
const lines = ref<ConsoleLine[]>(source.getConsoleHistory().slice(-LINE_CAP))
const paused = ref(false)
const scroller = ref<HTMLElement | null>(null)
const cmdScroller = ref<HTMLElement | null>(null)
const input = ref('')
const historyIdx = ref(-1)
const shellHistory = ref<string[]>([])
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

async function submit() {
  const line = input.value.trim()
  if (line.length === 0) return
  shellHistory.value.push(line)
  historyIdx.value = -1
  input.value = ''
  await channel.send(line)
  void nextTick(() => {
    const el = cmdScroller.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

function recall(dir: -1 | 1) {
  if (shellHistory.value.length === 0) return
  const n = shellHistory.value.length
  historyIdx.value = Math.min(n - 1, Math.max(0, (historyIdx.value === -1 ? n : historyIdx.value) + dir))
  const idx = historyIdx.value >= n ? n - 1 : historyIdx.value
  if (idx >= 0 && idx < n) input.value = shellHistory.value[n - 1 - idx]
}

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'ArrowUp') {
    e.preventDefault()
    recall(-1)
  } else if (e.key === 'ArrowDown') {
    e.preventDefault()
    recall(1)
  }
}

const PHASE_CLASS: Record<string, string> = {
  posted: 'text-muted-foreground',
  accepted: 'text-primary',
  terminal: '',
  'rejected-shell': 'text-destructive',
}

onMounted(() => {
  unsub = source.subscribeConsole(onLine)
  channel.start()
  scrollToBottom()
})

onBeforeUnmount(() => {
  unsub?.()
  channel.stop()
})
</script>

<template>
  <div class="flex h-full min-h-0 flex-col bg-background">
    <Tabs default-value="events" class="min-h-0 flex-1">
      <div class="flex h-6 shrink-0 items-center gap-2 border-b border-border px-2">
        <TabsList class="h-5 gap-1 border-0 bg-transparent p-0 text-[10px]">
          <TabsTrigger value="command" class="rounded-none px-1.5 py-0.5 text-[10px] data-[state=active]:bg-muted data-[state=active]:text-foreground">
            命令台
          </TabsTrigger>
          <TabsTrigger value="events" class="rounded-none px-1.5 py-0.5 text-[10px] data-[state=active]:bg-muted data-[state=active]:text-foreground">
            事件流 · {{ lines.length }}
          </TabsTrigger>
        </TabsList>
        <span v-if="!channel.isTauri" class="font-mono text-[10px] text-muted-foreground/60">
          browser mock（假投递回声）
        </span>
        <span v-else-if="channel.mappingDrift.value" class="truncate font-mono text-[10px] text-yellow-500">
          {{ channel.mappingDrift.value }}
        </span>
        <Button
          variant="ghost"
          size="icon-xs"
          class="ml-auto"
          :title="paused ? 'Resume console scroll' : 'Pause console scroll'"
          @click="togglePause"
        >
          <Play v-if="paused" />
          <Pause v-else />
        </Button>
      </div>

      <TabsContent value="command" class="m-0 min-h-0 flex-1 overflow-hidden data-[state=inactive]:hidden">
        <div class="flex h-full min-h-0 flex-col">
          <!-- 通道配置 -->
          <div class="flex shrink-0 items-center gap-1 border-b border-border/50 px-2 py-1">
            <input
              v-model="channel.mcaDir.value"
              placeholder="mca 通道目录（out/<run>/mca）"
              class="h-5 min-w-0 flex-1 border border-border bg-transparent px-1 font-mono text-[10px] text-foreground outline-none focus:border-primary/60"
              @change="channel.reconnect()"
            />
            <input
              v-model="channel.hooksLog.value"
              placeholder="agent-hooks.log 路径"
              class="h-5 min-w-0 flex-1 border border-border bg-transparent px-1 font-mono text-[10px] text-foreground outline-none focus:border-primary/60"
              @change="channel.reconnect()"
            />
            <Button variant="ghost" size="xs" class="shrink-0 font-mono text-[10px]" @click="channel.reconnect()">
              重连
            </Button>
          </div>

          <!-- 命令历史 -->
          <div ref="cmdScroller" class="min-h-0 flex-1 overflow-y-auto py-1">
            <p
              v-for="c in channel.commands"
              :key="c.id"
              class="flex gap-2 px-2 font-mono text-[11px] leading-5"
            >
              <span class="w-28 shrink-0 truncate text-right text-muted-foreground/50">{{ c.id }}</span>
              <span class="shrink-0" :class="PHASE_CLASS[c.phase]">
                {{ c.phase === 'terminal' ? (c.status ?? '?') : c.phase }}
              </span>
              <span class="min-w-0 shrink-0 text-foreground">{{ c.line }}</span>
              <span v-if="c.detail" class="min-w-0 break-all text-muted-foreground">{{ c.detail }}</span>
            </p>
            <p v-if="channel.commands.length === 0" class="px-2 py-1 font-mono text-[10px] text-muted-foreground/60">
              速记示例：getinfo | place 2 71 2 4 | fill 8 64 8 14 64 14 4 | tp 10 70 10 save | listops
            </p>
          </div>

          <!-- 输入行 -->
          <div class="flex h-7 shrink-0 items-center gap-1 border-t border-border px-2">
            <span class="font-mono text-[11px] text-primary">&gt;</span>
            <input
              v-model="input"
              class="h-5 min-w-0 flex-1 bg-transparent font-mono text-[11px] text-foreground outline-none"
              placeholder="op 位置参数（Enter 投递 / ↑↓ 历史）"
              @keydown="onKeydown"
              @keydown.enter="submit"
            />
          </div>
        </div>
      </TabsContent>

      <TabsContent value="events" class="m-0 min-h-0 flex-1 overflow-hidden data-[state=inactive]:hidden">
        <div ref="scroller" class="h-full overflow-y-auto py-1">
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
      </TabsContent>
    </Tabs>
  </div>
</template>
