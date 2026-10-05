package com.victorgponce.entityrenderdisablerrewritefabric.client.mixin;

import com.victorgponce.entityrenderdisablerrewritefabric.client.sound.SoundPoolInfo;
import net.minecraft.client.sound.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin implements SoundPoolInfo {
    @Unique
    private int entityrenderdisabler$staticCapacity;

    @Unique
    private int entityrenderdisabler$streamingCapacity;

    @ModifyArg(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sound/SoundEngine$SourceSetImpl;<init>(I)V", ordinal = 0), index = 0)
    private int entityrenderdisabler$staticCapacity(int capacity) {
        entityrenderdisabler$staticCapacity = capacity;
        return capacity;
    }

    @ModifyArg(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sound/SoundEngine$SourceSetImpl;<init>(I)V", ordinal = 1), index = 0)
    private int entityrenderdisabler$streamingCapacity(int capacity) {
        entityrenderdisabler$streamingCapacity = capacity;
        return capacity;
    }

    @Override
    public int entityrenderdisabler$getCapacity(SoundEngine.RunMode mode) {
        return mode == SoundEngine.RunMode.STREAMING ? entityrenderdisabler$streamingCapacity : entityrenderdisabler$staticCapacity;
    }
}
