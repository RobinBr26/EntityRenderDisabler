package com.victorgponce.entityrenderdisablerrewritefabric.client.mixin;

import com.victorgponce.entityrenderdisablerrewritefabric.client.culling.EntitySounds;
import com.victorgponce.entityrenderdisablerrewritefabric.client.culling.EntityCulling;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.sound.Channel;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.victorgponce.entityrenderdisablerrewritefabric.client.config.ModConfig;

import java.util.Map;

@Mixin(SoundSystem.class)
public abstract class SoundSystemMixin {
    @Shadow @Final
    private Map<SoundInstance, Channel.SourceManager> sources;

    @Shadow
    public abstract void stop(SoundInstance sound);

    @Unique
    private long entityrenderdisabler$revision = -1;

    @Unique
    private long entityrenderdisabler$appliedRevision = -1;

    @Unique
    private ClientWorld entityrenderdisabler$world;

    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)Lnet/minecraft/client/sound/SoundSystem$PlayResult;", at = @At("HEAD"), cancellable = true)
    private void entityrenderdisabler$play(SoundInstance sound, CallbackInfoReturnable<SoundSystem.PlayResult> cir) {
        if (EntitySounds.isSuppressed(sound)) {
            cir.setReturnValue(SoundSystem.PlayResult.NOT_STARTED);
        }
    }

    @Inject(method = "tick(Z)V", at = @At("HEAD"))
    private void entityrenderdisabler$stopExisting(boolean paused, CallbackInfo ci) {
        ClientWorld world = MinecraftClient.getInstance().world;
        long appliedRevision = world == null ? -1 : EntityCulling.get(world).getAppliedRevision();
        if (entityrenderdisabler$revision != ModConfig.getRevision()
                || entityrenderdisabler$appliedRevision != appliedRevision || entityrenderdisabler$world != world) {
            entityrenderdisabler$revision = ModConfig.getRevision();
            entityrenderdisabler$appliedRevision = appliedRevision;
            entityrenderdisabler$world = world;
            sources.keySet().stream().filter(EntitySounds::isSuppressed).toList().forEach(this::stop);
        }
    }
}
