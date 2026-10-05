package com.victorgponce.entityrenderdisablerrewritefabric.client;

import com.victorgponce.entityrenderdisablerrewritefabric.client.config.ModConfig;
import com.victorgponce.entityrenderdisablerrewritefabric.client.culling.EntityCulling;
import com.victorgponce.entityrenderdisablerrewritefabric.client.mixin.ChannelTestAccessor;
import com.victorgponce.entityrenderdisablerrewritefabric.client.mixin.SoundManagerTestAccessor;
import com.victorgponce.entityrenderdisablerrewritefabric.client.mixin.SoundSystemTestAccessor;
import com.victorgponce.entityrenderdisablerrewritefabric.client.sound.ChannelControl;
import com.victorgponce.entityrenderdisablerrewritefabric.client.sound.SoundPoolInfo;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.AudioStream;
import net.minecraft.client.sound.Channel;
import net.minecraft.client.sound.EntityTrackingSoundInstance;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundEngine;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import javax.sound.sampled.AudioFormat;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class SoundProtectionGameTest {
    private SoundProtectionGameTest() {
    }

    public static void runTests(MinecraftClient client) {
        SoundSystem system = ((SoundManagerTestAccessor) client.getSoundManager()).entityrenderdisabler$getSoundSystem();
        SoundSystemTestAccessor state = (SoundSystemTestAccessor) system;
        Vec3d listener = system.getListenerTransform().position();
        int capacity = ((SoundPoolInfo) state.entityrenderdisabler$getEngine()).entityrenderdisabler$getCapacity(SoundEngine.RunMode.STATIC);
        check(capacity > 0, "Sound engine did not report its real capacity");
        ModConfig.setSoundProtection(true);
        system.stopAll();
        try {
            testFlood(system, state, listener, capacity);
            testNearestSound(system, state, listener);
            testFullPool(system, state, listener, capacity);
            testHiddenCleanup(client, system, state);
            testCancelledStream(state);
            testDisabled(system, state, listener, capacity);
        } finally {
            ModConfig.setSoundProtection(true);
            ModConfig.setEntityVisible("minecraft:zombie", true);
            EntityCulling.get(client.world).tick();
            system.stopAll();
        }
        EntityrenderdisablerrewritefabricClient.LOGGER.info("Sound protection tests passed: 1,000 mob sounds, full pool recovery and cancelled stream cleanup");
    }

    private static void testFlood(SoundSystem system, SoundSystemTestAccessor state, Vec3d listener, int capacity) {
        int started = 0;
        for (int i = 0; i < 1000; i++) {
            SilentSound sound = new SilentSound(SoundEvents.ENTITY_ZOMBIE_AMBIENT, SoundCategory.HOSTILE, listener.add(2, 0, 0));
            if (system.play(sound) != SoundSystem.PlayResult.NOT_STARTED) {
                started++;
            }
        }
        check(started > 0 && started <= 4, "Repeated mob sounds were not bounded");
        check(state.entityrenderdisabler$getSources().size() < capacity, "Mob sounds filled every audio channel");
        check(system.play(new SilentSound(SoundEvents.BLOCK_STONE_BREAK, SoundCategory.BLOCKS, listener)) != SoundSystem.PlayResult.NOT_STARTED, "Block sound was starved by mob sounds");
        check(system.play(new SilentSound(SoundEvents.ENTITY_PLAYER_HURT, SoundCategory.PLAYERS, listener)) != SoundSystem.PlayResult.NOT_STARTED, "Player sound was starved by mob sounds");
        check(system.play(new SilentSound(SoundEvents.UI_BUTTON_CLICK.value(), SoundCategory.UI, listener)) != SoundSystem.PlayResult.NOT_STARTED, "Interface sound was starved by mob sounds");
        system.stopAll();
    }

    private static void testNearestSound(SoundSystem system, SoundSystemTestAccessor state, Vec3d listener) {
        List<SilentSound> distant = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            SilentSound sound = new SilentSound(SoundEvents.ENTITY_ZOMBIE_AMBIENT, SoundCategory.HOSTILE, listener.add(10 + i, 0, 0));
            system.play(sound);
            distant.add(sound);
        }
        SilentSound close = new SilentSound(SoundEvents.ENTITY_ZOMBIE_AMBIENT, SoundCategory.HOSTILE, listener.add(1, 0, 0));
        check(system.play(close) != SoundSystem.PlayResult.NOT_STARTED, "Closer mob sound could not replace a distant duplicate");
        check(distant.stream().map(sound -> state.entityrenderdisabler$getSources().get(sound))
                .filter(manager -> manager != null).anyMatch(Channel.SourceManager::isStopped), "Replaced mob sound did not release its native channel");
        system.tick(false);
        check(state.entityrenderdisabler$getSources().containsKey(close), "Cleanup removed the replacement sound");
        system.stopAll();
    }

    private static void testFullPool(SoundSystem system, SoundSystemTestAccessor state, Vec3d listener, int capacity) {
        SilentSound mob = new SilentSound(SoundEvents.ENTITY_ZOMBIE_AMBIENT, SoundCategory.HOSTILE, listener.add(2, 0, 0));
        check(system.play(mob) != SoundSystem.PlayResult.NOT_STARTED, "Could not start the eviction test sound");
        Channel.SourceManager original = state.entityrenderdisabler$getSources().get(mob);
        for (int i = 1; i < capacity; i++) {
            check(system.play(new SilentSound(SoundEvents.BLOCK_STONE_BREAK, SoundCategory.BLOCKS, listener)) != SoundSystem.PlayResult.NOT_STARTED, "Could not fill the real sound pool");
        }
        SilentSound player = new SilentSound(SoundEvents.ENTITY_PLAYER_HURT, SoundCategory.PLAYERS, listener);
        check(system.play(player) != SoundSystem.PlayResult.NOT_STARTED, "Important sound failed to reclaim a channel from a full pool");
        check(original.isStopped(), "Full-pool replacement left the old native channel allocated");
        system.tick(false);
        check(!state.entityrenderdisabler$getSources().containsKey(mob), "Evicted sound remained in playback bookkeeping");
        check(state.entityrenderdisabler$getSources().containsKey(player), "Important replacement was lost");
        system.stopAll();
    }

    private static void testHiddenCleanup(MinecraftClient client, SoundSystem system, SoundSystemTestAccessor state) {
        ZombieEntity entity = new ZombieEntity(EntityType.ZOMBIE, client.world);
        entity.setId(2_000_000_100);
        entity.refreshPositionAndAngles(client.player.getEntityPos().add(3, 0, 0), 0, 0);
        client.world.addEntity(entity);
        SilentBoundSound playing = new SilentBoundSound(entity);
        SilentBoundSound delayed = new SilentBoundSound(entity);
        SilentBoundSound queued = new SilentBoundSound(entity);
        check(system.play(playing) != SoundSystem.PlayResult.NOT_STARTED, "Could not start the hidden-sound cleanup test");
        Channel.SourceManager original = state.entityrenderdisabler$getSources().get(playing);
        system.play(delayed, 100);
        system.playNextTick(queued);
        ModConfig.setEntityVisible("minecraft:zombie", false);
        EntityCulling.get(client.world).tick();
        system.tick(false);
        audioBarrier(state);
        check(original.isStopped(), "Hidden sound retained a native audio channel");
        check(!state.entityrenderdisabler$getSources().containsKey(playing), "Hidden sound remained in playback bookkeeping");
        check(!state.entityrenderdisabler$getTicking().contains(playing), "Hidden loop remained in the tick list");
        check(!state.entityrenderdisabler$getDelayed().containsKey(delayed), "Hidden delayed sound remained scheduled");
        check(!state.entityrenderdisabler$getNextTick().contains(queued), "Hidden sound remained queued for the next tick");
        client.world.removeEntity(entity.getId(), Entity.RemovalReason.DISCARDED);
        ModConfig.setEntityVisible("minecraft:zombie", true);
        EntityCulling.get(client.world).tick();
        system.stopAll();
    }

    private static void testCancelledStream(SoundSystemTestAccessor state) {
        Channel channel = state.entityrenderdisabler$getChannel();
        Channel.SourceManager manager = channel.createSource(SoundEngine.RunMode.STREAMING).join();
        check(manager != null, "Could not allocate a streaming channel for the cleanup test");
        EmptyStream stream = new EmptyStream();
        ChannelControl control = (ChannelControl) channel;
        control.entityrenderdisabler$release(manager);
        control.entityrenderdisabler$release(manager);
        control.entityrenderdisabler$attachStream(manager, stream);
        audioBarrier(state);
        check(manager.isStopped() && stream.closed, "Stream completed after cancellation without releasing its resources");
    }

    private static void testDisabled(SoundSystem system, SoundSystemTestAccessor state, Vec3d listener, int capacity) {
        ModConfig.setSoundProtection(false);
        int count = Math.min(12, capacity);
        for (int i = 0; i < count; i++) {
            SilentSound sound = new SilentSound(SoundEvents.ENTITY_ZOMBIE_AMBIENT, SoundCategory.HOSTILE, listener.add(2, 0, 0));
            check(system.play(sound) != SoundSystem.PlayResult.NOT_STARTED, "Disabling sound protection did not restore normal allocation");
        }
        check(state.entityrenderdisabler$getSources().size() == count, "Disabled sound protection still limited duplicates");
        ModConfig.setSoundProtection(true);
        system.stopAll();
    }

    private static void audioBarrier(SoundSystemTestAccessor state) {
        CompletableFuture<Void> barrier = new CompletableFuture<>();
        ((ChannelTestAccessor) state.entityrenderdisabler$getChannel()).entityrenderdisabler$getExecutor().execute(() -> barrier.complete(null));
        barrier.orTimeout(5, TimeUnit.SECONDS).join();
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class SilentSound extends PositionedSoundInstance {
        private SilentSound(SoundEvent event, SoundCategory category, Vec3d position) {
            super(event, category, 0, 1, Random.create(1), position.x, position.y, position.z);
            repeat = true;
        }

        @Override
        public boolean shouldAlwaysPlay() {
            return true;
        }
    }

    private static final class SilentBoundSound extends EntityTrackingSoundInstance {
        private SilentBoundSound(Entity entity) {
            super(SoundEvents.ENTITY_ZOMBIE_AMBIENT, SoundCategory.HOSTILE, 0, 1, entity, 1);
            repeat = true;
        }

        @Override
        public boolean shouldAlwaysPlay() {
            return true;
        }
    }

    private static final class EmptyStream implements AudioStream {
        private boolean closed;

        @Override
        public AudioFormat getFormat() {
            return new AudioFormat(44100, 16, 1, true, false);
        }

        @Override
        public ByteBuffer read(int size) {
            return ByteBuffer.allocate(0);
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}
