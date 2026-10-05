package com.victorgponce.entityrenderdisablerrewritefabric.client.mixin;

import com.victorgponce.entityrenderdisablerrewritefabric.client.culling.EntityBoundSound;
import net.minecraft.client.sound.SnifferDigSoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.SnifferEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SnifferDigSoundInstance.class)
public abstract class SnifferSoundMixin implements EntityBoundSound {
    @Shadow @Final
    private SnifferEntity sniffer;

    @Override
    public Entity entityrenderdisabler$getEntity() {
        return sniffer;
    }
}
