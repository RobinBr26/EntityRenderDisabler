package com.victorgponce.entityrenderdisablerrewritefabric.client.mixin;

import com.victorgponce.entityrenderdisablerrewritefabric.client.config.ModConfig;
import com.victorgponce.entityrenderdisablerrewritefabric.client.culling.CampfireCulling;
import net.minecraft.block.BlockState;
import net.minecraft.client.particle.CampfireSmokeParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CampfireSmokeParticle.class)
public abstract class CampfireSmokeParticleMixin extends Particle {
    @Unique
    private BlockState entityrenderdisabler$source;

    protected CampfireSmokeParticleMixin(ClientWorld world, double x, double y, double z) {
        super(world, x, y, z);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void entityrenderdisabler$source(CallbackInfo ci) {
        entityrenderdisabler$source = CampfireCulling.getSmokeSource();
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void entityrenderdisabler$stopSmoke(CallbackInfo ci) {
        if (entityrenderdisabler$source != null && ModConfig.isBlockHidden(entityrenderdisabler$source)) {
            markDead();
            ci.cancel();
        }
    }
}
