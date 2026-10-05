package com.victorgponce.entityrenderdisablerrewritefabric.client.mixin;

import com.google.common.collect.Multimap;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.victorgponce.entityrenderdisablerrewritefabric.client.config.ModConfig;
import com.victorgponce.entityrenderdisablerrewritefabric.client.culling.EntityCulling;
import com.victorgponce.entityrenderdisablerrewritefabric.client.culling.EntitySounds;
import com.victorgponce.entityrenderdisablerrewritefabric.client.sound.ChannelControl;
import com.victorgponce.entityrenderdisablerrewritefabric.client.sound.SoundBudget;
import com.victorgponce.entityrenderdisablerrewritefabric.client.sound.SoundPoolInfo;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.AudioStream;
import net.minecraft.client.sound.Channel;
import net.minecraft.client.sound.SoundEngine;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundListenerTransform;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.client.sound.TickableSoundInstance;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.sound.SoundCategory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Mixin(SoundSystem.class)
public abstract class SoundSystemMixin {
    @Shadow @Final
    private Map<SoundInstance, Channel.SourceManager> sources;

    @Shadow @Final
    private Channel channel;

    @Shadow @Final
    private SoundEngine soundEngine;

    @Shadow @Final
    private Multimap<SoundCategory, SoundInstance> sounds;

    @Shadow @Final
    private List<TickableSoundInstance> tickingSounds;

    @Shadow @Final
    private Map<SoundInstance, Integer> soundStartTicks;

    @Shadow @Final
    private Map<SoundInstance, Integer> soundEndTicks;

    @Shadow @Final
    private List<TickableSoundInstance> soundsToPlayNextTick;

    @Shadow
    public abstract SoundListenerTransform getListenerTransform();

    @Unique
    private final Map<SoundInstance, Channel.SourceManager> entityrenderdisabler$retired = new IdentityHashMap<>();

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

    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)Lnet/minecraft/client/sound/SoundSystem$PlayResult;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sound/Channel;createSource(Lnet/minecraft/client/sound/SoundEngine$RunMode;)Ljava/util/concurrent/CompletableFuture;"),
            cancellable = true)
    private void entityrenderdisabler$admitSound(SoundInstance sound, CallbackInfoReturnable<SoundSystem.PlayResult> cir) {
        if (!ModConfig.isSoundProtectionEnabled()) {
            return;
        }
        SoundEngine.RunMode mode = sound.getSound().isStreamed() ? SoundEngine.RunMode.STREAMING : SoundEngine.RunMode.STATIC;
        int capacity = ((SoundPoolInfo) soundEngine).entityrenderdisabler$getCapacity(mode);
        SoundBudget.Decision decision = SoundBudget.choose(sound, sources, entityrenderdisabler$retired::containsKey,
                capacity, getListenerTransform().position());
        if (!decision.allowed()) {
            cir.setReturnValue(SoundSystem.PlayResult.NOT_STARTED);
        } else if (decision.victim() != null) {
            entityrenderdisabler$retire(decision.victim());
        }
    }

    @WrapOperation(method = "play(Lnet/minecraft/client/sound/SoundInstance;)Lnet/minecraft/client/sound/SoundSystem$PlayResult;",
            at = @At(value = "INVOKE", target = "Ljava/util/concurrent/CompletableFuture;thenAccept(Ljava/util/function/Consumer;)Ljava/util/concurrent/CompletableFuture;", ordinal = 1))
    private CompletableFuture<Void> entityrenderdisabler$attachStream(CompletableFuture<AudioStream> future, Consumer<AudioStream> consumer,
                                                                      Operation<CompletableFuture<Void>> original,
                                                                      @Local Channel.SourceManager manager) {
        return original.call(future, (Consumer<AudioStream>) stream ->
                ((ChannelControl) channel).entityrenderdisabler$attachStream(manager, stream));
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
            sources.keySet().stream().filter(EntitySounds::isSuppressed).toList().forEach(this::entityrenderdisabler$retire);
            soundStartTicks.keySet().removeIf(EntitySounds::isSuppressed);
            soundsToPlayNextTick.removeIf(EntitySounds::isSuppressed);
        }
        entityrenderdisabler$retired.forEach((sound, manager) -> {
            if (sources.get(sound) == manager) {
                sources.remove(sound);
                soundStartTicks.remove(sound);
                soundEndTicks.remove(sound);
                sounds.remove(sound.getCategory(), sound);
                tickingSounds.remove(sound);
                soundsToPlayNextTick.remove(sound);
            }
        });
        entityrenderdisabler$retired.clear();
    }

    @Unique
    private void entityrenderdisabler$retire(SoundInstance sound) {
        Channel.SourceManager manager = sources.get(sound);
        if (manager != null && entityrenderdisabler$retired.putIfAbsent(sound, manager) == null) {
            ((ChannelControl) channel).entityrenderdisabler$release(manager);
        }
    }

    @Inject(method = "stopAll", at = @At("RETURN"))
    private void entityrenderdisabler$reset(CallbackInfo ci) {
        entityrenderdisabler$retired.clear();
    }
}
