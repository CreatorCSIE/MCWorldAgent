<script setup lang="ts">
defineProps<{ workspaceLabel: string }>()
</script>

<template>
  <div class="relative h-full min-h-0 w-full overflow-hidden bg-background">
    <div class="viewport-grid absolute inset-0" />
    <div class="viewport-grid-major absolute inset-0" />

    <div class="absolute left-2 top-2 rounded border border-border bg-panel/80 px-2 py-0.5 text-xs text-muted-foreground">
      {{ workspaceLabel }} · World2 · infdev 20100327
    </div>

    <div class="pointer-events-none absolute inset-0 grid place-items-center">
      <div class="text-center">
        <p class="font-mono text-sm uppercase tracking-[0.3em] text-muted-foreground">
          3D viewport placeholder
        </p>
        <p class="mt-2 font-mono text-xs text-muted-foreground/60">
          no renderer attached — shell mock, Three.js deliberately absent
        </p>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 线框网格：只用主题 token 变量，不写死颜色 */
.viewport-grid {
  background-image:
    linear-gradient(to right, var(--border) 1px, transparent 1px),
    linear-gradient(to bottom, var(--border) 1px, transparent 1px);
  background-size: 44px 44px;
}

.viewport-grid-major {
  background-image:
    linear-gradient(to right, color-mix(in oklab, var(--primary) 22%, transparent) 1px, transparent 1px),
    linear-gradient(to bottom, color-mix(in oklab, var(--primary) 22%, transparent) 1px, transparent 1px);
  background-size: 220px 220px;
}
</style>
