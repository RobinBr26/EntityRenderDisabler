package com.victorgponce.entityrenderdisablerrewritefabric.client.mixin;

import com.victorgponce.entityrenderdisablerrewritefabric.client.culling.EntityBoundSound;
import net.minecraft.client.sound.AbstractBeeSoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.BeeEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AbstractBeeSoundInstance.class)
public abstract class BeeSoundMixin implements EntityBoundSound {
    @Shadow @Final
    protected BeeEntity bee;

    @Override
    public Entity entityrenderdisabler$getEntity() {
        return bee;
    }
}
