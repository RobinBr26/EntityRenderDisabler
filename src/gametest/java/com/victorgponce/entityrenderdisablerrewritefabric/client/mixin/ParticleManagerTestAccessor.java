package com.victorgponce.entityrenderdisablerrewritefabric.client.mixin;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Queue;

@Mixin(ParticleManager.class)
public interface ParticleManagerTestAccessor {
    @Accessor("newParticles")
    Queue<Particle> entityrenderdisabler$getNewParticles();
}
