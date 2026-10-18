# Project Context: Apeira

## 1. Executive Summary & Vision
**Apeira** is a modular, high-performance Minecraft Java mod built on **NeoForge 26.3** (Java 21). 

The goal of Apeira is to bring the rich, custom, interactive survival server experience of *Origin Realms* into a **native, singleplayer/multiplayer modded environment**.

### Why Apeira Exists:
* **Eliminate Server-Side Hackiness & Entity Lag:** Traditional custom servers rely heavily on display entities, invisible armor stands, complex resource pack hacks, and plugin overhead. This creates massive server TPS drop, entity tick lag, and rendering stutters.
* **Native Modded Performance:** Apeira replaces command blocks, plugins, and armor-stand tricks with clean, data-driven NeoForge blocks, VoxelShapes, custom client-side block entity renderers, and payload-driven UI networks.
* **Modular Architecture:** Rather than building a single monolithic mod file, Apeira is split into **three foundational library subprojects** (`apeira-core`, `apeira-decor`, `apeira-story`) and **one main content module** (`apeira-main`).

---

## 2. Technical Stack & Foundation
* **Target Engine:** Minecraft Java Edition / **NeoForge 26.3**
* **Language / JDK:** Java 21
* **Build System:** Gradle Multi-Project using the official `net.neoforged.moddev` plugin
* **Asset Pipeline:** Data Generators (Datagens) + Python scripts for automated model/texture generation
* **Rendering & Animation:** Fast TileRenderers, standard VoxelShapes, custom block entities, GeckoLib for complex mobs/bosses

---

## 3. Modular Architecture Breakdown

Apeira uses a Gradle multi-project layout. Every module serves a specific, isolated purpose:

                         ┌─────────────────────────────────┐
                         │           apeira-main           │
                         │    (Primary Content Module)     │
                         └────────────────┬────────────────┘
                                          │
        ┌─────────────────────────────────┼─────────────────────────────────┐
        ▼                                 ▼                                 ▼
┌──────────────┐                  ┌──────────────┐                  ┌──────────────┐
│  apeira-core │                  │ apeira-decor │                  │ apeira-story │
│  (Framework) │                  │ (Render/UI)  │                  │ (Quest/NPC)  │
└──────────────┘                  └──────────────┘                  └──────────────┘

apeira-core (Foundation & Registry Library):

DeferredRegister wrappers for Items, Blocks, Data Components, and Fluids.

Networking system (CustomPacketPayload and PayloadRegistrar).

Abstract Datagen bases (BlockStates, ItemModels, Language, Data Maps).

apeira-decor (Interaction & Seating Engine):

Direction-aware VoxelShapes for furniture.

Temporary mount entity logic for furniture seating.

Dynamic Painting Canvas system and dye color palette UI containers.

Player cosmetic model layers (Curios integration / Custom renderers).

apeira-story (Progression & World Systems):

Data-driven Quest system (JSON Codecs, objective tracking, HUD overlays).

NPC dialogue state machines and custom trading UI.

Boss AI state engine and arena trigger blocks.

apeira-main (The Content Mod):

Implements actual items, blocks, custom biomes, dimensions, recipes, foods, furniture, crates, cosmetics, mobs, and bosses using the underlying libraries.

4. Feature Roadmap & Execution Tree
Development follows a strict Dependency Tree to prevent feature bloat and burnout:

Phase 1: Foundation & Pipeline → Registries, Datagens, and Python automation.

Phase 2: Core Content → Blocks, items, foods (Data Maps), shaped/shapeless recipes.

Phase 3: Furniture & Seating → Directional VoxelShapes, Mount Entities, network payloads.

Phase 4: Painting System → Canvas block entity, color palettes, custom dynamic textures.

Phase 5: Cosmetics & Crates → Player model layers, Crate block entity, client-side roll animations.

Phase 6: Biomes & Worldgen → 26.3 surface rules, custom wood types, custom dimension.

Phase 7: Mobs, NPCs & Quests → GeckoLib entities, pathfinding AI, dialogue UI, quest journal.

Phase 8: Bosses & Specials → Multi-phase boss state machines, special abilities, arena triggers.

5. Coding Standards & AI Directives (FOR CLAUDE CODE)
CRITICAL INSTRUCTIONS FOR CLAUDE CODE:

Modern Registry Patterns Only: Always use DeferredRegister, DeferredItem, and DeferredBlock. Never call legacy Forge Registries or direct constructors.

ResourceLocation Standard: Always use ResourceLocation.fromNamespaceAndPath("apeira", "path"). NEVER instantiate new ResourceLocation(...).

Data Maps over Hardcoded Java: Features like composter yields, furnace fuels, and food values MUST use NeoForge 26.3 Data Maps (data/apeira/neoforge/data_maps/...).

Networking: Use modern CustomPacketPayload and PayloadRegistrar. Never use obsolete byte-buffer packet channels.

No Entity-Lag Hacks: Do not use invisible armor stands or display entities for seating or furniture. Use VoxelShapes, BlockEntities, or temporary non-ticking mount entities.

Code Quarantine: Legacy code in /reference_code/ is strictly READ-ONLY. Use it to extract mathematical formulas, custom algorithms, and asset references, but completely rewrite registration and network logic for NeoForge 26.3.

Strict Library Separation: Generic utilities MUST go into apeira-core, apeira-decor, or apeira-story. Do not contaminate apeira-main with underlying framework code.
