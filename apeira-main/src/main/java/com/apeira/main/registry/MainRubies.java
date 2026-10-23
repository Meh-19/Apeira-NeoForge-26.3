package com.apeira.main.registry;

import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.main.Apeira;
import com.apeira.main.ruby.ApeiraRubyDepositMenu;
import com.apeira.main.ruby.RubyDepositBlock;
import com.apeira.main.ruby.RubyDepositBlockEntity;
import com.mojang.serialization.Codec;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** The ruby economy: the balance attachment, the ruby deposit block, its block entity and its menu. */
public final class MainRubies {
    private static final ApeiraRegistrar R = Apeira.REGISTRAR;

    /**
     * {@code apeira:rubies}: a player's ruby balance ({@link com.apeira.main.ruby.Rubies}). Saved (when not 0), kept on
     * death, and synced only to the player it belongs to, for the HUD counter and the deposit menu.
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Long>> RUBIES = R.forRegistry(NeoForgeRegistries.Keys.ATTACHMENT_TYPES)
            .register("rubies", () -> AttachmentType.builder(() -> 0L)
                    .serialize(Codec.LONG.fieldOf("rubies"), rubies -> rubies != 0L)
                    .copyOnDeath()
                    .sync((holder, to) -> holder == to, ByteBufCodecs.VAR_LONG)
                    .build());

    /** The ruby deposit (Origin Realms' ruby ATM): an enchanting table's properties, drawn by its block entity. */
    public static final DeferredBlock<RubyDepositBlock> RUBY_DEPOSIT = R.blockWithItem("ruby_deposit", RubyDepositBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> 7)
                    .strength(5.0F, 1200.0F)
                    .noOcclusion(),
            properties -> properties);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RubyDepositBlockEntity>> RUBY_DEPOSIT_ENTITY = R.blockEntityType(
            "ruby_deposit", RubyDepositBlockEntity::new, RUBY_DEPOSIT);
    public static final DeferredHolder<MenuType<?>, MenuType<ApeiraRubyDepositMenu>> RUBY_DEPOSIT_MENU = R.forRegistry(Registries.MENU)
            .register("ruby_deposit", () -> new MenuType<ApeiraRubyDepositMenu>(ApeiraRubyDepositMenu::new, FeatureFlags.VANILLA_SET));

    private MainRubies() {}

    public static void init(ApeiraRegistrar registrar) {}
}
