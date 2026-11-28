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

**Coverage scope (user reaffirmed):** the tool covers **original builds and high-fidelity reconstruction builds** (launcher-provided reconstructions of lost versions are first-class coverage targets, not a research-only side path). Registry entries must label each build's tri-state (original / high-fidelity / no jar). High-fidelity builds carry no behavioral guarantee of matching the original, so their hook fingerprint tables are pinned per-build independently; era/feature conclusions still rest on original builds plus index notes.

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

**Design verdicts (2026-11-24, headless discussion batch):**

- Unified route = **client-jar direct invocation for all three eras** (Indev: `createLevel`/`initWorldGenSeed` on Level `a.a.f`; Infdev: fake session + touch-to-generate exploiting the load-path coupling; Beta: World `fd` constructor + chunk provider). The dedicated-server-jar detour is **rejected**: Beta client jars contain no server classes (b1.7.3 full dump: 0 hits) because singleplayer generation already lives client-side; borrowing server jars would add a dual-artifact matrix, build-mismatch risk (e.g. b1.5_02 is server-only) and coverage gaps for zero gain.
- Headlessness evidence: 0327 Level — 0 back-refs to the Minecraft client class; its only 3 `net.minecraft.client` refs are a pure-IO stream helper on the load/save path; `<clinit>` is a float light table; Level constructs its own observer impl (`a.a.a.c`), so render callbacks no-op on an empty observer list headless. b1.7.3 World (`fd`, 8040 disasm lines) — 0 client refs, 0 LWJGL/AWT. Dependency direction is one-way (`client → World`, never `World → client`); the observer interface is the inversion seam.
- Per-version gate = **runtime smoke test** (invoke the gen entry in a headless JVM; NoClassDefFoundError/UnsatisfiedLinkError = fail), not javap closure crawling; javap stays as post-failure forensics only.
- Fidelity trap on the ledger: 2010-era terrain noise depends on `Math.sin` float behavior which **varies across JVM versions** — "same seed, same world" requires pinning the JVM.
- F1 compiler new facet = **generation-entry anchor columns** (`genSeedMethod`/`createLevel`/`chunkProviderClass`) added to the existing per-version fingerprint table, same artifact, same forensics pass. Classic-era AWT ancestry (`MinecraftApplet extends Applet`) is a separate verdict when that line opens.

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

**Design verdicts (2026-11-24, Three.js/MCEdit reference batch):**

- Era split: pre-Infdev worlds are **finite** (Indev fixed-size maps) — full load + full render is legitimate there; the streaming model below applies from Infdev onward.
- Two interaction modes, not one: **Live mode** — the game owns chunk streaming, so the view follows the *player*, not a free camera; distant editing = teleport the player (the player is the searchlight), unloaded chunks render grayed/read-only with click-to-focus = tp. **Offline mode** — camera-centric spiral streaming load (`iterateChunks`-style, view-distance threshold triggers swap), two independent pools (data chunk cap + VRAM/mesh cap, evict farthest first), edit session in a **CoW overlay** with explicit commit; only dirty chunks write back, level.dat last.
- Two-layer dirty separation (MCEdit-proven): data-dirty (drives save) vs render-invalid (drives mesh rebuild per layer), decoupled; box edits invalidate `expand(1)` neighbors; mesh rebuild runs on a per-frame budget (Web Worker + frame-time quota).
- Viewing authority splits from editing authority: global view = **per-chunk summary layer** (heightmap + dominant-color 2D overview, offline-generated from the storage readers — no new agent forensics), detail = voxel mesh near camera. Global-visible + local-detailed + distant-editable all hold without loading everything.
- Live feedback: optimistic local apply of op receipts (diff version-stamped on the envelope id), periodic region-checksum correction; game-originated changes (mob moves, block updates) need a **change-journal subscription surface** — the single biggest new T2 forensics item (blockUpdate hook → outbox append stream).
- Lighting: render from the real SkyLight/BlockLight nibbles on disk (G2 invariant), surpassing MCEdit's heightmap approximation.
- MCEdit reference ranking: ①load/dirty/save architecture ②2D overview interaction ③tool→op mapping table (box-select/brush/fill-replace/schematic clipboard ≈ our getblocks/fill/replace/clone faces). Not reusable: its format loaders (master has no Indev scattered-file support and asserts Anvil) — early-era loaders stay self-built; not reusable: display-list renderer, albow UI.

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

### 4.11 Cross-Version Structure Transfer (versioned schematic + remap)

**Design verdicts (2026-11-24, schematic drift batch).** MCEdit-style raw `Blocks[]/Data[]` schematics carry **no version field** and break on downward import (new→old) with silent unmatched blocks. Our schematics must be **versioned artifacts** (source fingerprint id embedded), and cross-version import goes through a remap layer, never raw writes.

Drift has three layers:
1. **ID-table drift** (adds/removals/merges) — hard case on file: b1.2 "dye + new 16-color wool" merged separate color IDs into `35+data`; imports across that line change block identity.
2. **Data-value semantics drift** (id survives, meaning changes: stair/torch orientation, log-axis encoding) — subtler; first version warns via a known-drift blacklist, per-property translation rules accrue incrementally.
3. **World-parameter drift** (height 64→128 clipping; entity/TileEntity type names absent in target era).

Mechanism: **name-join translation** — source table `id→canonical name` joined to target table `name→id+data`, both sides derived from the existing per-version fingerprint block tables (F1/P3 assets; zero new forensics). Unmappable entries follow a tri-state policy (default `error` listing all misses; optional `substitute` with declared fallback id; `drop`), and every import emits a **remap report** — never silent.

Ledger consequence: the 21 desc-drift items upgrade from hygiene debt to **dependency debt** (they are exactly the name-join key holes); a block-table canonical-name cleanup batch is now a prerequisite for this layer. Relation to channel ledger §6.1: that section ruled id+data **forward** portability of the wire format; this section covers **bidirectional** world-data transfer.

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

