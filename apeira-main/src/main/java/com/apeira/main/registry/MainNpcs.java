package com.apeira.main.registry;

import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.main.Apeira;
import com.apeira.main.npc.JackoEntity;
import com.apeira.main.npc.JeckylEntity;
import com.apeira.main.npc.NpcEntity;
import com.apeira.main.npc.NpcShopMenu;
import com.apeira.main.realm.RealmPreset;
import com.geckolib.animation.RawAnimation;

import java.util.List;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Realm NPCs ({@link NpcEntity}), drawn from GeckoLib models by {@code client/npc/NpcRenderer} (registered in
 * {@link com.apeira.main.client.ApeiraClient}). Their size is their collision box, which the anchor cursor uses too.
 */
public final class MainNpcs {
    private static final ApeiraRegistrar R = Apeira.REGISTRAR;

    /*
     * Origin Realms' balloon conductors, one per region, each about three blocks tall with his hat and placed by his
     * region's new starter balloon ({@link #conductor(RealmPreset)}). They share one GeckoLib model and animation and
     * differ only in texture, name and greeting.
     */
    /** Lush. */
    public static final DeferredHolder<EntityType<?>, EntityType<NpcEntity>> CONDUCTOR_CARLO = conductor("conductor_carlo");
    /** Cold and Arctic. */
    public static final DeferredHolder<EntityType<?>, EntityType<NpcEntity>> CONDUCTOR_MARCO = conductor("conductor_marco");
    /** Tropical (Origin Realms' jungle). */
    public static final DeferredHolder<EntityType<?>, EntityType<NpcEntity>> CONDUCTOR_GINO = conductor("conductor_gino");
    /** Arid. */
    public static final DeferredHolder<EntityType<?>, EntityType<NpcEntity>> CONDUCTOR_ANTONIO = conductor("conductor_antonio");

    /** The conductors' shared loop, {@code geckolib/animations/entity/balloon_conductor.animation.json}. */
    private static final RawAnimation CONDUCTOR_IDLE = RawAnimation.begin().thenLoop("animation.balloon_conductor.idle");
    private static final RawAnimation JECKYL_IDLE = RawAnimation.begin().thenLoop("animation.jeckyl.idle");
    private static final RawAnimation JACKO_IDLE = RawAnimation.begin().thenLoop("animation.jacko.idle");

    /** Jeckyl, the farming accessories trader: wanders near the starter balloon until first talked to, then sells ({@link JeckylEntity}). */
    public static final DeferredHolder<EntityType<?>, EntityType<JeckylEntity>> JECKYL = R.entityType("jeckyl",
            (type, level) -> new JeckylEntity(type, level, JECKYL_IDLE), MobCategory.MISC,
            builder -> builder.sized(1.0F, 2.4F).fireImmune().clientTrackingRange(10).updateInterval(1));
    /** Jacko the farmer, who arrives after the first purchase from Jeckyl ({@link com.apeira.main.npc.JackoArrival}); his shop comes next. */
    public static final DeferredHolder<EntityType<?>, EntityType<JackoEntity>> JACKO = R.entityType("jacko",
            (type, level) -> new JackoEntity(type, level, JACKO_IDLE), MobCategory.MISC,
            builder -> builder.sized(1.0F, 2.4F).fireImmune().clientTrackingRange(10));
    /** An NPC's shop ({@link NpcShopMenu}). */
    public static final DeferredHolder<MenuType<?>, MenuType<NpcShopMenu>> NPC_SHOP_MENU = R.forRegistry(Registries.MENU)
            .register("npc_shop", () -> new MenuType<NpcShopMenu>(NpcShopMenu::new, FeatureFlags.VANILLA_SET));

    /**
     * {@code apeira:met_npc_types}: the NPC types (entity type ids) a player has talked to, which the locator bar's
     * question marks leave out ({@code client/npc/NpcQuestMarkers}). Kept by type, not by entity, so a realm reset's new
     * Jeckyl or conductor stays met. Saved, kept on death and synced only to its own player. It replaces
     * {@code apeira:met_npcs}, which kept entity UUIDs.
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<List<Identifier>>> MET_NPCS = R.forRegistry(NeoForgeRegistries.Keys.ATTACHMENT_TYPES)
            .register("met_npc_types", () -> AttachmentType.<List<Identifier>>builder(() -> List.of())
                    .serialize(Identifier.CODEC.listOf().fieldOf("met_npc_types"), met -> !met.isEmpty())
                    .copyOnDeath()
                    .sync((holder, to) -> holder == to, Identifier.STREAM_CODEC.apply(ByteBufCodecs.list()))
                    .build());

    private static DeferredHolder<EntityType<?>, EntityType<NpcEntity>> conductor(String name) {
        return R.entityType(name, (type, level) -> new NpcEntity(type, level, CONDUCTOR_IDLE), MobCategory.MISC, builder -> builder.sized(1.0F, 3.0F).fireImmune().clientTrackingRange(10));
    }

    /** The balloon conductor of a region. */
    public static EntityType<NpcEntity> conductor(RealmPreset preset) {
        return switch (preset) {
            case LUSH -> CONDUCTOR_CARLO.get();
            case COLD, ARCTIC -> CONDUCTOR_MARCO.get();
            case TROPICAL -> CONDUCTOR_GINO.get();
            case ARID -> CONDUCTOR_ANTONIO.get();
        };
    }

    private MainNpcs() {}

    public static void init(ApeiraRegistrar registrar) {}
}
