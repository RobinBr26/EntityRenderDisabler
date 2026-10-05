package com.victorgponce.entityrenderdisablerrewritefabric.client.sound;

import net.minecraft.client.sound.AudioStream;
import net.minecraft.client.sound.Channel;

public interface ChannelControl {
    void entityrenderdisabler$release(Channel.SourceManager manager);

    void entityrenderdisabler$attachStream(Channel.SourceManager manager, AudioStream stream);
}
