package com.victorgponce.entityrenderdisablerrewritefabric.client.mixin;

import net.minecraft.client.sound.Channel;
import net.minecraft.client.sound.Source;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Channel.SourceManager.class)
public interface SourceManagerAccessor {
    @Accessor("source")
    Source entityrenderdisabler$getSource();
}
