package com.victorgponce.entityrenderdisablerrewritefabric.client.mixin;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientPlayNetworkHandler.class)
public interface ClientPlayNetworkHandlerAccessor {
    @Invoker("playSpawnSound")
    void entityrenderdisabler$playSpawnSound(Entity entity);
}
