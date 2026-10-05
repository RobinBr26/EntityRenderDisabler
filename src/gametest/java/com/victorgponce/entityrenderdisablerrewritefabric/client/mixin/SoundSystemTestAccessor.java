package com.victorgponce.entityrenderdisablerrewritefabric.client.mixin;

import net.minecraft.client.sound.Channel;
import net.minecraft.client.sound.SoundEngine;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.client.sound.TickableSoundInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.Map;

@Mixin(SoundSystem.class)
public interface SoundSystemTestAccessor {
    @Accessor("sources")
    Map<SoundInstance, Channel.SourceManager> entityrenderdisabler$getSources();

    @Accessor("channel")
    Channel entityrenderdisabler$getChannel();

    @Accessor("soundEngine")
    SoundEngine entityrenderdisabler$getEngine();

    @Accessor("soundStartTicks")
    Map<SoundInstance, Integer> entityrenderdisabler$getDelayed();

    @Accessor("soundsToPlayNextTick")
    List<TickableSoundInstance> entityrenderdisabler$getNextTick();

    @Accessor("tickingSounds")
    List<TickableSoundInstance> entityrenderdisabler$getTicking();
}
