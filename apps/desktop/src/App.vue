<script setup lang="ts">
import {
  ResizableHandle,
  ResizablePanel,
  ResizablePanelGroup,
} from '@/components/ui/resizable'
import { TooltipProvider } from '@/components/ui/tooltip'
import AnalysisLayers from '@/components/analysis/AnalysisLayers.vue'
import ConsolePanel from '@/components/console/ConsolePanel.vue'
import PanelShell from '@/components/layout/PanelShell.vue'
import StatusBar from '@/components/layout/StatusBar.vue'
import TopBar from '@/components/layout/TopBar.vue'
import ViewportPlaceholder from '@/components/layout/ViewportPlaceholder.vue'
import InspectorPanel from '@/components/inspector/InspectorPanel.vue'
import WorldTree from '@/components/explorer/WorldTree.vue'
import AgentCard from '@/components/runtime/AgentCard.vue'
import type { WorkspaceId } from '@/mock/types'
import { computed, ref, type ComponentPublicInstance } from 'vue'

const workspace = ref<WorkspaceId>('explorer')

const LEFT_TITLES: Record<WorkspaceId, string> = {
  explorer: 'World Explorer',
  analysis: 'Analysis Layers',
  runtime: 'Runtime Agent',
}
const leftTitle = computed(() => LEFT_TITLES[workspace.value])

const leftPanel = ref<ComponentPublicInstance | null>(null)
const rightPanel = ref<ComponentPublicInstance | null>(null)
const bottomPanel = ref<ComponentPublicInstance | null>(null)

const leftCollapsed = ref(false)
const rightCollapsed = ref(false)
const bottomCollapsed = ref(false)

// reka SplitterPanel 经 useForwardExpose 暴露 collapse()/expand()
function toggle(panel: ComponentPublicInstance | null, collapsed: boolean) {
  const api = panel as unknown as { collapse?: () => void; expand?: () => void } | null
  if (collapsed) api?.expand?.()
  else api?.collapse?.()
}
</script>

<template>
  <TooltipProvider>
    <div class="flex h-screen min-h-[600px] min-w-[940px] flex-col overflow-hidden bg-background text-foreground">
      <TopBar v-model:workspace="workspace" />

      <ResizablePanelGroup direction="vertical" class="min-h-0 flex-1">
        <ResizablePanel :min-size="45" class="min-h-0">
          <ResizablePanelGroup direction="horizontal" class="min-h-0">
            <ResizablePanel
              ref="leftPanel"
              :default-size="22"
              :min-size="3"
              :max-size="40"
              collapsible
              :collapsed-size="3"
              class="min-w-0"
              @collapse="leftCollapsed = true"
              @expand="leftCollapsed = false"
            >
              <PanelShell
                :title="leftTitle"
                side="left"
                :collapsed="leftCollapsed"
                @toggle="toggle(leftPanel, leftCollapsed)"
              >
                <WorldTree v-if="workspace === 'explorer'" />
                <AnalysisLayers v-else-if="workspace === 'analysis'" />
                <AgentCard v-else />
              </PanelShell>
            </ResizablePanel>

            <ResizableHandle with-handle />

            <ResizablePanel :min-size="25" class="min-w-0">
              <ViewportPlaceholder :workspace-label="leftTitle" />
            </ResizablePanel>

            <ResizableHandle with-handle />

            <ResizablePanel
              ref="rightPanel"
              :default-size="23"
              :min-size="3"
              :max-size="40"
              collapsible
              :collapsed-size="3"
              class="min-w-0"
              @collapse="rightCollapsed = true"
              @expand="rightCollapsed = false"
            >
              <PanelShell
                title="Inspector"
                side="right"
                :collapsed="rightCollapsed"
                @toggle="toggle(rightPanel, rightCollapsed)"
              >
                <InspectorPanel />
              </PanelShell>
            </ResizablePanel>
          </ResizablePanelGroup>
        </ResizablePanel>

        <ResizableHandle with-handle />

        <ResizablePanel
          ref="bottomPanel"
          :default-size="23"
          :min-size="3"
          :max-size="45"
          collapsible
          :collapsed-size="3"
          @collapse="bottomCollapsed = true"
          @expand="bottomCollapsed = false"
        >
          <PanelShell
            title="Console"
            side="bottom"
            :collapsed="bottomCollapsed"
            @toggle="toggle(bottomPanel, bottomCollapsed)"
          >
            <ConsolePanel />
          </PanelShell>
        </ResizablePanel>
      </ResizablePanelGroup>

      <StatusBar />
    </div>
  </TooltipProvider>
</template>
