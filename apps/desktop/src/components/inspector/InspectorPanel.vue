<script setup lang="ts">
import { useSelection } from '@/composables/useSelection'

const { selected } = useSelection()
</script>

<template>
  <div class="h-full overflow-y-auto">
    <div
      v-if="!selected"
      class="flex h-full items-center justify-center p-4 text-center text-xs text-muted-foreground"
    >
      Select an object in the left panel
      <br>
      to inspect its fields
    </div>

    <div v-else class="flex flex-col gap-3 p-2 text-xs">
      <div class="rounded-md border border-border bg-elevated p-2.5">
        <div class="flex items-center justify-between gap-2">
          <span class="truncate font-medium">{{ selected.title }}</span>
          <span class="shrink-0 rounded-full border border-border px-1.5 py-0.5 font-mono text-[9px] uppercase text-muted-foreground">
            {{ selected.kind }}
          </span>
        </div>
        <p v-if="selected.subtitle" class="mt-0.5 truncate font-mono text-[10px] text-muted-foreground">
          {{ selected.subtitle }}
        </p>
      </div>

      <dl class="grid grid-cols-[auto_1fr] gap-x-3 gap-y-1.5 px-1">
        <template v-for="field in selected.fields" :key="field.label">
          <dt class="text-muted-foreground">{{ field.label }}</dt>
          <dd class="min-w-0 break-all" :class="field.mono ? 'font-mono text-[11px]' : ''">
            {{ field.value }}
          </dd>
        </template>
      </dl>
    </div>
  </div>
</template>
