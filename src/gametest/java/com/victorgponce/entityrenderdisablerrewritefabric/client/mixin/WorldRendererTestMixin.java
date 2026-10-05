package com.victorgponce.entityrenderdisablerrewritefabric.client.mixin;

import net.minecraft.client.render.WorldRenderer;
import com.victorgponce.entityrenderdisablerrewritefabric.client.WorldRendererTestAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererTestMixin implements WorldRendererTestAccess {
    @Unique
    private int entityrenderdisabler$reloads;

    @Inject(method = "reload()V", at = @At("HEAD"))
    private void entityrenderdisabler$reload(CallbackInfo ci) {
        entityrenderdisabler$reloads++;
    }

    @Override
    public int entityrenderdisabler$getReloads() {
        return entityrenderdisabler$reloads;
    }
}
