<script setup lang="ts">
import { useWorldSource } from '@/composables/useWorldSource'
import TreeItem from './TreeItem.vue'

const source = useWorldSource()
const tree = source.getWorldTree()
</script>

<template>
  <div class="h-full overflow-y-auto py-1" role="tree" aria-label="World tree">
    <TreeItem v-for="node in tree" :key="node.id" :node="node" :depth="0" />
  </div>
</template>
