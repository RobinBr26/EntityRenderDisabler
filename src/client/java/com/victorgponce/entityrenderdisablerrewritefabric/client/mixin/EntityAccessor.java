package com.victorgponce.entityrenderdisablerrewritefabric.client.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.world.entity.EntityChangeListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface EntityAccessor {
    @Accessor("changeListener")
    EntityChangeListener entityrenderdisabler$getChangeListener();
}
