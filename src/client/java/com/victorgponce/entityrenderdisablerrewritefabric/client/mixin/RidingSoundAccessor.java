package com.victorgponce.entityrenderdisablerrewritefabric.client.mixin;

import com.victorgponce.entityrenderdisablerrewritefabric.client.culling.EntityBoundSound;
import net.minecraft.client.sound.EntityRidingSoundInstance;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityRidingSoundInstance.class)
public interface RidingSoundAccessor extends EntityBoundSound {
    @Accessor("vehicle")
    Entity entityrenderdisabler$getEntity();
}
