# MCWorldAgent

**Minecraft Classic World Agent** — a standalone desktop application for exploring, analyzing, editing and controlling historical Minecraft worlds.

The project targets Minecraft versions released before the official 1.0: **Classic, Indev, Infdev, Alpha and Beta**. Rather than reimplementing a Minecraft engine, MCWorldAgent builds a unified toolkit around the original Minecraft runtime and world data.

> **Status:** Early planning / repository initialization. See [`plan.md`](plan.md) for the development direction and roadmap. This is not a finalized specification; actual source-code behavior and verified compatibility take precedence over initial planning assumptions.

## Operating Modes

- **Standalone Mode** — operates directly on world files, or invokes the original Minecraft JAR for offline operations (headless generation).
- **Agent Mode** — connects to a running Minecraft instance through a Java Agent (`premain()` instrumentation), enabling runtime inspection and control.

Both modes share a common **World Data Model** and **Version Adapter** architecture, with an explicit per-version compatibility registry: a version is marked supported only when its operations have been verified against original source/bytecode.

## Architecture Overview

| Layer | Proposed Technology | Responsibility |
|---|---|---|
| Desktop Application | Vue 3 + TypeScript + Tauri (Electron optional) | UI, workspaces, operation management |
| Java Core | Java | Runtime bootstrap, JAR/classpath management, agent lifecycle, storage adapters |
| World Data Model | — | Unified, version-aware representation of worlds, chunks, blocks, entities |
| Rendering | Three.js + WebGL2 | 3D world visualization and editing viewport |
| External Integration | Launcher Provider API, Agent Bridge, World API, MCP Server | Stable interfaces for launchers, AI agents and external tools |

## Engineering Principles (highlights)

1. **Source-Code First** — historical behavior must be verified through source investigation, bytecode inspection or reproducible tests.
2. **Version-Aware Design** — no assumption that modern Minecraft architecture applies to historical versions.
3. **Strict Instrumentation** — prefer reporting an unsupported hook over applying an uncertain transformation.
4. **Independent Operation** — the toolkit does not depend on any particular launcher.
5. **Safe Data Ownership** — never concurrently write a world that a running Minecraft instance owns.

## Repository Structure

```text
MCWorldAgent/
├── apps/            # Desktop application(s)
│   └── desktop/
├── packages/        # Shared frontend packages
│   ├── ui/
│   ├── world-model/
│   ├── core-spec/
│   └── api-client/
├── java/            # Java Core
│   ├── core/
│   ├── agent/       # Java Agent & runtime bridge
│   ├── adapters/    # Version adapters
│   ├── storage/     # World storage engines
│   └── generator/   # Standalone (headless) generator
├── integrations/    # External integration providers
│   ├── attic/
│   └── mcp/
├── docs/
│   ├── architecture/
│   ├── compatibility/
│   ├── research/    # Source-code research & integration memos
│   └── ui/
└── plan.md          # Initial development plan
```

This is an initial organizational proposal; the final structure follows actual module dependencies and build-system requirements.

## Documentation

- [`plan.md`](plan.md) — project goals, architecture, core modules, development phases and roadmap.
- [`docs/research/research-integration.md`](docs/research/research-integration.md) — mapping of verified source-code research findings onto the plan (module cross-references, mandatory registry facts, vertical-slice version candidates).

## License

MCWorldAgent is licensed under the **GNU General Public License v3.0** — see [LICENSE](LICENSE).

## Trademark Notice

Minecraft is a trademark of Mojang Synergies AB. This project is not affiliated with, endorsed by, or connected to Mojang or Microsoft. It does not distribute Minecraft game files; users must supply their own legally acquired versions.
