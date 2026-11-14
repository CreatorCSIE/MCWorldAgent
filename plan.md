# MCWorldAgent
## Minecraft Classic World Agent

> **Status:** Phase 0 complete (2026-11-12); Phase 1 write-side takeover verified — "first brick" place→explicit-save→re-enter read-back closed loop live-run on infdev 20100327 (2026-11-13); remaining planning sections still draft
> **Date:** 2026-11-13
> **Project Type:** Standalone Desktop Application
> **Target:** Minecraft Classic, Indev, Infdev, Alpha and Beta (pre-release history)
>
> **Important Notice:** This document describes the initial development direction, not a finalized technical specification. All planned features, architecture decisions, implementation priorities and compatibility assumptions may be modified, replaced, postponed or removed according to actual source-code investigation, prototype results and compatibility testing.

---

## 0. Reference Documents

This plan should be read together with the project's existing preliminary source-code research document.

The two documents have different responsibilities:

| Document | Purpose |
|---|---|
| Preliminary Source-Code Research | Records previously investigated source code, technical findings, historical implementation details and initial compatibility discoveries. |
| `plan.md` | Describes the proposed project architecture, development direction, module boundaries, UI structure and implementation roadmap. |
| Research Integration Memo | Maps confirmed research findings onto this plan: module cross-references, mandatory registry facts, vertical-slice version candidates and Phase 0 gaps. |
| Archaeology Workspace Handbook | The `minecraft-verification-analysis` workspace operating manual: version-existence indexes, evidence discipline (jar tri-state, SHA1 gate, source-tree-first-then-bytecode), physical jar base locations, standard forensics commands and a per-version readable source-tree corpus. |
| Phase 0 PoC Case File | Verified vertical-slice evidence: hook fingerprint table, Attic harness chain, old-verifier lesson, and the G1/G2/G3 answers that later phases must design against. |
| Phase 1 First-Brick Case File | Verified write-side evidence: set-side obfuscated names, place→explicit-save→re-enter read-back runs, UNSUPPORTED failure semantics, and the kill-race lesson (never force-kill while the player is in-world). |
| Phase 1 Command-Channel Case File | Verified external submit channel v1: file-queue protocol (ACCEPTED→terminal, idempotent, strict JSON subset), place/dig/getblock/save live ops, dig-persistence across re-entry, and the stale-handle fix (WORLD_SWITCHED). |

Concrete locations:

