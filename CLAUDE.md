# Project Context: Apeira

## 1. Executive Summary & Vision
**Apeira** is a modular, high-performance Minecraft Java mod built on **NeoForge 26.3** (Java 25). 

The goal of Apeira is to bring the rich, custom, interactive survival server experience of *Origin Realms* into a **native, singleplayer/multiplayer modded environment**.

### Why Apeira Exists:
* **Eliminate Server-Side Hackiness & Entity Lag:** Traditional custom servers rely heavily on display entities, invisible armor stands, complex resource pack hacks, and plugin overhead. This creates massive server TPS drop, entity tick lag, and rendering stutters.
* **Native Modded Performance:** Apeira replaces command blocks, plugins, and armor-stand tricks with clean, data-driven NeoForge blocks, VoxelShapes, custom client-side block entity renderers, and payload-driven UI networks.
* **Modular Architecture:** Rather than building a single monolithic mod file, Apeira is split into **three foundational library subprojects** (`apeira-core`, `apeira-decor`, `apeira-story`) and **one main content module** (`apeira-main`).

---

## 2. Technical Stack & Foundation
* **Target Engine:** Minecraft Java Edition / **NeoForge 26.3**
* **Language / JDK:** Java 25 (Minecraft 26.x requires it)
* **Build System:** Gradle Multi-Project using the official `net.neoforged.moddev` plugin
* **Asset Pipeline:** Data Generators (Datagens) + Python scripts in `scripts/` for model, texture and hitbox conversion (see section 6)
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

Phase 1: Foundation & Pipeline → Registries, Datagens, and Python automation. (Done)

Phase 2: Core Content → Blocks, items, foods (food data components), shaped/shapeless recipes. (In progress, see section 7)

Phase 3: Furniture & Seating → Directional VoxelShapes, Mount Entities, network payloads.

Phase 4: Painting System → Canvas block entity, color palettes, custom dynamic textures.

Phase 5: Cosmetics & Crates → Player model layers, Crate block entity, client-side roll animations.

Phase 6: Biomes & Worldgen → 26.3 surface rules, custom wood types, custom dimension.

Phase 7: Mobs, NPCs & Quests → GeckoLib entities, pathfinding AI, dialogue UI, quest journal.

Phase 8: Bosses & Specials → Multi-phase boss state machines, special abilities, arena triggers.

5. Coding Standards & AI Directives (FOR CLAUDE CODE)
CRITICAL INSTRUCTIONS FOR CLAUDE CODE:

Modern Registry Patterns Only: Always use DeferredRegister, DeferredItem, and DeferredBlock. Never call legacy Forge Registries or direct constructors.

Identifier Standard: Minecraft 26.x renamed ResourceLocation to net.minecraft.resources.Identifier. Always use ApeiraIds.id("path") or Identifier.fromNamespaceAndPath("apeira", "path"). NEVER instantiate new Identifier(...) or refer to ResourceLocation.

