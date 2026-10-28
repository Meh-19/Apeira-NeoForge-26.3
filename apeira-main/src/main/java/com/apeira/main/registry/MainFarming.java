package com.apeira.main.registry;

import java.util.List;
import java.util.function.Function;

import com.apeira.core.registry.ApeiraIds;
import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.main.Apeira;
import com.apeira.main.farming.FarmCrop;
import com.apeira.main.farming.FarmSeedItem;
import com.apeira.main.farming.Fertilizer;
import com.apeira.main.farming.FertilizerItem;
import com.apeira.main.farming.PlanterBoxBlock;
import com.apeira.main.farming.PlanterBoxBlockEntity;
import com.apeira.main.farming.PlanterBoxMenu;
import com.apeira.main.farming.SoilItem;
import com.apeira.main.farming.SprinklerBlock;
import com.apeira.main.farming.SprinklerBlockEntity;
import com.apeira.main.farming.WateringCanItem;
import com.mojang.serialization.Codec;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.core.component.DataComponentType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

/**
 * Origin Realms' farming (section 19 of the project notes): the planter box, the eight seeds and their produce (and its
 * golden form), Bag O' Soil, the four fertilizers, the watering can and the two sprinklers, the planter box's block
 * entity and menu, the crop quality and watering can components and the hoe quality data map.
 */
public final class MainFarming {
    private static final ApeiraRegistrar R = Apeira.REGISTRAR;

    // Data

    /** A harvested crop's star rating, 1 to 3 ({@link com.apeira.main.farming.CropQuality}); golden produce has none. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CROP_QUALITY = R.dataComponent("crop_quality",
            builder -> builder.persistent(Codec.intRange(1, 3)).networkSynchronized(ByteBufCodecs.VAR_INT));
    /** Uses of water left in a watering can; no component when empty. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> WATER = R.dataComponent("water",
            builder -> builder.persistent(Codec.intRange(1, WateringCanItem.CAPACITY)).networkSynchronized(ByteBufCodecs.VAR_INT));
    /**
     * {@code apeira:hoe_quality} ({@code data/apeira/data_maps/item/hoe_quality.json}): how much a hoe raises a manual
     * harvest's rating, 0 to 1 (netherite 1). Hands and tools without an entry count as 0.
     */
    public static final DataMapType<Item, Float> HOE_QUALITY = DataMapType.builder(ApeiraIds.id("hoe_quality"), Registries.ITEM,
            Codec.floatRange(0.0F, 1.0F)).synced(Codec.FLOAT, false).build();

    // Seeds

    public static final DeferredItem<FarmSeedItem> APPLE_SEEDS = seed("apple_seeds", FarmCrop.APPLE);
    public static final DeferredItem<FarmSeedItem> BANANA_SEEDS = seed("banana_seeds", FarmCrop.BANANA);
    public static final DeferredItem<FarmSeedItem> MANGO_PIT = seed("mango_pit", FarmCrop.MANGO);
    public static final DeferredItem<FarmSeedItem> CORN_SEEDS = seed("corn_seeds", FarmCrop.CORN);
    public static final DeferredItem<FarmSeedItem> PINEAPPLE_SEEDS = seed("pineapple_seeds", FarmCrop.PINEAPPLE);
    public static final DeferredItem<FarmSeedItem> EGG_PLANT_SEEDS = seed("egg_plant_seeds", FarmCrop.EGG_PLANT);
    public static final DeferredItem<FarmSeedItem> LETTUCE_SEEDS = seed("lettuce_seeds", FarmCrop.LETTUCE);
    public static final DeferredItem<FarmSeedItem> CHILLI_SEEDS = seed("chilli_seeds", FarmCrop.CHILLI);

    // Produce: the apple crop gives vanilla apples and golden apples.

    public static final DeferredItem<Item> BANANA = food("banana", 4, 0.3F);
    public static final DeferredItem<Item> MANGO = food("mango", 5, 0.4F);
    public static final DeferredItem<Item> CORN = food("corn", 3, 0.6F);
    public static final DeferredItem<Item> PINEAPPLE = food("pineapple", 6, 0.4F);
    public static final DeferredItem<Item> EGG_PLANT = food("egg_plant", 4, 0.5F);
    public static final DeferredItem<Item> LETTUCE = food("lettuce", 2, 0.3F);
    public static final DeferredItem<Item> CHILLI = food("chilli", 2, 0.2F);
    public static final DeferredItem<Item> GOLDEN_BANANA = golden("golden_banana", 4);
    public static final DeferredItem<Item> GOLDEN_MANGO = golden("golden_mango", 5);
    public static final DeferredItem<Item> GOLDEN_CORN = golden("golden_corn", 3);
    public static final DeferredItem<Item> GOLDEN_PINEAPPLE = golden("golden_pineapple", 6);
    public static final DeferredItem<Item> GOLDEN_EGG_PLANT = golden("golden_egg_plant", 4);
    public static final DeferredItem<Item> GOLDEN_LETTUCE = golden("golden_lettuce", 2);
    public static final DeferredItem<Item> GOLDEN_CHILLI = golden("golden_chilli", 2);

    // Soil, fertilizers and watering