### 7.4 World-Occupancy Lock, version-tiered (2026-11-24 verdict)

`session.lock` is itself a version-gated surface: introduced **Alpha v1.0.13**; absent 0327→a1.0.12. Verified on our own live lines (natural experiment across the boundary): a1.0.3 jar 0 string hits; a1.2.0 2 hits (class `cu`); b1.7.3 2 hits (class `fm`). Format: 8-byte BE int64 open-timestamp; semantics = **last opener wins + monitor-and-abort** (unlike typical locks).

- **Tier L1 (≥ a1.0.13)**: the offline layer may *read* session.lock as an occupancy hint (fresh ≈ recently opened; cannot prove liveness — the value is static during a session). **Red line: the offline layer never writes session.lock** — under last-opener-wins that steals the lock from a running game and makes the game abort its own world (MCEdit's hostile lock-grab model, explicitly rejected).
- **Tier L0 (0327/Infdev/a1.0.3–a1.0.12)**: the game has no lock concept; file-level probing yields nothing. Occupancy verdict = agent channel liveness (authoritative), else launcher process table / user confirmation. Our live lines a1.0.3, 0624, 0327 all sit in L0.
- Optional L0 forward-compat courtesy: the agent may stamp a session.lock (spec format above) — inert to the contemporary game, a valid warning for the offline layer and for future-version opens.
- General rule: the agent holds the authoritative lock; the offline layer queries it before any write (equivalent of checkSessionLock), plus write-time backup discipline.

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
14. **Index-Authoritative Version Pinning:** Version identification and existence claims must go through the Archaeology Workspace indexes (`version-index.md` for client, `server-index.md` for server) as the sole authority — a version number not listed is treated as nonexistent; every jar must match its index row by SHA1 before being used as evidence; jar tri-state (original / high-fidelity reconstruction / no jar) determines evidential weight, and high-fidelity reconstructions must always be labeled as such when cited. Coverage scope = original + high-fidelity (user reaffirmed): high-fidelity builds are formal coverage targets, but the labeling and per-build fingerprint-pinning discipline is never relaxed. High-fidelity source: `nasko222/the-minecraft-recreation-project` (reconstruction of lost/internal client builds, client-only; jars stored per stage branch, `main` holds only `hashes.info` MD5 list, `testing` is the pre-release community area) — match its jars against the index row and label tri-state before use. Notes-column claims must be cited as "index note (maintainer summary)", not as "community record".
15. **Classic Is a Different Shape, Not a Different Name:** The G1 Classic line (`com.mojang.*` readable topology) differs from the 0327 path structurally, not just in obfuscation names — class paths, method presence (`setBlockWithNotify` vs bare equivalents), and semantics all diverge (no metadata plane, no TileEntity, mob construction plane unknown). The Version Registry must model three tiers — method absent / signature differs / semantics differ — not mere name substitution; WorldOps execution likely splits into per-family adapters (G1 vs G2/G3). Cross-family extrapolation (B3 clustering included) is forbidden at the G1/G2 boundary. Classic-line op evidence is scoped by the user to a minimal set: world manipulation (place/dig/fill/getblocks/getinfo) + single-mob spawn (G-key equivalent); the remaining ops map to `UNSUPPORTED(capability-absent)`. Estimate Classic work as a new plane, not a new column.
16. **Capability-Flip Windows Need a Mode Dimension:** Classic 0.24–0.30 is a special-judgment zone (survival introduced ~0.24, removed, then creative/survival as selectable modes later — exact per-build flip must be pinned by item-by-item scan, not memory). Within a flip window the same version has different capability planes per mode; `spawn` availability must therefore hang on the runtime-condition tier of the five-state model, not the version table alone. Also note the G-key path (client-local keypress spawn) and the agent path (direct `new Entity + spawn`) are NOT isomorphic — mode gating on one does not imply gating on the other (cf. 0327 peaceful-difficulty mob kill precedent).

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

---

## 14. Current Status and Decision Queue (updated 2026-11-26, headless window batch T1–T9)

This section supersedes the §11 open-questions table as the live status board. Detailed ledgers live in `docs/` (architecture/, research/, compatibility/); commit history carries per-batch verdicts.

### 14.1 Where the agent is

| Layer | Status |
|---|---|
| Command channel v1 | Live and hardened: file-queue protocol (protocolVersion 1, tmp/inbox/processed/outbox), tick-boundary execution, **48 ops** (place…grow/clear/getbiome + D8 quartet + D10 xp/effect + D11 hunger + D13 gamemode + D14 read-face + T2 sleep), save-policy table, targetGeneration, listops self-surface with shell echo diff-check; guard batch: strict-vs-lenient fingerprint loading, MiniJson boundary assertions, envelope fuzz, KNOWN_OPS↔LISTOPS consistency gate (commit cffbf95) |
| Version coverage | 17 fingerprint JSONs (2 runtime-schema + 15 anchors; b1.8 landed as draft then calibrated against real-jar javap — T8 389371a, b1.8.md B/C/D/E rows proven misattributed = F6 value-drift case #3); **9 versions wired** (0327/0618/0629/a1.1.2_01/a1.2.0/b1.2_02/b1.7.3/b1.8.1/**b1.9-pre2**), **6 live-verified** (…+b1.9-pre2: three live batches closed 2026-11-25/26 — four hooks / 30+12 commands / level.dat roundtrip / health triple-shadow fix sd.a·uo.a·fv.a / TNT no-segment + pig with-segment); first wave b1.5_01/b1.3_01 anchors on file, live promotion pending — weather boundary chain rebuilt (b1.3 absent → b1.5 introduced → beta-wide same-shape getter shuffle o()Z=isRaining/m()Z=isThundering confirmed across dy/ei/rl/sx by NBT ground-truth javap; former "b1.7.3 key-case break" claim retired = F6 drift case #4 → b1.9-pre2 flag-setter convergence), WorldInfo migration boundary corrected to b1.2_02..b1.3_01, effect/xp/hunger surfaces pinned |
| Industrialization | P1/P2/P3/F4/F1×2/F6-eval all closed: F1 compiler covers 113 facets, F5 matrix 627/627 green, F4 verdict = cliff structure (obfuscation dictionaries regenerate per version; 86 effective tables; anchors cannot cross versions → compiler is the only scalable forensics route); 13 cluster-representative anchor drafts pending promotion |
| Desktop shell | T1 command panel implemented and live-run passed (shorthand translator + dual-source polling + kill×save race UI); D5 alias table + D8 ops + getbiome gap-fill landed (40-op mirror, vitest 10/10); T2 (Editor registry + Three.js real render) and T3 (split/drag/palette/persistence) queued per workbench-model.md |
| Launcher integration | Attic sidecar `launch` path verified (game start channel); kill/stop semantics deliberately out of shell v1 |

### 14.2 Decision queue (next steps, ordering = user's call)

| # | Item | Nature |
|---|---|---|
| D1 | ~~F6 retirement execution batch~~ **DONE** (headless window T3 4c198a4: superseded annotations ×11 + 7 open-case writebacks + gap-routing ledger in pilot §14; b1.8 table→JSON transcription; hooks schema unification split out and executed as T6 082bf15 — 5 anchors backfilled, 13/17 now carry hooks) | Offline |
| D2 | Draft promotion first wave: 2-3 cluster representatives per live boot (live harness scripting via Attic sidecar can compress manual-in-loop); **b1.9-pre2 fully promoted** (runtime enablement 7ade240 + live closed); remaining first wave = b1.5_01/b1.3_01 live promotion pending arrival pack | Live |
| D3 | Version-expansion pilot version pick: 0330 (free anchor reuse via F4 implicit borrowing) vs Classic c0.28 cluster (4 versions / 1 anchor) — note: Beta spine first wave already landed offline (5858e7a) | Mixed |
| D4 | T2 kickoff: Editor registry research + chunk export agent batch (read-only hard constraints) + listops v2 capability-metadata batch (T2 prerequisite per workbench-model §6) | Frontend+agent |
| D5 | ~~Shell alias table small batch~~ **DONE** (alias table + clear-trap ruling landed in shorthand.ts; §6.1) | Offline |
| D6 | Follow-up ledger: difficulty BLOCKED new criterion, b1.2.zip missing-6-classes borrow-side list, 21 desc-drift items (upgraded to dependency debt per §4.11), a1.2.6 headless fixture vs real-jar note | Offline |
| D7 | Storage write-side batch — **mcr leg DONE** (T4 a9d5014: NbtWriter.writeRaw + RegionStore.writeChunk, b1.3 single-unit semantics, in-place reuse vs tail-append allocation, >4092B refuse-not-truncate, StorageTest 60/60); remaining legs: level.dat read/write + scattered-file writer + write-gate/backup discipline per §7.4 | Offline |
| D8 | ~~Gating+zero-forensics op batch~~ **DONE both halves** (headless half 61a14e4/cffbf95; live half closed via b1.9-pre2 three-batch live run; weather full semantics landed T1 9b44b67 = WorldInfo flag-setter dual-write, gated wiSetRainM/wiSetThunderM null→old float-only path byte-identical, zero ripple on legacy lines) | Mixed |
| D9 | T2/T3 pre-accounts: change-journal subscription surface (biggest new forensics), chunk-summary export (offline, storage-side, zero forensics), optimistic-update version stamping, block-table canonical-name cleanup (§4.11 prerequisite), headless smoke-test harness (§4.4) | Mixed |
| D10 | **New op pipeline from mining (§6.1 addendum 2026-11-25)**: hunger/effect/xp/gamemode landed in prior batches; **sleep/wake DONE ×2** (T2 163ee30 first pin; **h10 e76055c live-crash reversal**: raw flag write left bedChunkCoordinates=null → EntityPlayer.tick NPE crash W173 — setSleep now calls game API sleepInBedAt/wakeUpPlayer, slots sleepInBedM/wakeUpM/sleepOkF across all five capSleep anchors, fakes model the tick NPE regression line; **173 live re-run 14/14 PASS** incl. 191-tick survival + dawn-skip self-wake + level.dat raining/thundering=1; setchest iz.d private fix live-verified dmg=3 roundtrip; 181/pre2/1501 sleep letters javap-pinned, 181 live round pending); remaining: entitylist type-filter widening | Mixed |

Design verdicts 2026-11-24 recorded in §4.4 (headless unified client-jar route), §4.9 (Three.js dual-mode/two-tier render), §4.11 (versioned schematic + remap), §7.4 (session.lock L0/L1 tiers); evidence dumps under %TEMP%\mcaut\b5\{fx0327,lockprobe}.

Headless window 2026-11-26 (T1–T9, commits 9b44b67→f361539): weather full semantics = WorldInfo flag-setter dual-write gated (T1); sleep op first pin (T2); F6 retirement execution batch + b1.8 table→JSON + real-jar adjudication (T3/T8 — b1.8.md B/C/D/E rows = F6 drift case #3, rv.n/p + nu.a(II) now measured); RegionStore.writeChunk mcr writer leg (T4); hooks schema normalization ×5 anchors (T6); b1.9-pre2 TE container surface promotion — hc=IInventory a/e_/a, acb/fv fields pinned, getfurnace int-type gate added after fv.a boolean shadow proved name-collision escape (T7); README current-state refresh (T9). Final headless run: 33/33 GREEN, StorageTest 60/60, HookSnapshot byte-identical (123 lines).

Live batch 2026-11-26 late (173/W173 TE+sleep+weather smoke → crash → **h10 reversal batch e76055c**): FAIL2 crash = raw sleeping write left bedChunkCoordinates null, gs.am tick NPE (frame-for-frame match); FAIL1 = iz.d=itemDamage private (fake claimed public, masking the break) + no (II)V ctor. Fix: setSleep via sleepInBedAt/wakeUpPlayer API (all five capSleep anchors re-slotted), resolveTe getDeclaredField+setAccessible+atomic-cache+ctor2-null, h9 worldSleepFlagM=m mispin corrected to fd.y()V, bed id 26 pinned, fakes gained tick-NPE regression models. Live re-run 14/14 PASS (gates d/c, 191-tick survival, dawn-skip self-wake, dmg=3 roundtrip, level.dat flags=1). New datum: ~~rainTime/thunderTime are TAG_Long in 173 level.dat~~ **W-1/W-3 翻案作废：tagType=3 即 TAG_Int（旧 check 注释误标 Int=4 诱发连锁误读）——类型化 walker 盘上真值 + 四版 javap 写侧终判全 TAG_Int，无宽度断代（b1.7.3.md §12）。**

### 14.3 Next headless window plan (3.5h, unattended, no game launch)

Priority order; all offline (javap/fakes/docs/storage), stop-and-log on any ambiguity.

1. **W-1 (30m) 181 live-round prep**: write `%TEMP%\mcaut\smoke_h10_181.ps1` (181 letters: sleep on=sz.d via same op, bed 26, rv.k daytime, provider acz gate not exercisable in overworld — keep d/c gates only; setchest ul.d private dmg roundtrip; weather+level.dat leg incl. TAG_Long-aware checker update in check_wx173181.ps1); dry-parse JSON envelopes; no launch. → **DONE a42dc10**（偏差：TAG_Long 本身即 tag 号误读，checker 改 3=Int/4=Long 宽度自适应；181 门形 h10 反汇编复核与 173 字节同形；dry-parse 15/15 未触队列未启动）。
2. **W-2 (60–90m) root1501 fake tree + tests**: b1.5_01 currently JSON-only (no FakeOps/A2Table coverage). Build root1501 fakes from %TEMP%\mcaut\b5 forensics + h10 sleep pins (gh=EntityPlayer, et=World, ii=ItemStack d-private, co=EnumStatus a-e, bed field **a:Lbk;** trap letter, isSleeping=O()Z); FakeOps1501Test (sleep four gates + tick NPE model + setchest) + A2Table1501Test (builtin absent → JSON↔fake slot parity); run.ps1 must stay green for all existing items. → **DONE 80a68f3**（root1501 假件 20 件；FakeOps1501 46 checks：门序实为 e→b→c→d 且 **1501 无头部 isRemote 旁路**（gh_disasm @0 直入 O()Z，与 173/181 断代）、A2Table1501 314 checks（v1 档运行时不可载→测试侧 injectTable 注入对账）；b1.5_01.json +1 行 capWeather 门旗；开放欠账：WorldOps resolve 缺 `(ZLwr;)V` 双参分支，零参分支误绑 `et.a()Lwl;` WCM 同形陷阱——转正批必修，已记 b1.5_01.md §9）。
3. **W-3 (30m) TAG_Long cross-audit**: javap WorldInfo NBT write-side (173 ei / 181 rl / pre2 sx / 1501) for rainTime/thunderTime put-method width (setInt vs setLong) → writeback verscan where doc claims Int; if widths differ across beta spine, table note per version. → **DONE acef6d6**（终审=四版全 TAG_Int：m:I/o:I 字段 + setInt `a(String,I)V` + getInt `e(String)I`，NBT 类字母 my/nu/xb/za 断代方法字母同形；无断代，回写 b1.7.3.md §12 + b1.8.1.md）。
4. **W-4 (45m) verscan doc gap-fill**: b1.9-pre2.md + b1.5_01.md + b1.8.md formal docs (evidence currently scattered in fingerprint notes; format per b1.7.3.md sections; mark draft rows). → **DONE e1994fd**（b1.5_01.md 61 行 + b1.9-pre2.md 65 行新建；b1.8.md 复核=已是正式旁证文档不重复产出，W-3 宽度证据按 per-build 钉表纪律**不**搬入（审计跑的是 b1.8.1 jar）；三处证据矛盾 ⚠️ 在档不静默裁决）。
5. **W-5 (stretch) D7 level.dat leg**: read-side key-extraction generalization (typed NBT walker replacing byte-scan in check scripts) + write-side gate/backup discipline design note per §7.4 (no live writes this window). → **DONE ce6fc36**（docs/architecture/leveldat-write-gate.md 设计稿：G1-G5 写门 + B1-B4 备份纪律 + LevelStore 接入位 + 3 待钉；nbt_walk.ps1 泛化=pos 精确全遍历+KeyPath 类型查询，三世界 END pos=size 自证）。

Window 2026-11-27 executed W-1→W-5 (commits a42dc10 → 80a68f3 → acef6d6 → e1994fd → ce6fc36): final headless run **35/35 GREEN**（+A2Table1501 314 / FakeOps1501 46），StorageTest 60/60，HookSnapshot byte-identical (123 lines)；队列零触碰、游戏零启动、14 件 EOL 噪音未混入（终核 `git diff --ignore-all-space` 仅真实改动）。

#### 14.3-续 窗口 2026-11-27 续（同窗追加，W-6..W-9，全离线）

6. **W-6 agent 静态欠账**：WorldOps.resolve saveWorld 补 `(ZL…;)V` 双参分支（Class.forName 内嵌类名）+ saveWorld() 调用面 `(false,null)` 配方 + FakeOps1501Test save 腿（46→48 检）。→ **DONE dd39151**（run.ps1 35/35 GREEN 复验；b1.5_01.md §9 欠账同步销账）。
7. **W-7 三矛盾离线复核**：① pre2 天气键=nbt_walk W19P2 原盘实扫**全小写**（note③ 大写说翻案）；② heartsLife=**lh.Z**（javap lh 有 `public int Z`、xx 无；tiny L5057 落 lh 块；note"xx.Z"翻案）；③ 1501 jz.B ↔ 173 kv.B=**同字母同义 showDebugInfo**（双版 tiny 对榫；note"异名/非 173 showFPS"措辞翻案）。→ **DONE 45e4a71**（docs-only，b1.9-pre2.md×4 + b1.5_01.md×1 回写；JSON 零触碰）。
8. **W-8 1501 存储链补钉**：pk/qi/hk/kh/fc/gw 六件 + 外围（vk/mp/sm/tu/vp/sk/az/ao）tiny 直钉 + javap -c 实扫（两 int[1024] 表、`e(II)=d[z*32+x]`、4096 扇区串面、"r."/"region"/".mcr" 宿主）——**判决 b1.3→b1.5 存储无断代，region adapter 复用 173 型成立**；顺改 §1"fm→vk"错位（正=wt→vk/fm→fc）。→ **DONE e1c4202**（docs-only，b1.5_01.md §6 全钉+§10 判据追加）。
9. **W-9 扩版自主判决**：b1.4_01/b1.6 双 jar 三态过闸（MOJANG_C 签名=原始，SHA1 6f157f26/ecc0288d 双位同件）；**不建全指纹表**（无活体证据+全表需 1501 批等深 javap，仓促=F6 漂移温床），产**边界锚批** b1.4_01-b1.6-boundary.md：睡眠面下界=b1.4 钉死（四门链+provider 门全码在，字母表 fm/eb/ay/cb/sy.c 双验）；**"b1.6=下界/disablePlayersleep 门下界"翻案**（b1.4_01 已有 WorldProviderHell=sf 全套+门2 消费；b1.6 真边界=isNether 更名+MaterialPortal=oq）；saveWorld 双参世代下界前移 b1.4_01；b1.7.3.md §10 床/火弹两行改口。→ **DONE 4896892**（docs-only，运行时零消费）。

续窗终态：run.ps1 **35/35 GREEN**、FakeOps1501 48、队列零触碰、游戏零启动、b1.9-pre2.json/b1.2_02.json/inf-20100618.json 等 14 件 EOL 噪音未混入；1501 活体批前置欠账（resolve 双参+存储链静态）已全清，verscan 三处 ⚠️ 矛盾全部终判无遗留待钉悬账（除活体面）。

Exit criteria: run.ps1 all-green (new items included), one commit per W-item (author discipline), plan.md 14.3 ticked with commit hashes; anything blocked → leave annotated note, do not guess.

Disciplines unchanged: live runs require per-batch user approval; evidence chains archived per run; protocol evolution goes through channel ledger only ("panel follows protocol"); no implicit world generation on read paths.

### 14.4 无头窗 2026-11-27 V 批（5h 无人值守，V-1..V-5+收尾，全离线）

戒律：零启动游戏、零触队列、卡点留注不猜、每件独立提交、**提交信息中文详账式**（用户纠偏后 project_memory.md 在案；h10 批 11 笔已推送英文不回改）。

1. **V-1 b1.4_01 全草稿指纹表**：javap -p 全量 dump（9903 行/619 类块）+client.tiny 三级直钉→`b1.4_01.json`（11967B 草稿锚，>4096B 运行时不消费；ops 77 键/hooks 4/caps 42=37 OK+5 UNSUPPORTED）。核心钉值：World=eb/Entity=pb/EntityLiving=jm/EntityPlayer=fm/InventoryPlayer=hh/ItemStack=hi/Item=fg/EntityList=hj/GuiIngame=qv/WorldInfo=dj；getWorldInfoM=**w**（1501=x 同形陷阱）；isEntityAlive=**pb.U()Z**（边界批 N()Z 误判终判）；saveWorld=`a(ZLtw;)V` null 守卫 @14/@30；四大边界判决（天气/群系下界、Giant 未到世、worldTime 五版同形）。GuardTest 17→18 JSON 闸。→ **DONE fce2620**。
2. **V-2 root1401 假件树+双测试**：假件 18 件（不建 tq 群系假件=判决②树侧自证；dj 无 boolean/eb 无 float=判决①负锚反射复核；fm 睡眠三槽全 private a/c/b）+ **A2Table1401Test 335 checks**（ops 全表+断代重点+hooks 四节+caps 双对榫+访问位 15 节）+ **FakeOps1401Test 45 checks**（sleep 四门 e→b→c→d+tick NPE+wake null-safe+setchest c_ 回环+oy 熔炉门+UNSUPPORTED 五件分道：四 require 槽名+weather capWeather Boolean 门）；新取证 fm_c.txt（四门/OK 尾/wake/tick/isInBed 全反汇编实锤，@111 lo.w_ super 回调 1401 实形）。run.ps1 35→**37 全绿**。→ **DONE 35de5d6**。
3. **V-3 b1.4_01.md 调研批文档**：十节格式（四大边界判决成文+saveWorld 双参断代带下界=b1.4_01+isEntityAlive 勘误件+HUD 门 A 非 B 翻面账+存储链 W-8 下界前移 1401+活体判据预登记 §10+转正批欠账清单）；边界批 §2 isEntityAlive 误行改口。→ **DONE ed4ef2c**。
4. **V-4 1501 活体脚本**：`%TEMP%\mcaut\smoke_h10_1501.ps1`（16 腿含 **getbiome 首测腿**+weather 双写+am() NPE 观测窗+save 双参配方；头注=1501 字母判决全引防借表）；**仅 DryRun**=DRYPARSE 16/16 PASS（零触队列、零启动）。b1.5_01.md §9「1501 活体批未建」欠账销记（剩余欠账=活体执行本身，窗口外另约）。→ **DONE badc154**。
5. **V-5 b1.6 真 jar 复验（富余）**：javap -p 18 类+gq_c.txt 全量 -c（%TEMP%\mcaut\exp\b16\，SHA1=ecc0288d 对榫索引）→ 边界批 **§3.1**；**头号警示=sister 仓 b1.6 tiny 字段面 ≠ 真 jar**（Entity 系错位一位：worldObj aI→aH/pos aM..→aL..；gq 睡眠三槽 tiny u/a/v→真 jar u:Z/b:I/a:Lbp;——转正批以真 jar 为准）；+1/版规律复验严格成立；RUN_TICK=`k()V` 断代下界提前至 b1.6；主类六字段全洗牌；ChunkCoordinates=bp 补钉；iw 仅 (III)V/d_ 同 1501/群系链 jz.n/xm.a(II) 在位复验。→ **DONE f01be2c**。
6. **V-6 b1.6 全草稿指纹表**（用户「继续下一步」自主判定补批）：javap -p 全量 9465 行/666 类块+定向 -c 四件（gq/fb/mc/uk）→`b1.6.json`（13307B 草稿锚，>4096B 运行时不消费；ops 77 键/hooks 4/caps 42=**39 OK+3 UNSUPPORTED**——与 1501 能力面同世代）。核心钉值：World=fb/Entity=si/EntityLiving=lo/EntityPlayer=gq/InventoryPlayer=iu/ItemStack=iw/WorldInfo=eg；**字段字母全按真 jar**（tiny 错位警示入表注）；天气判决正半轴（rainStrF=**j**/thunderStrF=**l** 经 fb.E()V 反汇编语义钉死+wiSetRainM=b/wiSetThunderM=a）；biome a/a/n（jz.n public）；giant=**np 首到世**；iframes=null；RUN_TICK=k 入钩表；主类六字段全洗牌；saveLevel=D()V private 访问位翻面注。GuardTest 18→**19 JSON 闸**。→ **DONE d16bdaf**。
7. **V-7 root16 假件树+双测试**：假件 20 件（gq 睡眠三槽按真 jar 钉 protected u/private b/private a:Lbp;，四门 e→b→c→d+OK 尾+wake null-safe+tick am() NPE 全复刻；fb 天气四 float protected i/j/k/l+eg 旗标 setter；不建前代件）+ **A2Table16Test 327 checks**（身份三锚 ecc0288d+ops 全表+tiny 错位警示注+hooks 四节+caps 39/3 双对榫+null 槽 9+访问位复核 14 节）+ **FakeOps16Test 50 checks**（**weather 双写 OK 腿**+**getbiome 首测腿**+sleep 四门全腿+NPE 复刻+setchest d_ 回环+UNSUPPORTED 响亮三件）；run.ps1 37→**39 全绿**（两新项首跑即过，HookSnapshot byte-identical）。→ **DONE b7913f2**。
8. **V-8 b1.6.md 调研批文档**：十节格式（头号警示成文+七核心判决+撞车位实录 8 组+存储链三段全洗牌/方法形同形延续+活体判据预登记 §10+转正批欠账）；边界批 §4「不建全表」判决**改口注记**落盘（草稿锚纪律化解 F6 顾虑，转正路径不变）。→ **DONE（本笔）**。

收尾终态：run.ps1 **37/37 GREEN**（A2Table1401 335/FakeOps1401 45 新增，GuardTest 102/StorageTest 60 无回归）；git status 残留恰 **14 件** EOL 噪音未混入（b1.9-pre2.json 禁改件未触）；队列零触碰、游戏零启动；V 批五笔提交全中文详账（fce2620→35de5d6→ed4ef2c→badc154→f01be2c，未 push）。

V-6..V-8 续账终态（用户「继续下一步，还没到截止时间」驱动的第二波）：run.ps1 **39/39 GREEN**（A2Table16Test 327/FakeOps16Test 50 新增首跑即过，GuardTest checks=104/StorageTest 60 无回归）；git status 残留恰 **14 件** EOL 噪音（禁改件未触）；队列零触碰、游戏零启动；续账三笔全中文详账（d16bdaf→b7913f2→V-8 本笔，未 push）。窗口内 b1.6 链闭环=V-5 复验→V-6 表化→V-7 测试化→V-8 文档化+判决改口；窗口外欠账不变（1501/1401/b1.6 活体执行、两份 JSON 精简转正、b1.3/b1.2 系回扩）。

### 14.5 活体批（2026-11-28，用户「可以活体测试了」解禁）
- **L-1/L-2 1501 转正（8a084bb）**：活体前置=运行时消费面（草稿锚不消费）。VersionRegistry.build1501() 内建表（76 槽+mobs 9+hudXyzPaint=FALSE 边界线，仿 pre2 体例）+HookTargets.TARGETS_1501 钩组（SAVE_WORLD=et 双参实体 ENTRY/RUN_TICK=**j()V**/LOAD_WORLD 3 参 EXIT/HUD tl EXIT）+A2Table1501Test 转正对榫段（314→432 checks：内建表 vs 锚库逐槽+TARGETS 六元组）+InjectSmoke1501Test（37 checks：织入 et/Minecraft/tl 实形+跨组隔离双验+双参实体钩 ILOAD 1 观察位）+root1501 补 tl/Minecraft 桩两件；run.ps1 39→**40 全绿**。
- **L-3/L-4 启动链**：build.ps1 重建 jar（19 资源件）；sidecar 首轮破碎=legacyfix javaagent 参数 gameDir 空格未引号（主类位空）→手工直组命令行成功；`.attic\versions\b1.5_01.jar` 首启自动装件（SHA1=e2a692e5 对榫原件）；**VERSION SHA1 自动对榫命中（未钉强制保险丝=戒律 14 活体半区证据）**；computer-use 建世界 w1501（press_key 逐字符入名，type_text 后台键入对 raw-input 游戏无效）。
- **L-5 活体 16/16 全中**：钩四项 HOOKED（j()V 首证/HUD xyz-injection-exempt）；SAVE_WORLD 双形态（#1 indirect=true 游戏直调、#10 =false ops 配方）；四门 d-far=d/h-dayon=c 恰拒、e-on OK、**f-alive 190 tick 存活（am() NPE 红线未触）**；setchest→getchest `slots=27 0=2x5 d3` 回环；**weather 双写 OK+level.dat 四键终核（raining/thundering Byte=1+rainTime/thunderTime Int 在位——判决②活体闭环）**；**getbiome=Seasonal Forest 首证**；RandomSeed 与 getinfo 逐位同。**副作用实录=game-dir 重定向未生效，世界落 `C:\Users\GAME\saves\w1501`**。
- **产线小修（随本批提交）**：WorldOps setSleep OK 消息「bed=v set」硬编码 173 字母→「bed coords set」通用措辞（零行为变更）。
- **L-7 补腿批（同日，游戏续跑期，判决账入 b1.5_01.md §11）**：spawn pig/giant OK+entitylist 回环（`Giant@h=200` 首报=mx 活体实锤+ik.b() 查名中；N=136→138 增量对榫）；**invulnerable UNSUPPORTED `slot iframesF not pinned for b1.5_01`**（判决④实报）；region r.0.0.mcr 头细验=**McRegion legacy 全解**（无 version 字段+偏移/时间戳双表+comp=2 zlib——与 173 §8 静态判决同形）。陷阱实录：PS5 `$args` 参数名撞自动变量（信封假 REJECTED）、mcr FileShare 共享读。
- **L-6.5 P1-P5 销案（实体证据链，本窗 CLI 问题报告后续）**：argv 探针（sidecar spawn 18 参数全对，javaagent 带引号单参）→RAW 命令行回放真 java（窗口起）→sidecar 原命令复测（b1.4_01/b1.5_01 双版本成功，agent 注入+hooks 落日志）→WMI 抓 pid 命令行逐字节对榫；legacyfix 全 jar 扫描无 re-exec 代码。**P1 原判（空主类/组装不一致）作废**；P2-P5 经用户归因核定=**网络波动+沙箱干扰瞬态**（14:49 现场为首次 provisioning 中间态，补丁面残缺指纹在档），全部销案；P6（versions 子命令）保留为功能缺口。方法论入账：活体失败先重放再定罪，前台 harness 进程树/管道不可信。
- **L-8 1401 活体（同日，判决账入 b1.4_01.md §11）**：**启动链切 sidecar**（`launch --version b1.4_01 --java jdk1.8.0_202 --jvm-arg asm --jvm-arg agent` 后台宿主，P1 销案后链路首用）；SHA1 自动对榫 6f157f26=build1401 命中；**钩四项 HOOKED 全中（`j()V` 活体首证）**；SAVE_WORLD 双形态（#1 indirect=true `dir=?` 构造断代降级自证+#2..#8 autosave false）；computer-use 建世界 w1401；**15 腿直读全中**：sleep OK（1401 睡眠面活体首跑，玩家拉床 x=4.5 实锤）、getinfo difficulty=1（getWorldInfoM=w）、setchest/getchest `0=2x5 d3`（jp.c_+hi.d）、sethealth `17->10`（healthF=V）、**UNSUPPORTED 响亮四件全中**（weather capWeather 门控/getbiome biomeWcmM/invulnerable iframesF/spawn giant `bad-kind` REJECTED 道）；harness 实录=smoke 收集循环沙箱文件可见性延迟 0/15 假 miss（直读 15/15 为准，与 P2-P5 归因同型）。
- **L-9 b1.6 转正+活体（同日收口，判决账入 b1.6.md §11）**：转正=build16() 76 槽+mobs 9+TARGETS_16 四钩（**k()V 翻面下界首钉**）+InjectSmoke16Test 37+run.ps1 41→42 全绿。**healthF 勘误案 Y→X（活体翻案第二例，1401 U 先例）**：sethealth 写 Y 心形不动+初值 0 未死→真 jar javap 三重实锤（NBT "Health" 双向绑 X+构造保底 10+clamp 20；Y=hurt 快照备份）→+1/版规律 V/W/X/Y 复位→勘误四连（JSON+表+测试+假件）→活体复验 20→10→**截图心形减半**+Giant[h=188] 掉血自证。活体 30 件回包：k()V HOOKED 首证、四门谱全中（门1=N()Z）、weather 双写+level.dat 四键在位（旗标 tick 归零自证；w16 落 .attic saves=game-dir 重定向本代生效非普适）、getbiome=Shrubland（n 字母首活体）、giant np 首到世（null→mx→np 三段链闭环）、invulnerable UNSUPPORTED、setchest d_ 链、saveWorld(false)。harness 两课：-File 子 powershell 读 %APPDATA%=沙箱可见性盲区（判决一律 inline 直读）；PS `-X -42` 负数吞参假拒（内联字面量重发）。
- **L-8.5 1401 sleep 三门谱补腿（同日，b1.4_01.md §12）**：三启 sidecar 误进 w16（列表新→旧排序实录）就地补腿八腿全中——d-far=d/e-on=OK（三槽 a/c/b private 形首跑）/e-dup=e/f-alive 存活/g-day/h-dayon=c/i-off；白天抢序废谱变废为宝=**四门序 e→b→c→d 活体钉死**；§11 遗留腿全销。
- **L-10 Beta 采样收尾批（2026-11-28，lite 配方首用）**：181+pre2 两代活体-lite（复用 w16 不建新世界+10-12 腿，成本≈旧法 1/3）——181 四钩 HOOKED（rv/ny/sz/abj+k()V）/sleep d 门高差实录→同高 OK/e 门/off；pre2 四钩（ti/pd/uo/adm）+**门1 时序发现**（睡眠旗标被游戏 tick 自动清除，dup 判决窗口=同 tick 内——背靠背复验实锤非表错）；两代 weather/chest d_/save 单参 wrapper/tick 存活全绿；**setmeta UNSUPPORTED=observersF 未钉 181**→完整床配方受阻（用户二次指正 c-bed2=双 foot，bit3 头脚位——纠正账入 b1.5_01.md §11 通用账）。文档落点=b1.8.1.md §12+b1.9-pre2.md §11。
- **L-11 药水双版活体批（2026-11-28，pre2+pre3 首启）**：`potiontest` 三段（predicted=dd_d 引擎/applied=程序化喝药不入包/readback=activePotionsMap）双版判决——pre2 位型组全中（2048=heal 主料、18496=slowness+heal II **双效实测修正姊妹仓手验 heal+fireRes**、32767=饥饿+反胃**用户肉眼人证**、reqTableSize=19）；pre3 跨版翻案四对全中（糖 1→heal、2048→废、18496→刷锅水、reqTableSize=8）+**跨版效果存续实锤**（pre2 存盘效果经 pre3 加载 readback 残留 {17,18,4,9}=PotionEffect NBT 链兼容）；pre3 首启 SHA1 命中 build19pre3 四钩全 HOOKED 含 k()V 活体首证。**产线缺陷二笔入账**：①place 缺未注册 ID 守卫（pre2 锅腿垃圾 ID 118 写脏 chunk→游戏线程崩溃，铁证=同 tick pt-8192 永停 ACCEPTED；w16 磁盘大概率干净待消毒腿）②pre3 save 腿 NPE=最小转正 require 缺口（应响亮 UNSUPPORTED）。GUI harness 教训：Select World 列表不响应自动化输入（LWGL 真实焦点轮询），进世界=人工协作位。后续批候选：place 守卫修复+save require 缺口+give 373 入包腿。文档落点=b1.9-pre2.md §13。
- 未销：**无**——Beta 全代活体覆盖完成（1.4/1.5/1.6/1.7.3/1.8.1/1.9-pre2 六代+173 el 批）+pre3 药水面活体；Infdev/Alpha 家族锚 lite 待跑；可选腿=b1.6 存储链 javap 抽样复验、qr TNT 面复验、**产线缺陷修复批（place 守卫+save require）**。
- **N-1 `locate` op 立项（2026-11-28 剖树调研批，设计已钉死待实现）**：
  - 签名：`locate(kind=structure|biome, target=<village|mineshaft|stronghold|dungeon|biome名>, radius?=100)` → `{nearest:{x,z,dist}, candidates:[前N]}`；
  - 数据源（b1.8.1 混淆对榫表 18 项在位，client.tiny↔jar javap 全验）：structure=「`zo.e` HashMap 缓存快路 → 环展开反射 `zo.a(cx,cz)` canSpawn 补算（种子确定性=与游戏真生成 100% 一致，村庄 spacing32/salt10387312、矿井 nextInt(80) 判据）→ 要塞 `sd.g[3]` 直读」三合一，**最近语义与现代 26.3 `findNearestMapStructure` 同心环算法逐一对齐**；biome=环采样 `nu.func_35562_a`（GenLayer 纯种子函数，比现代更自由）+ `vh`/BiomeGenBase 静态实例 name 反查（getbiome 名字典复用）；
  - 门控（**下确界实证批 2026-11-28，姊妹仓树面实锤**）：
    · `locate biome` ≥ **a1.2.0**——`WorldChunkManager.java`+`getBiomeFromLookup` a1.2.0 首现（a1.1.2 零命中）；**与 getbiome 同数据源同下确界自洽（用户指正）→门控复用 biome 槽面零新门**；
    · `locate structure(dungeon)` ≥ **inf-20100625-1917**——WorldGenDungeons 阶梯 624 no/625-1 no/**625-2 YES**（同日晚 build，jar inf-20100625-1917 在位；该版未转正=未来转正批顺带补 Infdev 锚）；已钉代中 0629/1121 起可用，0327/0618 在界下；
    · `locate structure(村庄/矿井/要塞)` ≥ **b1.8**（MapGen 三族；625-1917~b1.7.3 区间=仅地牢，与「b1.8 前只有地牢」修正账自洽）；
  - **Infdev~Beta 覆盖评估（同日批）**：产线 13 代≈全谱 51 版 26%（Infdev 4/~20、Alpha 2/~14、Beta 8/~17）——采样代表策略产物；活体=Beta 六代+Infdev 三件+pre3 药水面，**Alpha 家族锚零**；全覆盖=37 版 headless 三件套批产排期候选；
  - 关键判决（剖树实锤）：b1.8.1 结构**生成即弃零落盘**（五重证据，StructureStart 无 NBT 方法/区块键全集无结构项/无 VillageCollection/jar 常量池零命中）；**26.3 现代格式=区块 NBT `structures→starts→<资源ID>`+References LongArray（level.dat 不参与，"(cx,cz)" 键式属 1.16-1.18 旧格式）**；b1.8.1 无神庙（Temple 类零命中，沙岩=自然生成不可作指纹）；
  - 新表槽 ≈8（adp/zo/lu/mp/sd/nu/vh/cq）；W181 真盘指纹旁证在位（村庄 door4+planks148@chunk(-21,8)、矿井三例、要塞超探索圈符合预期）；
  - headless 备选=.mcr 指纹簇扫描（spawner/door/rail 判据，Python stdlib 原型 mcr_probe.py 已验）——视野=已探索区。
- **L-11.5 守卫复验活体批（2026-11-28，pre2 w16，c6c5ccf 销案）**：place 118→**REJECTED unregistered-block**（写前拦截，后续 7 腿连跑游戏存活——二次崩溃根因案全链闭环）；place 999→out of range（校验层分工正常）；**give 373 1 18496→OK saved=true**（物品入包腿闭环）；sanitize place 0→returned false=**w16 本无脏 chunk 落盘自证**；potiontest 18496 双效回归复现（守卫零连坐）；save OK。