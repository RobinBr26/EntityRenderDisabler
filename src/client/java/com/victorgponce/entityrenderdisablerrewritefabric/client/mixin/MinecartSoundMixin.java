package com.victorgponce.entityrenderdisablerrewritefabric.client.mixin;

import com.victorgponce.entityrenderdisablerrewritefabric.client.culling.EntityBoundSound;
import net.minecraft.client.sound.MovingMinecartSoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(MovingMinecartSoundInstance.class)
public abstract class MinecartSoundMixin implements EntityBoundSound {
    @Shadow @Final
    private AbstractMinecartEntity minecart;

    @Override
    public Entity entityrenderdisabler$getEntity() {
        return minecart;
    }
}
