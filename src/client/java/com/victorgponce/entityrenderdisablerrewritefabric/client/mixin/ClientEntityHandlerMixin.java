package com.victorgponce.entityrenderdisablerrewritefabric.client.mixin;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.victorgponce.entityrenderdisablerrewritefabric.client.culling.EntityCulling;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.client.world.ClientWorld$ClientEntityHandler")
public abstract class ClientEntityHandlerMixin {
    @WrapWithCondition(method = "stopTracking(Lnet/minecraft/entity/Entity;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;detach()V"))
    private boolean entityrenderdisabler$keepPassengers(Entity entity) {
        return !EntityCulling.get((ClientWorld) entity.getEntityWorld()).isDetaching(entity);
    }
}