    public static final DeferredItem<SoilItem> BAG_O_SOIL = R.items().registerItem("bag_o_soil", SoilItem::new);
    public static final DeferredItem<FertilizerItem> FERTILIZER_MULTIPLIER = fertilizer("fertilizer_multiplier", Fertilizer.PRODUCE_MULTIPLIER);
    public static final DeferredItem<FertilizerItem> FERTILIZER_SPEED = fertilizer("fertilizer_speed", Fertilizer.SPEED_GROW);
    public static final DeferredItem<FertilizerItem> FERTILIZER_STAR = fertilizer("fertilizer_star", Fertilizer.THREE_STAR_GUARANTEE);
    public static final DeferredItem<FertilizerItem> FERTILIZER_GOLD = fertilizer("fertilizer_gold", Fertilizer.GOLDEN_GOODS);
    public static final DeferredItem<WateringCanItem> WOODEN_WATERING_CAN = R.items().registerItem("wooden_watering_can", WateringCanItem::new,
            properties -> properties.stacksTo(1));

    // Blocks

    public static final DeferredBlock<PlanterBoxBlock> PLANTER_BOX = R.blockWithItem("planter_box", PlanterBoxBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(2.0F, 3.0F)
                    .sound(SoundType.WOOD)
                    .ignitedByLava(),
            properties -> properties);
    public static final DeferredBlock<SprinklerBlock> WOODEN_SPRINKLER = R.blockWithItem("wooden_sprinkler", p -> new SprinklerBlock(2, p),
            () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(1.5F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
                    .ignitedByLava(),
            properties -> properties);
    public static final DeferredBlock<SprinklerBlock> IRON_SPRINKLER = R.blockWithItem("iron_sprinkler", p -> new SprinklerBlock(3, p),
            () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion(),
            properties -> properties);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlanterBoxBlockEntity>> PLANTER_BOX_ENTITY = R.blockEntityType(
            "planter_box", PlanterBoxBlockEntity::new, PLANTER_BOX);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SprinklerBlockEntity>> SPRINKLER_ENTITY = R.blockEntityType(
            "sprinkler", SprinklerBlockEntity::new, WOODEN_SPRINKLER, IRON_SPRINKLER);
    public static final DeferredHolder<MenuType<?>, MenuType<PlanterBoxMenu>> PLANTER_BOX_MENU = R.forRegistry(Registries.MENU)
            .register("planter_box", () -> new MenuType<PlanterBoxMenu>(PlanterBoxMenu::new, FeatureFlags.VANILLA_SET));

    private MainFarming() {}

    public static void init(ApeiraRegistrar registrar) {}

    public static void registerDataMaps(RegisterDataMapTypesEvent event) {
        event.register(HOE_QUALITY);
    }

    /** The seed that plants {@code crop}. */
    public static Item seed(FarmCrop crop) {
        return seeds().get(crop.ordinal()).get();
    }

    /** The seeds, in {@link FarmCrop} order. */
    public static List<DeferredItem<FarmSeedItem>> seeds() {
        return List.of(APPLE_SEEDS, BANANA_SEEDS, MANGO_PIT, CORN_SEEDS, PINEAPPLE_SEEDS, EGG_PLANT_SEEDS, LETTUCE_SEEDS, CHILLI_SEEDS);
    }

    /** The produce Apeira adds, then its golden forms, in {@link FarmCrop} order (the apple crop's are vanilla's). */
    public static List<DeferredItem<Item>> produce() {
        return List.of(BANANA, MANGO, CORN, PINEAPPLE, EGG_PLANT, LETTUCE, CHILLI);
    }

    public static List<DeferredItem<Item>> goldenProduce() {
        return List.of(GOLDEN_BANANA, GOLDEN_MANGO, GOLDEN_CORN, GOLDEN_PINEAPPLE, GOLDEN_EGG_PLANT, GOLDEN_LETTUCE, GOLDEN_CHILLI);
    }

    public static List<DeferredItem<FertilizerItem>> fertilizers() {
        return List.of(FERTILIZER_MULTIPLIER, FERTILIZER_SPEED, FERTILIZER_STAR, FERTILIZER_GOLD);
    }

    private static DeferredItem<FarmSeedItem> seed(String name, FarmCrop crop) {
        return R.items().registerItem(name, (Function<Item.Properties, FarmSeedItem>) p -> new FarmSeedItem(p, crop));
    }

    private static DeferredItem<FertilizerItem> fertilizer(String name, Fertilizer type) {
        return R.items().registerItem(name, (Function<Item.Properties, FertilizerItem>) p -> new FertilizerItem(p, type),
                properties -> properties.rarity(Rarity.UNCOMMON));
    }

    private static DeferredItem<Item> food(String name, int nutrition, float saturation) {
        return R.items().registerItem(name, Item::new,
                properties -> properties.food(new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).build()));
    }

    /** Golden produce: two more nutrition than the crop's, saturation 1.2, always edible, as golden carrots and apples are. */
    private static DeferredItem<Item> golden(String name, int nutrition) {
        return R.items().registerItem(name, Item::new, properties -> properties.rarity(Rarity.RARE)
                .food(new FoodProperties.Builder().nutrition(nutrition + 2).saturationModifier(1.2F).alwaysEdible().build()));
    }
}
