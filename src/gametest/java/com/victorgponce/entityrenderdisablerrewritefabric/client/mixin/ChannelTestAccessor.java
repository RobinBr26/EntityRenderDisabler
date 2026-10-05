package com.victorgponce.entityrenderdisablerrewritefabric.client.mixin;

import net.minecraft.client.sound.Channel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.concurrent.Executor;

@Mixin(Channel.class)
public interface ChannelTestAccessor {
    @Accessor("executor")
    Executor entityrenderdisabler$getExecutor();
}
