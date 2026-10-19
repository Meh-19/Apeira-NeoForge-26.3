package com.apeira.main.datagen;

import java.util.stream.Stream;

import com.apeira.main.registry.MainEntities;
import com.apeira.main.registry.MainMaterials;

import net.minecraft.data.loot.EntityLootSubProvider;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.EnchantedCountIncreaseFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.functions.SmeltItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemKilledByPlayerCondition;
import net.minecraft.world.level.storage.loot.providers.number.floats.ContextFloatProviders;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

/**
 * Loot tables of Xenon's mobs, as Xenon dropped them: a turkey drops one or two raw turkeys (cooked when it burns) and one
 * or two feathers, a crocodile killed by a player up to three teeth (one more per Looting level at most), and the
 * grizzly bear and wraith nothing.
 */
public final class MainEntityLootSubProvider extends EntityLootSubProvider {
    public MainEntityLootSubProvider(LootTableSubProvider.Context context) {
        super(FeatureFlags.REGISTRY.allFlags(), context);
    }

    @Override
    public void generate() {
        add(MainEntities.TURKEY.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(ContextIntProviders.exactly(1)).add(LootItem.lootTableItem(MainMaterials.RAW_TURKEY.get())
                        .apply(SetItemCountFunction.setCount(ContextIntProviders.between(1, 2)))
                        .apply(SmeltItemFunction.smelted().when(shouldSmeltLoot()))))
                .withPool(LootPool.lootPool().setRolls(ContextIntProviders.exactly(1)).add(LootItem.lootTableItem(Items.FEATHER)
                        .apply(SetItemCountFunction.setCount(ContextIntProviders.between(1, 2))))));
        add(MainEntities.CROCODILE.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(ContextIntProviders.exactly(1)).add(LootItem.lootTableItem(MainMaterials.CROCODILE_TOOTH.get())
                        .apply(SetItemCountFunction.setCount(ContextIntProviders.between(0, 3)))
                        .apply(EnchantedCountIncreaseFunction.lootingMultiplier(enchantments, ContextFloatProviders.between(0.0F, 1.0F))))
                        .when(LootItemKilledByPlayerCondition.killedByPlayer())));
        add(MainEntities.GRIZZLY_BEAR.get(), LootTable.lootTable());
        add(MainEntities.WRAITH.get(), LootTable.lootTable());
    }

    @Override
    protected Stream<EntityType<?>> getKnownEntityTypes() {
        return MainEntities.all().stream();
    }
}