- Preliminary Source-Code Research: [minecraft-classic-to-infdev-internal-level-api-external-reuse-analysis.md](file:///E:/4k/github-projects/minecraft-verification-analysis/minecraft-classic-to-infdev-internal-level-api-external-reuse-analysis.md) (verification-analysis workspace; Classic→Infdev 20100327 scope, evidence-graded ✓/◐/？)
- Research Integration Memo: [docs/research/research-integration.md](file:///E:/4k/github-projects/MCWorldAgent/docs/research/research-integration.md)
- Archaeology Workspace Handbook: [README.md](file:///E:/4k/github-projects/minecraft-verification-analysis/README.md), with authoritative indexes [version-index.md](file:///E:/4k/github-projects/minecraft-verification-analysis/indexes/version-index.md) (client) and [server-index.md](file:///E:/4k/github-projects/minecraft-verification-analysis/indexes/server-index.md) (server)
- Phase 0 PoC Case File: [docs/research/phase0-agent-poc.md](./docs/research/phase0-agent-poc.md)
- Phase 1 First-Brick Case File: [docs/research/phase1-first-brick.md](./docs/research/phase1-first-brick.md)
- Phase 1 Command-Channel Case File: [docs/research/phase1-command-channel.md](./docs/research/phase1-command-channel.md)

**Note:** Any further source-code investigation MCWorldAgent requires (e.g. the Alpha/Beta Phase 0 gap) should be conducted directly in the archaeology workspace following its handbook workflow, reusing its indexes, readable source trees (`src-minecraft/`, 112 trees covering Pre-Classic→Beta, including 26 Beta trees) and jar base, instead of re-establishing tooling or version facts from scratch.

**No-tree fallback:** the tree corpus covers only part of the indexed versions (~112 trees vs ~306 index rows). For a version without a readable tree, the fixed "tree-first" order degrades to direct bytecode forensics: SHA1-gated original jar → `jar tf` (entry/obfuscation layout) → `javap -p -c` down to offset level; for readability, decompile with `tools/cfr-0.152.jar` and/or attach the ProGuard maps (`client0218.tiny` / per-tree `client.tiny`). The constant-fingerprint and anchor-walk strategy in the research document §6 was designed exactly for this tree-less path (the research document's own obfuscation findings were established this way).

### 0.1 How to Use These Documents

- The source-code research document provides the initial technical evidence and investigation results.
- This plan translates the research and subsequent architectural discussions into development objectives.
- Existing research should be reused rather than unnecessarily repeated; new investigation should ride on the archaeology workspace's existing indexes, source trees and forensics workflow (see the Note above).
- Unverified assumptions must remain distinguishable from confirmed findings.
- If a proposed design conflicts with actual source behavior, the implementation and verified research take precedence.
- Both documents may be updated as new discoveries are made.

**Neither document is a permanent specification. Actual source-code behavior and implementation results take precedence over initial planning assumptions.**

---

## 1. Project Overview

Minecraft World Toolkit is an independent desktop application designed for exploring, analyzing, editing and controlling historical Minecraft worlds.

The project focuses on Minecraft versions released before the official 1.0 release, covering multiple generations of Minecraft's world representation, storage formats, terrain generation and runtime architecture.

Rather than implementing a new Minecraft game engine, the project aims to build a unified toolkit around the original Minecraft runtime and world data.

The toolkit will provide two primary operating modes:

- **Standalone Mode:** Operates directly on world files or invokes the original Minecraft JAR for offline operations.
- **Agent Mode:** Connects to a running Minecraft instance through a Java Agent, enabling runtime inspection and control.

Both modes should share a common World Data Model and Version Adapter architecture.

### 1.1 Project Goals

- Provide a unified interface for working with historical Minecraft worlds.
- Support multiple Minecraft generations without assuming identical internal implementations.
- Integrate existing independent Minecraft research and utility tools.
- Enable runtime interaction with Minecraft through Java Agent instrumentation.
- Provide a 3D World Explorer for visualization and editing.
- Expose a stable API for external applications and AI agents.
- Remain independent of any particular Minecraft launcher.

### 1.2 Non-Goals (Initial Scope)

The following are not initial development priorities:

- Replacing Minecraft with a new game engine.
- Supporting modern Minecraft versions as a primary target.
- Reimplementing every historical terrain generation algorithm.
- Building a complete replacement for WorldEdit or Axiom.
- Developing a sophisticated AI terrain-generation Skill system.
- Replacing Attic Corner Launcher or becoming a general-purpose Minecraft launcher.
- Supporting arbitrary Minecraft modifications without version-specific investigation.

These areas may be reconsidered if later research establishes a practical reason to include them.

---

## 2. Target Minecraft Versions

The initial compatibility scope covers the following historical generations:

| Generation | Initial Scope |
|---|---|
| Classic | World inspection and supported runtime interaction |
| Indev | Finite world storage, editing and exploration |
| Infdev | Transition toward chunk-based and expanding worlds |
| Alpha | Chunk-based world data and runtime interaction |
| Beta | Chunk, entity and world data operations |

Exact supported version numbers must be established through source-code investigation and compatibility testing.

The project must not assume that all versions within one generation share the same world format or runtime architecture.

Each supported version should have an explicit compatibility profile.

### 2.1 Version Compatibility Registry

The registry should describe:

- Minecraft version identification.
- Main class and runtime entry points.
- World and chunk storage format.
- World generation capabilities.
- Entity and player access mechanisms.
- Lighting capabilities.
- Available runtime hooks.
- Supported data operations.
- Known limitations and unsupported features.

A version should be marked as supported only when its relevant operations have been verified.

First registry entry: **infdev 20100327** — runtime-agent observation operations verified (hook fingerprints, storage layout `saves/<world>/<base36-x>/<base36-z>` loose `.dat` + `level.dat`, save semantics, lighting trust semantics); write operations now also verified (setBlockWithNotify placement with render notify, explicit saveWorld, re-enter read-back, failure-path semantics — see Phase 1 case file). Metadata/entity writes remain unverified.

---

## 3. Architecture Overview

The initial architecture consists of six major layers.

### 3.1 Desktop Application Layer

**Proposed technologies:**

- Vue 3
- TypeScript
- Tauri (preferred desktop host)
- Optional Electron compatibility

Responsibilities:

- Application UI.
- World Explorer interface.
- Version and instance selection.
- Toolkit configuration.
- Operation management.
- Visualization and interaction.
- Communication with Java Core.

The application UI should remain independent of the selected desktop container.

The UI should follow a mature desktop-editor layout rather than a conventional administration dashboard.

### 3.2 UI Foundation and Template Strategy

**Initial proposal: shadcn-vue + custom desktop workspace.**

The project should reuse an established Vue component system and mature layout conventions to avoid unnecessary UI design from scratch.

Proposed references:

| Reference | Intended Use |
|---|---|
| shadcn-vue | Base component system and interaction patterns |
| shadcn-vue-admin | Sidebar, navigation, command palette and general application layout |
| Blender | 3D viewport, object hierarchy, inspector and editor workspace |
| VS Code | Explorer, tabs, command palette, logs and tool panels |
| Unity Editor | Scene hierarchy, Inspector and editing workflow |
| Tauri Vue templates | Desktop application structure and host integration |

These are design and implementation references, not mandatory dependencies in their entirety.

The project should not directly inherit an entire Admin Dashboard's business structure.

#### UI Design Principles

- Reuse mature components instead of inventing basic UI controls.
- Keep the 3D viewport as the primary workspace.
- Use familiar desktop-editor interaction patterns.
- Support resizable and collapsible panels.
- Separate application navigation from world-editing tools.
- Keep UI components independent of Java Core implementation details.
- Avoid introducing complex custom visual systems before actual interaction requirements are established.

#### Initial Main Window Layout

The initial layout should follow a Blender / VS Code hybrid structure:

| Area | Responsibility |
|---|---|
| Top Bar | File, Edit, View, Tools, Help, execution mode and common actions |
| Left Panel | World Explorer, object hierarchy and analysis layers |
| Center Viewport | Three-dimensional world visualization and editing |
| Right Panel | Inspector and selected object properties |
| Bottom Panel | Console, logs, problems and operation progress |
| Status Bar | Coordinates, connection status, current mode and application state |

The panel layout should be adjustable and collapsible.

#### Initial Workspace Organization

The first version may provide three major workspaces:

1. **World Explorer**
   - World browsing.
   - Chunk and block inspection.
   - Entity and player visualization.
   - Basic editing operations.

2. **World Analysis**
   - Slime Chunk Mapping.
   - Monolith Mapping.
   - Historical structure detection.
   - Terrain analysis overlays.

3. **Runtime**
   - Agent connection status.
   - Minecraft process information.
   - Player and Entity inspection.
   - Runtime operation controls.

These workspaces should share the same application shell and, where appropriate, the same 3D viewport.

#### UI Prototype Stage

Before implementing the full Java Core integration, create a static UI prototype.

Tasks:

- [x] Establish the Vue 3 project structure.
- [x] Integrate the selected component library.
- [x] Implement the main application shell.
- [x] Implement resizable side panels. (collapse/resize verified; physical pointer drag pending manual confirmation)
- [x] Implement the 3D viewport placeholder.
- [x] Implement Explorer and Inspector placeholders.
- [x] Implement the bottom Console panel.
- [x] Establish theme and typography conventions.
- [x] Verify the layout inside Tauri.
- ~~Evaluate Electron compatibility.~~ **CANCELLED** — shell decided: Tauri only (see §11 "Desktop shell").

The prototype should validate the application layout and interaction model without requiring real Minecraft data.

The final UI structure may change after actual workflow testing.

### 3.3 Java Core

The Java Core is responsible for Minecraft-specific operations.

Responsibilities:

- Java Runtime discovery and bootstrap.
- Minecraft JAR and classpath management.
- Java Agent packaging and lifecycle.
- Bytecode transformation.
- Runtime discovery.
- Version Adapter execution.
- World and Chunk data access.
- Offline Generator execution.
- Storage adapters.
- Runtime communication.

The Java Core should operate independently from the desktop UI and should not require a particular launcher.

### 3.4 World Data Model

The World Data Model provides a unified representation of Minecraft world data.

Proposed components:

- World
- Dimension (only where applicable)
- Region / Chunk
- Block
- Entity
- Player
- Environment
- Lighting
- World Metadata

The model must support differences between historical versions.

It should not force versions without a particular feature to expose artificial equivalents.

For example:

- A version without per-block lighting should not be represented as having zero-valued lighting data.
- A finite Indev world should not be forced into a modern infinite-world representation.
- Missing chunk data must not automatically be interpreted as a request to generate new terrain.

### 3.5 Rendering Layer

**Initial proposal: Three.js + WebGL2.**

Responsibilities:

- 3D world visualization.
- Chunk mesh construction and rendering.
- Camera controls.
- Block selection.
- Entity visualization.
- Visualization overlays.
- Editing interaction.

The renderer is a presentation layer, not the authoritative Minecraft simulation engine.

Minecraft's original runtime remains responsible for gameplay simulation in Agent Mode.

In Standalone Mode, the renderer displays stored world data without implicitly running Minecraft entity AI or world simulation.

The rendering implementation may be revised after performance and integration tests.

### 3.6 External Integration Layer

Proposed interfaces:

- Launcher Provider API
- Agent Bridge Protocol
- World API
- MCP Server

These interfaces should be independently versioned where appropriate.

External AI agents should interact through stable World API operations rather than directly manipulating internal Java objects.

---

## 4. Core Development Modules

### 4.1 Version Registry and Adapters

**Priority: Foundational**

Implement a version-aware compatibility system.

Tasks:

- [ ] Define version identification.
- [ ] Define version capability descriptions.
- [ ] Define Version Adapter interfaces.
- [ ] Investigate historical Minecraft class structures.
- [ ] Investigate world storage differences.
- [ ] Document verified version-specific limitations.
- [ ] Establish compatibility tests.

The adapter architecture must be based on actual source investigation rather than assumptions derived from modern Minecraft.

### 4.2 World Storage Engine

**Priority: High**

Provide direct access to historical Minecraft world data.

Tasks:

- [ ] Identify supported world storage formats.
- [ ] Implement format-specific readers.
- [ ] Implement format-specific writers.
- [ ] Support world metadata.
- [ ] Support block data.
- [ ] Support entity data where available.
- [ ] Implement safe write operations.
- [ ] Provide backup and recovery mechanisms.
- [ ] Validate data integrity after modifications.

Storage adapters should be version-specific when formats differ substantially.

### 4.3 Chunk and World Data Management

**Priority: High**

Provide a unified interface for loading and modifying world data.

Proposed chunk states:

- ABSENT
- LOADING
- CLEAN
- DIRTY
- SAVING
- EVICTED

Requirements:

- [ ] Lazy loading where appropriate.
- [ ] Dirty data tracking.
- [ ] Safe saving and eviction.
- [ ] Distinguish missing data from generated terrain.
- [ ] Support finite-world data layouts.
- [ ] Support chunk-based world layouts.
- [ ] Avoid unintended terrain generation during inspection.
- [ ] Preserve version-specific data.

Finite worlds may use a simpler in-memory representation if actual size and data layout permit it.

### 4.4 Standalone Generator

**Priority: Medium**

Invoke the original Minecraft implementation to generate worlds offline.

The project should reuse original generation logic rather than independently reimplementing historical terrain generation algorithms.

Tasks:

- [ ] Implement Java Runtime Bootstrap.
- [ ] Resolve Minecraft JAR and dependencies.
- [ ] Construct the required classpath.
- [ ] Build version-specific generation profiles.
- [ ] Execute generation in a controlled environment.
- [ ] Capture output and errors.
- [ ] Verify generated world compatibility.
- [ ] Prevent concurrent writes with a running Minecraft instance.

Standalone Generator and Agent Mode should share lower-level Java Runtime infrastructure, but use different execution profiles.

- Headless Profile: Offline generation.
- Client Profile: Minecraft runtime with Java Agent injection.

Advanced terrain-generation Skills are explicitly outside the initial scope.

### 4.5 Java Agent and Runtime Bridge

**Priority: High**

Provide runtime interaction with Minecraft.

Initial injection strategy:

**Java Instrumentation `premain()`**

Attach-based injection is not part of the initial implementation plan.

Tasks:

- [ ] Build the Java Agent bootstrap.
- [ ] Register class transformers before Minecraft startup.
- [ ] Identify relevant Minecraft classes.
- [ ] Implement runtime discovery.
- [ ] Implement version-specific hooks.
- [ ] Establish Agent Bridge communication.
- [ ] Implement runtime lifecycle management.
- [ ] Report unsupported hooks explicitly.
- [ ] Verify transformed bytecode.

Instrumentation should use strict matching criteria:

- Class fingerprints.
- Method names and complete descriptors where available.
- Access flags.
- Instruction patterns.
- Expected match counts.
- Bytecode verification.

The system should prefer reporting an unsupported hook over applying an uncertain transformation.

Minecraft classes should generally be discovered as Minecraft loads them. The Agent should avoid unnecessarily forcing their loading during `premain()`.

**Phase 0 status:** the observation half of this module is built and live-verified (`java/agent/`: premain, fingerprint table, two-pass weaving, handle-①, tick drain, logs).

**Phase 1 status:** the write side is live-verified end-to-end — "first brick" reflection path (`FirstBrick.java`: setBlockWithNotify → explicit saveWorld → re-enter read-back → UNSUPPORTED failure semantics), zero new bytecode injection, full evidence in the Phase 1 case file. The external submit channel is now ALSO live-verified (file-queue v1: place/dig/getblock/save, dig+place persistence across re-entry — see Command-Channel case file). **Remaining-list updated 2026-11-14:** metadata/entity writes (setmeta batch), settime/setspawn/tp (tp/setw batches), batch writes (fill/replace batches) and the HUD observation hook (hud-overlay batch, EXIT mode) are now ALL live-verified — 33 ops total, see Command-Channel case file §18-23. What genuinely remains: the deeper hook layer (`getChunkFromChunkCoords`) — now carrying a hard constraint from the [0327 coverage audit](./docs/research/audit-0327-coverage.md): `getblocks`-style reads go through the provider and **in-place-generate whole unloaded chunks** (1729 vs 639 miss-count gap, Phase 0 lesson 5), so any read-only chunk export must gate on `chunkExists`/disk-direct and must not implicitly generate terrain (§3.4/§4.3 requirement). Desktop-shell command panel remains (workbench-model T1).

### 4.6 Player Controller

**Priority: Medium**

Integrate existing Player-related research and tools.

Potential operations:

- [ ] Locate player instance.
- [ ] Read player position.
- [ ] Read rotation and orientation.
- [ ] Modify player position.
- [ ] Modify movement-related state.
- [ ] Expose player status where supported.

Existing Position-Hack functionality should be investigated for integration.

The implementation should distinguish between direct runtime manipulation and ordinary game-controlled movement.

### 4.7 Entity Controller

**Priority: Medium**

Integrate existing Entity-related research.

Potential operations:

- [ ] Enumerate entities.
- [ ] Identify entity types.
- [ ] Read entity positions.
- [ ] Read entity bounding boxes.
- [ ] Modify entity state where safe.
- [ ] Support entity visualization.
- [ ] Track entity lifecycle events.

Existing Entity-Hack functionality should be evaluated as a starting point.

Entity simulation remains the responsibility of the Minecraft runtime in Agent Mode.

The 3D renderer should not independently reproduce Minecraft AI or physics.

### 4.8 World Analyzer and Structure Locator

**Priority: Medium**

Integrate existing analytical tools into the unified world coordinate system.

Potential capabilities:

- [ ] Slime Chunk Mapping.
- [ ] Infdev Pyramid Position Mapping.
- [ ] Stronghold Locator, subject to repository and version investigation.
- [ ] Monolith Mapping / Finder.
- [ ] Additional historical terrain analysis.

Existing independent repositories should be reused where practical instead of duplicating their established algorithms.

Monolith Mapping is an existing capability and should be treated as an integration task, not a new feature to develop from scratch.

### 4.9 3D World Explorer

**Priority: High for the application experience**

Provide a common visualization interface.

Initial features:

- [ ] World overview.
- [ ] Chunk visualization.
- [ ] Block inspection.
- [ ] Camera navigation.
- [ ] Entity visualization.
- [ ] Coordinate display.
- [ ] Selection and highlighting.
- [ ] Basic editing interaction.
- [ ] Multiple analytical overlays.

Potential visualization layers:

- Terrain.
- Slime Chunks.
- Monoliths.
- Brick Pyramids.
- Strongholds.
- Live Entities.
- Player Position.
- Other version-specific analysis results.

The rendering layer should use a common coordinate system while respecting version-specific world dimensions and data availability.

Lighting should be capability-driven rather than assumed to exist uniformly across historical versions.

### 4.10 MCP and External API

**Priority: Medium / Later Phase**

Expose selected Toolkit operations to external applications and AI agents.

Potential operations:

- Inspect world metadata.
- List and inspect chunks.
- Read block data.
- Query entities.
- Locate structures.
- Modify selected world data.
- Control supported runtime state.
- Retrieve operation progress.

The MCP Server should call the same World API used by the desktop interface.

MCP must not become a separate implementation of world manipulation logic.

Advanced AI Skills, including procedural terrain planning, road design and architectural planning, are deferred.

---

## 5. Attic Corner Launcher Integration

Attic Corner Launcher is an optional Launcher Provider, not a mandatory dependency.

The Toolkit must remain independently usable.

### 5.1 Integration Goals

- [ ] Support launching Toolkit from Attic.
- [ ] Support both Electron and Tauri hosts.
- [ ] Define a unified Launch Request.
- [ ] Define Toolkit process lifecycle handling.
- [ ] Define local communication protocol.
- [ ] Support Agent Mode requests.
- [ ] Support opening a selected world or instance.
- [ ] Allow Toolkit to request supported launcher operations.

### 5.2 Dual-Host Integration

Electron and Tauri should use separate host adapters while sharing the same integration contract.

Electron may use its Node.js environment.

Tauri may use Rust to launch and supervise the Toolkit process.

Both should produce equivalent launch requests and session behavior.

### 5.3 Launcher Provider API

The Toolkit should define a provider capability contract.

Potential capabilities:

- Discover Minecraft instances.
- Resolve Java Runtime.
- Launch Minecraft.
- Inject Java Agent.
- Observe game process.
- Retrieve instance metadata.

Attic may implement these capabilities according to its existing architecture.

If a capability is unavailable, Toolkit should use another provider or request user-supplied launch configuration where appropriate.

### 5.4 Avoid Duplicating Launcher Responsibilities

The Toolkit should not gradually become a second full Minecraft launcher.

Shared Java Runtime Bootstrap functionality may be reused by Standalone Generator and Direct Java Provider, but their execution profiles must remain separate.

Attic-specific functionality must remain behind the Launcher Provider interface.

---

## 6. Existing Project Integration

Existing repositories and tools should be investigated before implementing equivalent functionality.

Initial integration candidates:

| Existing Project | Planned Integration |
|---|---|
| Position-Hack | Player Controller / Runtime Discovery |
| Entity-Hack | Entity Controller |
| Slime-Chunk-Mapping | Chunk Analyzer |
| Infdev-Pyramid-Position-Mapping | Structure Locator |
| minecraft-monolith-finder | Terrain Analyzer |
| minecraft-stronghold-finder | Structure Locator, pending investigation |

Integration does not necessarily mean copying all source code into the Toolkit repository.

Possible approaches:

- Shared libraries.
- Adapter implementations.
- Independent modules.
- Reimplemented interfaces where the original architecture cannot be reused cleanly.

The final approach should be determined after repository and dependency investigation.

---

## 7. Data Ownership and Concurrency

A strict data ownership model is required.

### 7.1 Standalone Mode

Toolkit Storage Engine is the authoritative writer.

Minecraft must not concurrently modify the same world.

### 7.2 Agent Mode

The running Minecraft process is the authoritative runtime state owner.

Toolkit communicates through the Agent Bridge.

The Toolkit should not directly write the same world files while Minecraft is running.

Per Phase 0 finding G1: the game cannot be relied on to persist agent mutations — Agent Mode must explicitly invoke `saveWorld` through handle-① (queued to the tick boundary) after committed changes. **Verified in Phase 1 first-brick**: explicit saveWorld on the tick boundary persisted the placement and survived re-entry.

Kill-race discipline (Phase 1 lesson): never force-kill the game process while the player is still in-world — confirm the user is back at the title screen first, or the kill×save race can intermittently corrupt level/chunk NBT.

### 7.3 Large Operations

Bulk modifications should support:

- [ ] Batching.
- [ ] Progress reporting.
- [ ] Cancellation.
- [ ] Frame or tick budget limits where applicable.
- [ ] Error reporting.
- [ ] Undo / rollback where feasible.

Operations affecting the running game should avoid blocking Minecraft's main thread for excessive periods.

---

## 8. Development Phases

The following phases represent an initial dependency-oriented roadmap, not a fixed release schedule.

### Phase 0 — Research Consolidation and Feasibility

- [x] Review the existing preliminary source-code research.
- [x] Map existing research findings to proposed modules.
- [x] Identify confirmed facts and unresolved assumptions.
- [x] Inspect existing repositories.
- [x] Investigate target Minecraft versions.
- [x] Document world storage formats.
- [x] Identify important runtime classes.
- [x] Test Java Agent instrumentation.
- [ ] Evaluate Three.js rendering integration.
- [ ] Evaluate the selected UI component system.
- [x] Establish initial capability matrix. (first entry: infdev 20100327)
- [x] Identify major compatibility blockers.

**Exit criteria:** A technically verified architecture for at least one representative historical version, with major UI and integration assumptions evaluated.

**Status: MET for the runtime/agent half** (infdev 20100327 vertical slice, full-chain live run 2026-11-12, see case file). The UI evaluation items (Three.js, component system) are deferred to the Phase 1 static prototype, where they will actually be exercised.

#### Phase 0 verified findings (design constraints for all later phases)

1. **Agent chain works on shipped runtimes.** premain + ASM strict-fingerprint weaving, five hooks, handle-① live-World capture, tick drain queue and log evidence chain all verified end-to-end on the Attic-launched infdev 20100327 client (including its legacyfix javaagent), with clean exit and no behavioral damage.
2. **Old verifier constraint (class major=49).** Passing an uninitialized `this` as an INVOKESTATIC argument inside a constructor throws VerifyError, and the Applet launcher swallows it as a "clean exit". Instrumentation of pre-Alpha versions must follow the two-part handle pattern (constructor ENTRY captures reference-slot args only; live object captured from a guaranteed early method call such as the constructor-tail `saveWorld(false)`).
3. **G1 — no in-session autosave to rely on.** Vanilla 0327 bytecode autosaves every 200 world-ticks, but the shipped runtime's legacyfix `AutoSavePatch` rewrites that interval to (by default) 5 minutes — accepted as a known cost of mounting legacyfix as an additional javaagent, not pursued further. **Agent Mode must explicitly invoke `saveWorld` through handle-① after mutations; "quit-to-title" is not a save guarantee either.**
4. **G2 — lighting data is trusted from disk.** `readChunkNBTData` loads SkyLight/BlockLight/HeightMap as-is; `generateHeightMap` runs only when the data is missing/invalid. Verified invariant `lightRecalc == chunkMiss` (recalc only accompanies newly generated chunks). World-writing components must therefore produce valid light/height data alongside blocks, or deliberately trigger recalc.
5. **Hook-layer coverage limit.** Disk chunk count exceeded the `ChunkProviderLoadOrGenerate.loadChunk` miss count (1729 vs 639), proving generation/loading paths bypass that layer. Chunk-level interception in Phase 1+ must hook the `World.getChunkFromChunkCoords` layer for complete accounting.
6. **Attic integration path settled.** Attic exposes no CLI; the harness `require`s its `@attic/runtime` CJS dist, uses `dryRunLaunch` for the LaunchConfig and spawns the JVM itself with `-javaagent` injected — zero changes to the Attic repository, SHA1-gated jar.
7. **Version mapping drift is real.** Obfuscation names from adjacent versions (e.g. 0325 maps) are invalid for 0327; every version requires fresh javap fingerprinting before any hook claim.

### Phase 1 — Foundation and UI Prototype

**Write-side takeover pre-laid and verified (2026-11-13):** the "first brick" closed loop — place via setBlockWithNotify on the tick boundary → explicit `saveWorld` → re-enter read-back → failure-path semantics — is live-verified on infdev 20100327 (see Phase 1 case file). The external submit channel (file-queue v1, place/dig/getblock/save, persistence verified) is live-verified the same day (see Command-Channel case file). The checklist below covers the remaining foundation/UI scope.

- [ ] Create the Toolkit repository.
- [ ] Establish project structure.
- [ ] Implement Version Registry.
- [ ] Define World Data Model.
- [ ] Define Java Core communication.
- [ ] Implement Java Runtime Bootstrap.
- [x] Establish desktop UI foundation.
- [x] Integrate shadcn-vue or the selected component system.
- [x] Implement the main application shell.
- [x] Implement Explorer, Inspector and Console panels.
- [x] Implement the 3D viewport placeholder.
- [x] Verify Tauri desktop behavior.
- ~~[ ] Evaluate Electron compatibility.~~ **CANCELLED** (shell decided: Tauri only).
- [ ] Define error and logging conventions.

**Exit criteria:** Desktop application can communicate with Java Core, identify a supported Minecraft version and provide a usable static workspace prototype.

### Phase 2 — Standalone World Access

**First slice landed (2026-11-14):** `java/storage` read-only chunk exporter (NbtReader + ChunkExport CLI, pure JDK8, zero game contact) live-verified against the static World2 save — 10/10 assertions green incl. Furnace TE/items matching the te-1 live-run ledger, `present:false` for absent chunks and a 2813-file SHA1 zero-write guarantee (see [chunk-export spec](./.trae/specs/chunk-export/spec.md) and [reconcile](./integrations/attic/out/chunk-export-1/reconcile.md)). Bytecode-corrected layout facts: flat 16×16×128 chunk, Blocks idx = `x<<11|z<<7|y` (y innermost), nibble even=low/odd=high. Remaining Phase 2 scope: writers/dirty tracking/safe save (unplanned so far — Agent Mode covers live writes), multi-chunk iteration, metadata surface.

- [ ] Implement initial storage adapter. ← **first read-only adapter landed (java/storage, 2026-11-14)**
- [ ] Load world metadata.
- [x] Read blocks and chunks. (chunk-export batch, static-save verified)
- [ ] Implement dirty tracking.
- [ ] Implement safe save operations.
- [ ] Verify data preservation.
- [ ] Display basic world information.

**Exit criteria:** A supported historical world can be opened, inspected and safely saved.

### Phase 3 — 3D World Explorer

- [ ] Integrate Three.js.
- [ ] Implement world coordinate system.
- [ ] Implement Chunk Mesh Builder.
- [ ] Implement camera navigation.
- [ ] Implement block selection.
- [ ] Implement basic visualization.
- [ ] Add analytical overlays.
- [ ] Connect the viewport to Explorer and Inspector.
- [ ] Validate rendering performance.

**Exit criteria:** A supported world can be explored in 3D through the desktop application.

### Phase 4 — Runtime Agent

Foundation partly pre-laid by Phase 0 (agent observation chain live-verified on infdev 20100327; see §4.5 status).

- [ ] Implement Java Agent.
- [ ] Implement strict class and method matching.
- [ ] Implement runtime discovery.
- [ ] Implement Agent Bridge.
- [ ] Implement initial Player Controller.
- [ ] Implement initial Entity Controller.
- [ ] Validate runtime synchronization.

**Exit criteria:** Toolkit can safely inspect a running supported Minecraft instance.

### Phase 5 — Generator and Analysis Integration

- [ ] Implement Headless Generator Profile.
- [ ] Verify original generation behavior.
- [ ] Integrate existing analysis tools.
- [ ] Add additional version-specific adapters.
- [ ] Implement common analysis visualization.

**Exit criteria:** Standalone generation and selected historical analysis tools work through the unified interface.

### Phase 6 — Launcher Integration

- [ ] Define Launcher Provider API.
- [ ] Implement Attic Provider.
- [ ] Implement Electron host adapter.
- [ ] Implement Tauri host adapter.
- [ ] Establish unified launch requests.
- [ ] Establish local session communication.
- [ ] Test both desktop containers.

**Exit criteria:** Both Attic desktop variants can launch and communicate with Toolkit.

### Phase 7 — MCP and External Integration

- [ ] Define stable World API operations.
- [ ] Implement MCP Server.
- [ ] Expose read-only operations.
- [ ] Add controlled modification operations.
- [ ] Add progress and error reporting.
- [ ] Validate external agent workflows.

**Exit criteria:** External clients can operate the Toolkit through the documented API.

---

## 9. Initial Repository Structure

Proposed structure; subject to revision after implementation prototypes.

```text
minecraft-world-toolkit/
├── apps/
│   ├── desktop/
│   │   ├── src/
│   │   └── ...
│   └── ...
│
├── packages/
│   ├── ui/
│   ├── world-model/
│   ├── core-spec/
│   ├── api-client/
│   └── ...
│
├── java/
│   ├── core/
│   ├── agent/
│   ├── adapters/
│   ├── storage/
│   └── generator/
│
├── integrations/
│   ├── attic/
│   ├── mcp/
│   └── ...
│
├── docs/
│   ├── architecture/
│   ├── compatibility/
│   ├── research/
│   └── ui/
│
└── README.md
```

This is an initial organizational proposal. The final repository structure should follow actual module dependencies and build-system requirements.

---

## 10. Engineering Principles

The following principles should guide implementation.

1. **Source-Code First:** Historical Minecraft behavior must be verified through source investigation, bytecode inspection or reproducible tests.
2. **Version-Aware Design:** Do not assume modern Minecraft architecture applies to historical versions.
3. **Original Runtime Preservation:** Reuse original Minecraft generation and simulation where practical.
4. **Strict Instrumentation:** Avoid uncertain bytecode transformations.
5. **Independent Operation:** Toolkit must not depend on Attic.
6. **Shared Contracts:** Standalone, Agent, UI and MCP should use common data and operation contracts.
7. **Explicit Capability Detection:** Unsupported features must be represented explicitly.
8. **Safe Data Ownership:** Avoid concurrent writes to active worlds.
9. **Incremental Compatibility:** Add verified versions progressively.
10. **No Premature Abstraction:** Architecture may evolve when actual source investigation reveals a better model.
11. **Template-Based UI Development:** Reuse mature UI components and editor layout conventions instead of designing every interface from scratch.
12. **Viewport-Centered Workflow:** Keep the 3D World Viewport central to world exploration and editing.
13. **UI / Core Separation:** UI components should communicate through defined interfaces rather than directly depending on Minecraft internals.

---

## 11. Open Questions and Research-Dependent Items

The following decisions are intentionally unresolved.

| Topic | Current Proposal | Required Investigation |
|---|---|---|
| Renderer | Three.js / WebGL2 | Performance and Tauri integration |
| UI component system | shadcn-vue | **In use** (reka-nova, Tailwind v4, dark-first); reka-ui 2.11 forwardRef pitfall documented in docs/ui/phase1-desktop-shell/notes.md §3 |
| Main UI layout | Blender / VS Code hybrid | Workflow validation |
| Desktop host | Tauri preferred | Host integration and platform behavior |
| Electron support | Optional compatibility | Shared UI and runtime integration |
| Desktop shell | **DECIDED: Tauri only** — single shell, no Attic-style dual-shell | Component base: shadcn-vue template; workspace paradigm: Blender + VS Code (§3.2) |
| Java Core protocol | Local RPC | Process lifecycle and cross-platform behavior |
| World Model | Unified version-aware model | Historical data structure differences |
| Chunk caching | Lazy loading and dirty tracking | Actual storage and memory behavior |
| Agent hooks | Premain + ASM | **Verified on 0327** (strict matching, two-pass, old-verifier rules); per-version drift re-forensics remains |
| World editing | Shared World API | **0327 write path verified** (first-brick: setBlockWithNotify + explicit saveWorld + re-enter read-back); shared World API and broader write coverage pending |
| Generator | Original Minecraft JAR | Headless compatibility per version |
| Lighting | Capability-driven | **0327 answered**: trusted from disk, recalc only on missing/invalid (G2); other versions pending |
| Entity model | Common entity abstraction | Version-specific entity structures |
| Attic integration | Launcher Provider | **Settled**: no CLI; harness requires `@attic/runtime` dist + self-spawn, Attic zero-change |
| MCP | Shared World API | Appropriate operation boundaries |

**All items in this section may be changed after source-code research and prototype validation.**

If research shows that an assumed feature does not exist in a particular version, the compatibility profile should document that limitation rather than forcing an artificial implementation.

---

## 12. Initial Milestone Definition

The first meaningful milestone should not be a complete multi-version toolkit.

Instead, it should demonstrate the entire architectural path on one carefully selected historical Minecraft version:

1. Identify the version.
2. Open an existing world.
3. Read its world and chunk data.
4. Display the world in a 3D viewport.
5. Start the original Minecraft runtime with the Java Agent.
6. Identify the running world and player.
7. Read selected runtime state.
8. Demonstrate a safe, controlled operation.
9. Confirm that the world remains valid after saving.

The UI should be validated through a separate static prototype before the full runtime integration.

This vertical slice will validate the most important architectural assumptions before the project expands to additional versions.

---

## 13. Change Policy

This plan is an initial development reference, not a commitment to implement every listed item.

The following circumstances may justify changing the plan:

- Source-code investigation reveals different runtime behavior.
- Historical world formats differ from initial assumptions.
- Java Agent instrumentation proves unreliable for a particular version.
- A prototype demonstrates that a proposed technology is unsuitable.
- Existing tools already provide a better implementation.
- Integration with Attic requires a different provider contract.
- Performance or memory constraints require a different design.
- UI workflow testing reveals a more suitable layout or component system.
- A feature is determined to be outside the project's actual scope.

Changes should be documented in the relevant architecture, research or UI notes.

**The actual implementation and verified compatibility take precedence over this initial plan.**