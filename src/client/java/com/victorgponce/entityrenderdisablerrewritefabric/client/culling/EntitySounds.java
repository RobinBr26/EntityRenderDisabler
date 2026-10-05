package com.victorgponce.entityrenderdisablerrewritefabric.client.culling;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.world.ClientWorld;

public final class EntitySounds {
    private EntitySounds() {
    }

    public static boolean isSuppressed(SoundInstance sound) {
        if (CullingEffects.isSuppressed()) {
            return true;
        }
        if (sound instanceof EntityBoundSound tracking) {
            return EntityCulling.isHidden(tracking.entityrenderdisabler$getEntity());
        }
        ClientWorld world = MinecraftClient.getInstance().world;
        return world != null && !sound.isRelative()
                && (CampfireCulling.isSoundSuppressed(sound, world)
                || EntityCulling.get(world).suppressPositionalSound(sound.getId(), sound.getX(), sound.getY(), sound.getZ()));
    }
}
