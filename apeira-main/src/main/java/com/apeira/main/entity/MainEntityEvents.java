package com.apeira.main.entity;

import com.apeira.main.registry.MainEntities;

import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

/** Mod bus events for Xenon's mobs: their attributes. */
public final class MainEntityEvents {
    private MainEntityEvents() {}

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(MainEntities.TURKEY.get(), TurkeyEntity.createAttributes().build());
        event.put(MainEntities.GRIZZLY_BEAR.get(), GrizzlyBearEntity.createAttributes().build());
        event.put(MainEntities.CROCODILE.get(), CrocodileEntity.createAttributes().build());
        event.put(MainEntities.WRAITH.get(), WraithEntity.createAttributes().build());
    }
}
