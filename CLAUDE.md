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

Phase 2: Core Content → Blocks, items, foods (food data components), shaped/shapeless recipes. (In progress: the legacy ORForged mod's materials, tools, armour, mineral blocks and woods ported, see section 7)

Phase 3: Furniture & Seating → Directional VoxelShapes, Mount Entities, network payloads. (In progress, see section 8)

Phase 4: Painting System → Canvas block entity, color palettes, custom dynamic textures. (In progress: painting table, paintbrush and paintable furniture ported from CosmoLib, see section 9)

Phase 5: Cosmetics & Crates → Player model layers, Crate block entity, client-side roll animations. (In progress: CosmoLib's cosmetics, wardrobe and crates and the Crates mod's cosmetics and 14 crates ported, see sections 10 and 11)

Phase 6: Biomes & Worldgen → 26.3 surface rules, custom wood types, custom dimension. (ORForged's woods exist as blocks, section 7; they have no saplings, trees or ore generation yet)

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

Legacy source: the content is the legacy ORForged mod's (`Meh-19/orforged-1.20.1-0.1`, which has the textures and lang; `orforged-1.20.4` has the same registries). Names, ids, textures and properties are ORForged's. ORForged had no recipes beyond fungal, vertical planks and beams, so the others follow vanilla (see `MainRecipeProvider`). Not ported, because ORForged has no textures for them: its stone variants, paper lanterns, stained stone, vanilla vertical planks and diamond pillar. ORForged has no vines or lily pads, and no block of it ticks.

MainItems: `platinum` and `ruby`; platinum's sword, pickaxe, axe, shovel and hoe (`PLATINUM_TOOL_MATERIAL`: diamond tier, 500 uses, speed 7, +2.2 damage, enchantability 12, repaired with `#apeira:platinum_tool_materials`; every tool gets 3 attack damage and attack speed 2, as in ORForged) and armour (`PLATINUM_ARMOR_MATERIAL`: durability 25, defence 2/6/7/3, enchantability 12, iron's equip sound, `#apeira:repairs_platinum_armor`). The armour model is the hand-made equipment asset `assets/apeira/equipment/platinum.json` (`textures/entity/equipment/humanoid{,_leggings}/platinum.png`, ORForged's armour layers; no baby layer). Armour items use NeoForge's dynamic trim model (`ApeiraItemModelProvider#trimmableArmor`), so trims show without a model per trim material.

MainBlocks: platinum and ruby blocks, bricks and pillars (an iron block's properties, strength 5/6, stone sounds; pillars are `RotatedPillarBlock`s), their ores and deepslate ores (`DropExperienceBlock`, 2-5 experience, stone with hardness 2; ores need a stone tool, deepslate ores iron, and drop the material with Fortune or themselves with Silk Touch) and `diamond_brick`. All are mined with a pickaxe.

MainWoods: seven trees (maple, bleak, fir, palm, bayou, wisteria, baobab), each with a log, wood, stripped log and wood (strength 4), planks with stairs, slab, button, pressure plate, fence and gate (core's `PlankSet`), vertical planks, carved planks (`<wood>_planks_carved`), one or two leaves colours and a beam; plus `palm_crown`, four `wisteria_flowers_<colour>`, `fungal` planks in plain and 16 dyed sets with vertical and carved fungal planks, and beams and carved planks for the ten vanilla woods. Leaves are vanilla `LeavesBlock`s, which decay by their `distance` state; with no saplings they drop sticks, or themselves with shears or Silk Touch (`ApeiraBlockLootProvider#createLeavesDropsWithoutSapling`). Axe stripping is the `neoforge:transformables` data map and burning the `neoforge:flammable` data map (`MainDataMapProvider`, vanilla's odds); wooden items burn in furnaces through `cookingFuel`. Each tree's logs are in `#apeira:<wood>_logs` (`MainTags#logs`), which is in `#minecraft:logs_that_burn`; planks, leaves and shapes are in vanilla's block and item tags (`MainBlockItemTags`, written by both tag providers). `MainWoods#all` lists every block it registers.

MainCreativeTabs: the `Apeira Content` tab (`apeira:content`, title key `itemGroup.apeira.content`, ruby icon), registered with `ApeiraRegistrar#creativeTab`. It lists every item of the registrar's item register in registration order, so new blocks and items appear in it without changes here, except cosmetics and crates, which are in the `Cosmetics` tab (`apeira:cosmetics`, western hat icon): crates first, then cosmetics grouped by crate.

MainCosmetics and MainCrates: the Crates mod's cosmetics and crates, see sections 10 and 11.

Datagen: `MainBlockStateProvider` (block states and models: cubes with a `block/<name>` texture; pillars, logs and beams with `block/<name>` sides and `block/<name>_top` ends and a horizontal model; woods with their log's side texture; wood families through vanilla's `BlockModelGenerators#family`), `MainModelProvider` (flat and handheld item models, `item/<name>` texture), `MainRecipeProvider` (shaped, shapeless, smelting and blasting recipes; wood families through vanilla's `RecipeProvider#generateRecipes`), `MainBlockLootSubProvider` (block loot tables), `MainBlockTagProvider` and `MainItemTagProvider` (tool, armour, wood and mining tags), `MainDataMapProvider` (stripping and flammability) and `MainLanguageProvider` (en_us names and the tab titles). Core helpers for these: `ApeiraRegistrar#blockWithItem(name, factory, Supplier<Properties>, itemProperties)` (copied properties and fuel items), `PlankSet`, `ApeiraBlockStateProvider#log/family/pillar/leaves`, `ApeiraItemTagsProvider` (`ApeiraDataGenerators#itemTags`), `BlockAndItemTags` for tags written to both registries, and `ApeiraDataMapProvider#strippable/flammable`. Loot tables are datapack registry entries in 26.x like recipes: core's `ApeiraBlockLootProvider` extends vanilla's `BlockLootSubProvider`, `ApeiraDataGenerators#blockLoot` runs it through `GatherDataRegistryEntriesEvent`, and it only checks the registrar's own blocks, so every new block needs a loot table (or `noDrop()`) or the data run fails. Block tags extend core's `ApeiraBlockTagsProvider` (NeoForge's `BlockTagsProvider`) and are added with `ApeiraDataGenerators#blockTags`; in 26.x tag appenders take `ResourceKey`s, which its `add(tag, blocks...)` helper takes from each `DeferredBlock`. Core's `ApeiraModelProvider` is final and runs every model provider of a module, so module model providers extend `ApeiraBlockStateProvider` or `ApeiraItemModelProvider`. Register client-only providers (block states, item models) in `MainData` with lambdas, not constructor references, so a dedicated server never loads their classes.

Generated resources: `./gradlew :apeira-main:runData` writes to `apeira-main/src/generated/resources/`, which is committed. CI runs it and fails when the committed files are out of date. The language file is generated, so `assets/apeira/lang/en_us.json` must not also exist in `src/main/resources`.

Textures: `src/main/resources/assets/apeira/textures/{block,item}/<name>.png`, ORForged's, copied as they are except `<x>_pillar_side` (now `<x>_pillar`) and `<wood>_planks_carved_top` (now `<wood>_planks_carved`, used on every side, the only carved texture ORForged has). The ores and the crimson and warped beams are animated (`.png.mcmeta`).

Creative tab: the `Apeira Content` tab also lists the decor module's items (furniture, painting table, paintbrush), after the main module's.

8. Furniture & Seating (`apeira-decor`)
apeira-decor depends on apeira-core and has its own `ApeiraRegistrar` (`ApeiraDecor.REGISTRAR`, shared `apeira` namespace; entity types use `ApeiraRegistrar#entityType`) and `ApeiraDataGenerators` (`ApeiraDecor.DATA`). Holder classes are under `com.apeira.decor.registry`, data providers under `com.apeira.decor.datagen` (added in `DecorData.init`).

Blocks (`com.apeira.decor.block`): `ApeiraFurnitureBlock` is the abstract base: a `HorizontalDirectionalBlock` (`FACING`) that faces the placing player and takes its shape facing north, turned for the other facings with `Shapes.rotateHorizontal`. Shapes and models are authored facing north (the front, where a sitter looks out, on the north side); build the shape with `parse_blockbench_hitboxes.py` from the model. `ApeiraChairBlock` adds right-click sitting and a seat height in pixels; sneaking players and players already riding don't sit. Minecraft 26.x blocks have no codecs, so subclasses only need a `(Properties)` constructor. Furniture blocks need `noOcclusion()`.

Seating (`com.apeira.decor.entity`): `ApeiraSeatEntity` (`apeira:seat`, `DecorEntities.SEAT`) is an invisible, zero-size entity spawned at the chair centre at seat height, so its block position is the chair's. One seat per chair: sitting reuses an empty seat and fails if someone already sits there. Its tick does nothing but discard it when it has no passenger or its block is no longer a chair, and it discards itself when its passenger dismounts; riding is forced, so the dismount cooldown does not block getting back up. Players stand up in front of the chair (else beside, behind, then on top). It is saved with the chunk because vanilla only lets players ride savable entities, which also reseats a player who logged out sitting. Every entity type needs a client renderer even when invisible: `client/ApeiraDecorClient` (`@Mod(dist = Dist.CLIENT)`) registers a `NoopRenderer`.

DecorBlocks: `oak_chair` (`OakChairBlock`), wooden properties (oak planks' strength and sound, flammable). Mined with an axe (`minecraft:mineable/axe`), since it is wooden furniture; drops itself, keeping its paint. Its seat cushion (the model's `tintindex` 0 faces) is paintable (section 9). Model and texture are the server pack's `custom/furniture/western/saloon_chair`, converted with `process_server_pack.py --base-item firework_star --match 'furniture/western/saloon_chair$' --copy-textures --output apeira-decor/src/main/resources/assets/apeira` and `flatten_textures.py --assets apeira-decor/src/main/resources/assets/apeira`, then moved to `models/block/oak_chair.json` and `textures/block/oak_chair.png`. Furniture models are hand-made resources in `apeira-decor/src/main/resources/assets/apeira/models/block/`; datagen writes only their block states (`ApeiraBlockStateProvider#furniture`), and block items use the block model.

Datagen: `./gradlew :apeira-main:runDecorData` (the `decorData` run in apeira-main, `--mod apeira_decor`) writes to `apeira-decor/src/generated/resources/`, which is committed; CI runs it after `runData` and checks both folders. Each module's run only writes its own mod's providers, so a new decor block needs its block state, language, loot and tag entries in the decor providers, not the main ones.

Server pack: furniture models are in the pack's `assets/minecraft/items/firework_star.json` (`custom/furniture/...`), decorations and blocks in `paper.json`. Pack models for display entities are often offset (for example 8 pixels down); check that a model's cubes lie in 0-16 (backrests may reach higher) before using it as a block model.

9. Painting (`apeira-decor`)
Ported from the legacy CosmoLib mod (`net.meh.cosmolib.paint`, `PaintingTableBlock`, `PaintingTableMenu`, `PaintingTableScreen`, `PaintbrushItem`; the GitHub repo `Meh-19/CosmoLib-NeoForge-1.21.X`, branch `menna-1`, matches the copy in `reference-code`). Code is under `com.apeira.decor.paint`, `.block`, `.item`, `.menu` and `.client`.

Paint: a 24-bit RGB value, either one of `PaintColor`'s nine colours times seven shades (CosmoLib's palette) or a `FinishType` (14 finishes stored under CosmoLib's reserved values: red and blue 255, green `(id - 1) * 2`). Items carry it in the `apeira:paint` data component (`DecorDataComponents.PAINT`, saved and synced; CosmoLib used a `PaintColor` int in `custom_data`). `Paint` reads and writes it, gives the tint (`Paint#tint`) and the tooltip line ("Painted Light Blue, Shade 3", "Gold Finish"), which the client adds to any painted item through `ItemTooltipEvent`.

Paintable blocks: implement `PaintableBlock` (an `EntityBlock` whose block entity is `PaintBlockEntity`, type `apeira:paint`, `DecorBlockEntities.PAINT`) and add the block to that type's valid blocks. The block entity stores the paint as its `apeira:paint` component, so placing a painted item paints the block, pick-block copies it, and the loot table copies it back with `copy_components` (`DecorBlockLootSubProvider#dropSelfWithPaint`). Paint changes are sent with the vanilla block entity update packet, and the client rebuilds the chunk section. On the client, register `PaintBlockTint` for the block in `ApeiraDecorClient#registerBlockTints` (faces with `tintindex` 0 take the paint, unpainted faces keep their texture) and give the item the `apeira:paint` item tint source (`PaintTintSource`, registered in `ApeiraDecorClient#registerItemTints`; datagen: `DecorBlockStateProvider#paintableItem`). The `oak_chair` is the only paintable block so far.

`painting_table` (`PaintingTableBlock`, faces the player, front/side/top/bottom textures from CosmoLib, axe, drops itself; recipe: two white wool over four planks): opens `PaintingTableMenu` (`DecorMenus.PAINTING_TABLE`, screen `client/PaintingTableScreen`). Put a paintbrush or a paintable block's item in the input slot, pick a colour (3x3 grid) and shade (row below), or click the paint icon for the finishes (two pages of seven), and take one painted copy, which uses up one input. The screen sends its clicks as vanilla menu button clicks (`clickMenuButton`) and the selection comes back through data slots, so there are no custom payloads (CosmoLib had none either). GUI textures are `textures/gui/painting_table*.png`.

`paintbrush` (`PaintbrushItem`, 300 durability; recipe: feather, copper ingot, stick, stacked): gets its paint in the painting table. Right-clicking a paintable block paints it and costs one durability; sneak-right-clicking a painted block copies its paint onto the brush. It acts in `onItemUseFirst`, before the block's own right-click, so painting a chair does not sit on it. Model and textures are CosmoLib's Blockbench brush (`models/item/paintbrush.json`, `textures/item/paintbrush.png` and the animated `paintbrush_paint.png`, whose drip is `tintindex` 0).

Not ported, or changed:
- Finish effects: CosmoLib drew finishes with replacement core shaders (`assets/minecraft/shaders/core/rendertype_entity_*`, `rendertype_gui`, a finish atlas bound to `Sampler3`). 26.x renders through `RenderPipeline`s and has no `RenderSystem.setShaderTexture`, so finishes are drawn in their representative colour for now, and the screen names each finish on hover. The GUI-only `finish_preview` item is gone with them.
- The `cosmolib:paintable` block tag (empty in CosmoLib, and a block without a `PaintBlockEntity` has nowhere to keep paint). Paintable cosmetics are ported (section 10).
- Fixed from CosmoLib: the menu lost its input item when closed, shift-clicking the output gave a painted copy without using up the input, and `stillValid` was always true.

10. Cosmetics & Wardrobe (`apeira-decor` framework, `apeira-main` content)
Ported from CosmoLib (`net.meh.cosmolib.cosmetic`, its wardrobe screen and cosmetic layer) and the Crates mod (GitHub `Meh-19/Crates-NeoForge-1.21.1`; `CratesItems`, `models/cosmetics/`). Framework code is under `com.apeira.decor.cosmetic`, `.menu` and `.client`; the cosmetics themselves are `com.apeira.main.registry.MainCosmetics`.

`CosmeticItem`: a stack-of-one item with a `CosmeticSlot` (`HAT`, `BACK`, `HAND`), a `CosmeticRarity` (common, rare, epic, legendary, limited: the name colour and a badge glyph from the `apeira:cosmetic` bitmap font, `assets/apeira/font/cosmetic.json` and `textures/gui/lore_*.png`), whether it is paintable (paintable badge in the tooltip) and an optional default paint, set as the item's default `apeira:paint` component. The tooltip ends with the gold "<Theme> Crate Exclusive" line (`tooltip.apeira.exclusive.<crate>`); the paint line is hidden while a cosmetic keeps its default paint.

Painting: `Paint#canPaint` accepts paintable cosmetics, so the painting table paints them, and the paintbrush paints the paintable cosmetic in the other hand (right-click) or copies its paint (sneak-right-click). Painted faces are the model's `tintindex` 0 faces, through the `apeira:paint` item tint source.

Wardrobe: the `apeira:wardrobe` player attachment (`DecorAttachments.WARDROBE`, the `Wardrobe` record: one stack per slot), saved, kept on death and synced to every client tracking the player. The period key (`key.apeira.open_wardrobe`) sends `OpenWardrobePayload`; the server opens `WardrobeMenu` (`DecorMenus.WARDROBE`, screen `client/WardrobeScreen` on CosmoLib's `cosmetic_screen.png`, as `textures/gui/wardrobe.png`, with a turning player preview). The server menu's cosmetic slots are the attachment itself, so CosmoLib's trust in a client-sent stack is gone.

Render layer: `client/CosmeticLayer` is added to player and mannequin renderers of both skins (`EntityRenderersEvent.AddLayers`); `CosmeticLayer.MODIFIER` (a render state modifier) resolves the worn stacks into the avatar render state. The hat is drawn on the head as vanilla's head items, the back cosmetic on the body, the hand cosmetic in the off hand when it is empty, each with the item model's `head` (hat, back) or third-person hand display. Nothing is drawn for invisible players.

Content: 177 cosmetics in `MainCosmetics`, with the Crates mod's slots, rarities, paintability, default paints and crates. 127 have the mod's 3-D model (`models/item/cosmetic/<name>.json`, textures in `textures/item/`); their item definitions (`MainModelProvider#cosmetic`) show it in the head and third-person hand contexts and the rarity token (`item/<rarity>_<slot>_token`) elsewhere. The other 50 had no model in the Crates mod and show only the token. Names and exclusive lines are in `MainLanguageProvider`.

Not ported, or changed:
- No Curios: CosmoLib kept its own wardrobe, and so does Apeira, as an attachment.
- Tool skins and the Crates mod's furniture blocks (most of each crate's loot) are not ported yet.
- Finish effects are not drawn on cosmetics either (section 9); finishes show their representative colour.
- CosmoLib's per-cosmetic back offset config is dropped; back cosmetics use one fixed offset.

11. Crates (`apeira-decor` framework, `apeira-main` content)
Ported from CosmoLib's `net.meh.cosmolib.crate` (`CrateEntity`, `CrateLootTable`, `CrateRarityWeights`, `CrateLuckAttachment`, `CrateCameraController`, `CrateHud`, `CrateItemLayer`, `CrateRenderer`) and the Crates mod's crate assets. Framework code is under `com.apeira.decor.crate`, `.network` and `.client.crate`; the crates are `com.apeira.main.registry.MainCrates`.

GeckoLib: apeira-decor depends on GeckoLib 5 (`com.geckolib:geckolib-neoforge-26.3`, version `geckolib_version` in `gradle.properties`, Cloudsmith maven) as an `api` dependency, and its mods.toml requires `geckolib`. GeckoLib's mixins make vanilla's render states implement its `GeoRenderState` at runtime; for compiling, apeira-decor passes a copy of GeckoLib's interface injections (`apeira-decor/geckolib_interface_injections.json`) to ModDevGradle's `interfaceInjectionData`. Update the copy when GeckoLib's changes. Files: `assets/<namespace>/geckolib/models/<path>.geo.json` and `geckolib/animations/<path>.animation.json`, both referred to as `<namespace>:<path>`.

Block: CosmoLib's crate was an entity placed by an item; Apeira's is a block, `CrateBlock` (`ROTATION_16`, kept even: crates face the placing player in 45 degree steps, as CosmoLib's did), with a `CrateBlockEntity` (`apeira:crate`, `DecorBlockEntities.CRATE`, valid for every `CrateBlock` of any module, collected from the block registry). The block renders `INVISIBLE`; its block model only gives breaking particles. A closed crate breaks and drops itself (loot table); while open it cannot be broken by players or explosions. Crate items stack to 16.

Opening (`CrateBlockEntity`, all server side, synced with the block entity update packet): right-click rolls three items and starts `OPENING` (76 ticks), then `LOOP`, where the opener crouches to collect or punches the crate to reroll (`REROLLING`, 60 ticks, 3 rerolls, `CrateEvents` handles the punch through `PlayerInteractEvent.LeftClickBlock`); after the last reroll the items are collected after 40 ticks. Collecting gives the items (dropping what does not fit), updates luck and plays `VANISHING` (16 ticks), then the block is removed. If the opener leaves (logout, other dimension, more than 8 blocks) the items drop and the crate vanishes. The opener gets Resistance V and `LivingIncomingDamageEvent` cancels any damage. The state, opener, rerolls and items are saved, so an opening survives a restart (CosmoLib reset to closed).

Loot (`CrateLoot`, loaded by `CrateLootManager` from `data/<namespace>/crate_loot/<crate>.json`, keyed by the crate's block id): lists of item ids for `common`, `rare`, `epic`, `legendary` and `limited`. Weights 60/25/10/4/1; the first item is always rare or better; an empty tier falls back to a lower one, then a higher one. Luck (`CrateLuck`, `apeira:crate_luck` player attachment, saved, kept on death, max 60) moves weight from common to rare (common never below 5): a legendary or limited resets it, three commons add 15, one rare and two commons add 8, two rares or more without an epic add 3. CosmoLib's `config/cosmolib/crate_rarity_weights.json` is replaced by constants in `CrateLoot`.

Client: `client/crate/CrateRenderer` (a `GeoBlockRenderer`) draws `geckolib/models/crate/<crate>.geo.json` with `textures/entity/crate/<crate>.png`, turned by `180 - yaw`; `CrateItemLayer` draws the rolled items on the model's `item1`-`item3` bones (scale 0.7, ground display) and, in `LOOP`, their names facing the camera. Animations are `geckolib/animations/crate/<crate>.animation.json` with clips `animation.<crate>.open`, `.open_loop`, `.reroll` and `.vanish` (CosmoLib asked for `animation.crate.*`, which the Crates mod's files do not have). `CrateOpenPayload` / `CrateUnlockPayload` lock and free the camera (`CrateCamera`: eases to the crate over 20 ticks, then keeps the player aimed at it; 25% slower, no hotbar scrolling); `CrateHud` shows CosmoLib's hints (`message.apeira.crate.*`).

Content: 14 crates in `MainCrates` (aether, animal, aquatic, atlantis, bathroom, beach, business, camping, construction, easter, fast food, festive, fun fair, zodiac). Assets were copied from the Crates mod: geo models (with empty `item1`-`item3` bones added under `base`, which the animations move but the models lacked, and zodiac's `crate_base`/`crate_lid` renamed `base`/`lid` to match its animations), animations, entity textures, the crate item models (`models/item/<crate>.json`, texture moved to `textures/block/<crate>.png`). Loot is the Crates mod's `loot_tables/crates/<crate>.json` with only the ported cosmetics kept. Datagen writes their block states, item definitions, loot tables (drop self), `minecraft:mineable/axe` tags and names; the block and item models are hand-made resources. The Crates mod has no crate recipes, so there are none.

Not ported:
- The other 19 Crates mod crates (gothic, harvest, kitchen, lovely, lunar, medieval, mining, muerte, music, palace, paradise, shamrock, space, spooky, spring, toy, western, winter, wizardry) have no geo model in the reference. The garden crate has a model, but its loot is only furniture, none of it ported yet, so it could not roll anything. `badger_crate.geo.json` has no crate type.
- CosmoLib's two-hit break with wobble (an entity behaviour): a closed crate breaks like a block.
