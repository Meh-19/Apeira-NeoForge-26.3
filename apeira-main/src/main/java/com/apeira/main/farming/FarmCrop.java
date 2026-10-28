package com.apeira.main.farming;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.apeira.core.registry.ApeiraIds;
import com.apeira.main.registry.MainFarming;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Origin Realms' eight planter box crops. A crop is planted at stage 1 and grows one stage at each 7:00 AM tick
 * ({@link FarmingService}) it was watered for, so it is ripe after {@link #days()} watered mornings, at stage
 * {@link #stages()}. Each stage is drawn with Origin Realms' model {@code item/crop/<model>_<stage>} (a ripe golden crop
 * with {@link #goldenModel()}) above the planter box, and pictured in the planter box menu with
 * {@code textures/gui/farming/<id>/<stage>.png} ({@code gold.png} when golden).
 */
public enum FarmCrop {
    APPLE("apple", "apple_tree", "apple_tree_3_gold", 5, () -> Items.APPLE, () -> Items.GOLDEN_APPLE),
    BANANA("banana", "banana_tree", "banana_tree_3_gold", 5, () -> MainFarming.BANANA.get(), () -> MainFarming.GOLDEN_BANANA.get()),
    MANGO("mango", "mango_tree", "mango_tree_3_gold", 5, () -> MainFarming.MANGO.get(), () -> MainFarming.GOLDEN_MANGO.get()),
    CORN("corn", "corn", "corn_gold", 4, () -> MainFarming.CORN.get(), () -> MainFarming.GOLDEN_CORN.get()),
    PINEAPPLE("pineapple", "pineapple", "pineapple_gold", 4, () -> MainFarming.PINEAPPLE.get(), () -> MainFarming.GOLDEN_PINEAPPLE.get()),
    EGG_PLANT("egg_plant", "egg_plant", "egg_plant_gold", 3, () -> MainFarming.EGG_PLANT.get(), () -> MainFarming.GOLDEN_EGG_PLANT.get()),
    LETTUCE("lettuce", "lettuce", "lettuce_gold", 2, () -> MainFarming.LETTUCE.get(), () -> MainFarming.GOLDEN_LETTUCE.get()),
    CHILLI("chilli", "chilli", "chilli_gold", 2, () -> MainFarming.CHILLI.get(), () -> MainFarming.GOLDEN_CHILLI.get());

    private static final FarmCrop[] VALUES = values();

    private final String id;
    private final String model;
    private final String goldenModel;
    private final int days;
    private final Supplier<Item> produce;
    private final Supplier<Item> golden;

    FarmCrop(String id, String model, String goldenModel, int days, Supplier<Item> produce, Supplier<Item> golden) {
        this.id = id;
        this.model = model;
        this.goldenModel = goldenModel;
        this.days = days;
        this.produce = produce;
        this.golden = golden;
    }

    public String id() {
        return id;
    }

    /** Watered mornings from planting to ripe. */
    public int days() {
        return days;
    }

    /** Growth stages, the planted one included; the last is ripe. */
    public int stages() {
        return days + 1;
    }

    /** What a harvest gives: the produce, or its golden form for a golden crop. */
    public Item produce(boolean golden) {
        return golden ? this.golden.get() : produce.get();
    }

    /** The item definition ({@code assets/apeira/items/crop/<model>.json}) that draws {@code stage}, or the golden crop. */
    public Identifier model(int stage, boolean golden) {
        return ApeiraIds.id("crop/" + (golden ? goldenModel : model + "_" + stage));
    }

    /** The menu picture of {@code stage}, or of the golden crop. */
    public Identifier picture(int stage, boolean golden) {
        return ApeiraIds.id("textures/gui/farming/" + id + "/" + (golden ? "gold" : stage) + ".png");
    }

    /** The crop's name, {@code crop.apeira.<id>}. */
    public String translationKey() {
        return "crop.apeira." + id;
    }

    /** The crop a seed grows, or {@code null} for anything that is not a farming seed. */
    public static @Nullable FarmCrop of(ItemStack stack) {
        return stack.getItem() instanceof FarmSeedItem seed ? seed.crop() : null;
    }

    /** The crop with this ordinal, or {@code null} (the menu syncs crops as ordinals, -1 for none). */
    public static @Nullable FarmCrop byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : null;
    }
}
