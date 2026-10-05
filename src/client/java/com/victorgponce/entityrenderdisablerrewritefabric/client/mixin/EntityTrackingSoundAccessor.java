package com.victorgponce.entityrenderdisablerrewritefabric.client.mixin;

import net.minecraft.client.sound.EntityTrackingSoundInstance;
import com.victorgponce.entityrenderdisablerrewritefabric.client.culling.EntityBoundSound;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityTrackingSoundInstance.class)
public interface EntityTrackingSoundAccessor extends EntityBoundSound {
    @Accessor("entity")
    Entity entityrenderdisabler$getEntity();
}
