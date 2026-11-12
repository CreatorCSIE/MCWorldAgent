<script setup lang="ts">
import { useSelection } from '@/composables/useSelection'
import { useWorldSource } from '@/composables/useWorldSource'
import { cn } from '@/lib/utils'
import { computed } from 'vue'

const source = useWorldSource()
const status = source.getAgentStatus()
const { selected, select } = useSelection()

const liveTicks = computed(() => status.baseline.ticks + source.tick.value)
</script>

<template>
  <div class="flex h-full flex-col gap-3 overflow-y-auto p-2 text-xs">
    <!-- 连接状态 -->
    <section class="rounded-md border border-border bg-elevated p-2.5">
      <div class="flex items-center justify-between">
        <span class="font-medium uppercase tracking-wider text-muted-foreground">Agent connection</span>
        <span class="flex items-center gap-1.5">
          <span
            class="size-2 rounded-full"
            :class="source.connection.value === 'connected' ? 'bg-primary' : 'bg-destructive'"
          />
          <span>{{ source.connection.value }}</span>
        </span>
      </div>
      <p class="mt-1.5 font-mono text-[11px] text-muted-foreground">
        premain attached · {{ status.versionId }}
      </p>
      <p class="font-mono text-[11px] break-all text-muted-foreground/70">
        args=[{{ status.agentArgs }}]
      </p>
    </section>

    <!-- 进程信息 -->
    <section>
      <h3 class="mb-1 font-medium uppercase tracking-wider text-muted-foreground">Process</h3>
      <dl class="grid grid-cols-[auto_1fr] gap-x-3 gap-y-0.5">
        <dt class="text-muted-foreground">java</dt>
        <dd class="font-mono">{{ status.process.javaVersion }}</dd>
        <dt class="text-muted-foreground">pid</dt>
        <dd class="font-mono tabular-nums">{{ status.process.pid }}</dd>
        <dt class="text-muted-foreground">launcher</dt>
        <dd class="font-mono">{{ status.process.launcher }}</dd>
        <dt class="text-muted-foreground">runtime</dt>
        <dd class="font-mono">{{ status.process.runtime }}</dd>
        <dt class="text-muted-foreground">jar sha1</dt>
        <dd class="font-mono break-all text-[10px]">{{ status.jarSha1 }}</dd>
      </dl>
    </section>

    <!-- 把手① -->
    <section>
      <h3 class="mb-1 font-medium uppercase tracking-wider text-muted-foreground">Handle ① capture</h3>
      <dl class="grid grid-cols-[auto_1fr] gap-x-3 gap-y-0.5">
        <dt class="text-muted-foreground">capturedWorld</dt>
        <dd class="font-mono break-all" :class="status.handle1.capturedWorld ? 'text-primary' : 'text-muted-foreground'">
          {{ status.handle1.capturedWorld ?? '— not captured' }}
        </dd>
        <dt class="text-muted-foreground">capturedSaveDir</dt>
        <dd class="font-mono break-all" :class="status.handle1.capturedSaveDir ? 'text-primary' : 'text-muted-foreground'">
          {{ status.handle1.capturedSaveDir ?? '— not captured' }}
        </dd>
      </dl>
      <p class="mt-1 text-[10px] text-muted-foreground/70">
        two halves: world-ctor records save dir, save-world captures live World
      </p>
    </section>

    <!-- 钩子列表 -->
    <section>
      <h3 class="mb-1 font-medium uppercase tracking-wider text-muted-foreground">
        Hooks ({{ status.hooks.length }})
      </h3>
      <div class="flex flex-col gap-1">
        <button
          v-for="hook in status.hooks"
          :key="hook.id"
          type="button"
          :class="cn(
            'rounded-md border border-border bg-elevated px-2 py-1.5 text-left hover:border-primary/40',
            selected?.id === hook.id && 'ring-1 ring-inset ring-primary/40',
          )"
          @click="select(hook)"
        >
          <span class="flex items-center justify-between gap-2">
            <span class="font-mono text-[11px] font-medium">{{ hook.hookId }}</span>
            <span
              class="shrink-0 rounded-full border border-border px-1.5 font-mono text-[9px] uppercase"
            >
              {{ hook.injectMode === 'entry' ? 'entry' : 'entry+result' }}
            </span>
          </span>
          <span class="mt-0.5 block truncate text-[10px] text-muted-foreground">{{ hook.subtitle }}</span>
        </button>
      </div>
    </section>

    <!-- 计数 -->
    <section>
      <h3 class="mb-1 font-medium uppercase tracking-wider text-muted-foreground">Counters</h3>
      <dl class="grid grid-cols-[1fr_auto] gap-x-3 gap-y-0.5 font-mono text-[11px] tabular-nums">
        <dt class="text-muted-foreground">ticks (live)</dt>
        <dd>{{ liveTicks }}</dd>
        <dt class="text-muted-foreground">chunk enter</dt>
        <dd>{{ status.baseline.chunkEnter }}</dd>
        <dt class="text-muted-foreground">chunk hit</dt>
        <dd>{{ status.baseline.chunkHit }}</dd>
        <dt class="text-muted-foreground">chunk miss</dt>
        <dd>{{ status.baseline.chunkMiss }}</dd>
        <dt class="text-muted-foreground">light recalc</dt>
        <dd>{{ status.baseline.lightRecalc }}</dd>
        <dt class="text-muted-foreground">saveWorld calls</dt>
        <dd>{{ status.baseline.saveCalls }}</dd>
      </dl>
    </section>

    <!-- autosave 代价 -->
    <section class="rounded-md border border-destructive/30 bg-elevated p-2.5">
      <h3 class="font-medium uppercase tracking-wider text-destructive">Autosave (G1)</h3>
      <p class="mt-1 font-mono text-[10px] text-muted-foreground">
        vanilla 0327: every {{ status.autosave.vanillaIntervalTicks }} world-ticks
      </p>
      <p class="font-mono text-[10px] text-muted-foreground">
        legacyfix: {{ status.autosave.legacyfixInterval }}
      </p>
      <p class="mt-1 text-[10px]">{{ status.autosave.note }}</p>
    </section>
  </div>
</template>
