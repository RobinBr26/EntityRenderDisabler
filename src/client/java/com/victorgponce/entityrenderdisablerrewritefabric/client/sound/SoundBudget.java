package com.victorgponce.entityrenderdisablerrewritefabric.client.sound;

import com.victorgponce.entityrenderdisablerrewritefabric.client.culling.EntityBoundSound;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.Channel;
import net.minecraft.client.sound.Sound;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Vec3d;

import java.util.Map;
import java.util.function.Predicate;

public final class SoundBudget {
    public static final Decision ALLOW = new Decision(true, null);
    public static final Decision REJECT = new Decision(false, null);

    private SoundBudget() {
    }

    public static Decision choose(SoundInstance incoming, Map<SoundInstance, Channel.SourceManager> sources,
                                  Predicate<SoundInstance> retired, int capacity, Vec3d listener) {
        if (capacity <= 0 || incoming.getSound() == null) {
            return ALLOW;
        }
        int priority = priority(incoming);
        double distance = distance(incoming, listener);
        Sound clip = incoming.getSound();
        if (priority < 3 && incoming.getAttenuationType() == SoundInstance.AttenuationType.LINEAR) {
            double range = Math.max(incoming.getVolume(), 1) * clip.getAttenuation();
            if (distance >= range * range) {
                return REJECT;
            }
        }
        int active = 0;
        int mobs = 0;
        int duplicates = 0;
        SoundInstance worstMob = null;
        SoundInstance worstDuplicate = null;
        for (Map.Entry<SoundInstance, Channel.SourceManager> entry : sources.entrySet()) {
            SoundInstance sound = entry.getKey();
            if (retired.test(sound) || entry.getValue().isStopped() || sound.getSound() == null
                    || sound.getSound().isStreamed() != clip.isStreamed()) {
                continue;
            }
            active++;
            if (priority(sound) < 3) {
                mobs++;
                worstMob = worse(worstMob, sound, listener);
                if (sound.getId().equals(incoming.getId())) {
                    duplicates++;
                    worstDuplicate = worse(worstDuplicate, sound, listener);
                }
            }
        }
        if (priority < 3) {
            int duplicateLimit = Math.min(priority == 1 ? 4 : 8, Math.max(1, capacity / 4));
            if (duplicates >= duplicateLimit) {
                return replace(incoming, worstDuplicate, listener);
            }
            int reserve = Math.min(capacity - 1, Math.min(32, Math.max(2, capacity / 4)));
            int mobLimit = Math.max(1, Math.min(64, capacity - reserve));
            if (mobs >= mobLimit || active >= capacity - reserve) {
                return replace(incoming, worstMob, listener);
            }
        }
        if (active >= capacity) {
            return worstMob == null ? REJECT : new Decision(true, worstMob);
        }
        return ALLOW;
    }

    private static Decision replace(SoundInstance incoming, SoundInstance victim, Vec3d listener) {
        if (victim != null && (priority(incoming) > priority(victim)
                || priority(incoming) == priority(victim) && distance(incoming, listener) + 0.25 < distance(victim, listener))) {
            return new Decision(true, victim);
        }
        return REJECT;
    }

    private static SoundInstance worse(SoundInstance first, SoundInstance second, Vec3d listener) {
        if (first == null || priority(second) < priority(first)
                || priority(second) == priority(first) && distance(second, listener) > distance(first, listener)) {
            return second;
        }
        return first;
    }

    public static int priority(SoundInstance sound) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (sound.isRelative() || sound.getCategory() == SoundCategory.PLAYERS || sound.getCategory() == SoundCategory.MASTER) {
            return 3;
        }
        boolean entitySound = sound.getCategory() == SoundCategory.HOSTILE || sound.getCategory() == SoundCategory.NEUTRAL;
        if (sound instanceof EntityBoundSound bound) {
            Entity entity = bound.entityrenderdisabler$getEntity();
            if (entity instanceof PlayerEntity || entity == client.getCameraEntity()
                    || client.player != null && entity.hasPassengerDeep(client.player)) {
                return 3;
            }
            entitySound = true;
        }
        if (!entitySound) {
            return 3;
        }
        String path = sound.getId().getPath();
        return sound.isRepeatable() && !path.contains("attack")
                || path.contains("ambient") || path.endsWith(".step") || path.endsWith(".swim")
                || path.contains("flap") || path.contains("breathe") ? 1 : 2;
    }

    private static double distance(SoundInstance sound, Vec3d listener) {
        if (sound.isRelative()) {
            return 0;
        }
        if (sound instanceof EntityBoundSound bound) {
            return listener.squaredDistanceTo(bound.entityrenderdisabler$getEntity().getEntityPos());
        }
        return listener.squaredDistanceTo(sound.getX(), sound.getY(), sound.getZ());
    }

    public record Decision(boolean allowed, SoundInstance victim) {
    }
}
