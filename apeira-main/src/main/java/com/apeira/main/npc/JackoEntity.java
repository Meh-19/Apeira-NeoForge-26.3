package com.apeira.main.npc;

import org.jspecify.annotations.Nullable;

import com.geckolib.animation.RawAnimation;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Jacko the farmer, who arrives at the 7:00 AM after a realm's first purchase from Jeckyl ({@link JackoArrival}). He buys
 * crops in Origin Realms; for now he stands where he arrived, says one of his two lines and sells seeds
 * ({@link NpcShop#JACKO}): left-clicking him in his dialogue opens his market.
 */
public class JackoEntity extends NpcEntity {
    private static final String[] LINES = {"dialogue.apeira.jacko.welcome", "dialogue.apeira.jacko.crops"};

    public JackoEntity(EntityType<? extends NpcEntity> type, Level level, RawAnimation idle) {
        super(type, level, idle);
    }

    @Override
    public @Nullable NpcShop shop() {
        return NpcShop.JACKO;
    }

    @Override
    public String lineFor(ServerPlayer player) {
        return LINES[getRandom().nextInt(LINES.length)];
    }
}