Data-Driven over Hardcoded Java: Furnace fuel and composter chances are item properties in 26.x (Item.Properties#cookingFuel and #compostable, keyed into the minecraft:context_int_provider registry); NeoForge 26.3 removed its furnace_fuels and compostables data maps. Food values are the food data component (Item.Properties#food). Custom per-item values (sell prices, quest rewards) use data maps under data/<namespace>/data_maps/..., written with ApeiraDataMapProvider.

Networking: Use modern CustomPacketPayload and PayloadRegistrar. Never use obsolete byte-buffer packet channels.

No Entity-Lag Hacks: Do not use invisible armor stands or display entities for seating or furniture. Use VoxelShapes, BlockEntities, or temporary non-ticking mount entities.

Code Quarantine: Legacy code in /reference_code/ is strictly READ-ONLY. Use it to extract mathematical formulas, custom algorithms, and asset references, but completely rewrite registration and network logic for NeoForge 26.3.

Strict Library Separation: Generic utilities MUST go into apeira-core, apeira-decor, or apeira-story. Do not contaminate apeira-main with underlying framework code.

6. Asset Automation Scripts (`scripts/`)
Python 3.10+ scripts that use only the standard library. Run them from the repository root; each has `--help`, and every default path below can be changed with an option. Scripts that write files never replace an existing file unless given `--force`, and accept `--dry-run` to only print what they would do. Tests: `python -m unittest discover -s scripts/tests`.

process_server_pack.py: Converts the custom models of the Origin Realms server pack (default `reference_code/server_resource_pack/`, which is git-ignored) into `apeira-main/src/main/resources/assets/apeira/models/item/<name>.json`. It reads both CustomModelData formats: legacy `overrides` in `assets/minecraft/models/item/*.json`, and the 1.21.4+ item definitions in `assets/minecraft/items/*.json` that the current pack actually uses (`range_dispatch`, `select` or `condition` on `custom_model_data`). Models are named after their file in snake_case, with parent folder names added only where two would clash; pack parent models are copied too, and texture references move to `apeira:item/...` or `apeira:block/...`. `--copy-textures` copies the textures, `--item-definitions` also writes `assets/apeira/items/<name>.json` (only needed for items whose definition is more than one model, such as bows or trimmed armour, since datagen writes plain ones), `--base-item` and `--match` pick a subset, and a CustomModelData -> item report goes to `build/asset-scripts/server_pack_manifest.json`.

flatten_textures.py: Moves every texture under `assets/apeira/textures/` up to its first folder (`item/`, `block/`, ...; the atlases load sprites by those folders, so they stay) and renames files to lowercase snake_case, carrying `.png.mcmeta` files along and updating texture references in `assets/apeira/models/`. Two files that would get the same name are reported and left in place (exit code 1), or with `--on-collision prefix` get their parent folder names as a prefix.

make_placeholder_textures.py: Draws a 16x16 placeholder PNG into `apeira-main/src/main/resources/assets/apeira/textures/` for every `apeira:block/...` or `apeira:item/...` texture that a generated model refers to but that does not exist yet: bricks for blocks, a gem shape for items, coloured from the texture name. Run it after datagen adds a block or item; real textures replace the placeholders, which are never overwritten without `--force`.

parse_blockbench_hitboxes.py: Reads `.bbmodel` or Java model JSON files and prints `VoxelShape` constants built from `Block.box(x1, y1, z1, x2, y2, z2)` in pixel coordinates, for apeira-decor furniture. Flat cubes are dropped, contained cubes removed and aligned cubes merged; rotated cubes use the box around their corners. A Blockbench outliner group named `hitbox` or `collision` is used on its own when present. `--facings` adds east, south and west versions of a north-facing model, `--bounds` gives one box, `--min-size` drops small details, `--clamp` cuts boxes to the block, and `-o` writes to a file.

7. Content Layout (`apeira-main`)
Content lives in holder classes under `com.apeira.main.registry`, each with a static `init(ApeiraRegistrar)` that `Apeira`'s constructor passes to `REGISTRAR.register(...)` as a `RegistryModule`. Data providers live under `com.apeira.main.datagen` and are added in `MainData.init`, which `Apeira` passes to `DATA.register(...)`.

MainBlocks: `carved_apeira_stone` and `apeira_brick`, stone-like blocks registered with `blockWithItem`, so each has a plain block item. Both drop themselves, are mined with a pickaxe and need a stone tool or better.

MainItems: crafting materials `apeira_ingot` and `apeira_gem`; `apeira_ration` (food: 10 nutrition, saturation modifier 1.0); `ember_shard` (fuel: `cookingFuel(ContextIntProviders.COOKING_TIME_BLAZE_ROD)`, 12 items); `moss_clump` (compostable: `compostable(ContextIntProviders.COMPOSTABLE_MEDIUM)`, 65%). Names and values are placeholders.

MainCreativeTabs: the `Apeira Content` tab (`apeira:content`, title key `itemGroup.apeira.content`, gem icon), registered with `ApeiraRegistrar#creativeTab`. It lists every item of the registrar's item register in registration order, so new blocks and items appear in it without changes here.

Datagen: `MainBlockStateProvider` (block states and cube models, `block/<name>` texture), `MainModelProvider` (flat item models, `item/<name>` texture), `MainRecipeProvider` (shaped, shapeless and stonecutting recipes), `MainBlockLootSubProvider` (block loot tables), `MainBlockTagProvider` (`minecraft:mineable/pickaxe` and `minecraft:needs_stone_tool`) and `MainLanguageProvider` (en_us names and the tab title). Loot tables are datapack registry entries in 26.x like recipes: core's `ApeiraBlockLootProvider` extends vanilla's `BlockLootSubProvider`, `ApeiraDataGenerators#blockLoot` runs it through `GatherDataRegistryEntriesEvent`, and it only checks the registrar's own blocks, so every new block needs a loot table (or `noDrop()`) or the data run fails. Block tags extend core's `ApeiraBlockTagsProvider` (NeoForge's `BlockTagsProvider`) and are added with `ApeiraDataGenerators#blockTags`; in 26.x tag appenders take `ResourceKey`s, which its `add(tag, blocks...)` helper takes from each `DeferredBlock`. Core's `ApeiraModelProvider` is final and runs every model provider of a module, so module model providers extend `ApeiraBlockStateProvider` or `ApeiraItemModelProvider`. Register client-only providers (block states, item models) in `MainData` with lambdas, not constructor references, so a dedicated server never loads their classes.

Generated resources: `./gradlew :apeira-main:runData` writes to `apeira-main/src/generated/resources/`, which is committed. CI runs it and fails when the committed files are out of date. The language file is generated, so `assets/apeira/lang/en_us.json` must not also exist in `src/main/resources`.

Textures: `src/main/resources/assets/apeira/textures/{block,item}/<name>.png`. All seven are placeholders from `make_placeholder_textures.py` until real art replaces them.
