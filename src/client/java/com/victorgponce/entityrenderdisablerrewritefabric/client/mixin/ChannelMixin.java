package com.victorgponce.entityrenderdisablerrewritefabric.client.mixin;

import com.victorgponce.entityrenderdisablerrewritefabric.client.EntityrenderdisablerrewritefabricClient;
import com.victorgponce.entityrenderdisablerrewritefabric.client.sound.ChannelControl;
import net.minecraft.client.sound.AudioStream;
import net.minecraft.client.sound.Channel;
import net.minecraft.client.sound.Source;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.Executor;

@Mixin(Channel.class)
public abstract class ChannelMixin implements ChannelControl {
    @Shadow @Final
    private Set<Channel.SourceManager> sources;

    @Shadow @Final
    private Executor executor;

    @Override
    public void entityrenderdisabler$release(Channel.SourceManager manager) {
        executor.execute(() -> {
            if (sources.remove(manager) && !manager.isStopped()) {
                manager.close();
            }
        });
    }

    @Override
    public void entityrenderdisabler$attachStream(Channel.SourceManager manager, AudioStream stream) {
        executor.execute(() -> {
            Source source = ((SourceManagerAccessor) manager).entityrenderdisabler$getSource();
            if (source != null && !manager.isStopped()) {
                source.setStream(stream);
                source.play();
            } else {
                try {
                    stream.close();
                } catch (IOException exception) {
                    EntityrenderdisablerrewritefabricClient.LOGGER.warn("Failed to close a cancelled sound stream", exception);
                }
            }
        });
    }
}
