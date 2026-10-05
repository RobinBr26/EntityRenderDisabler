package com.victorgponce.entityrenderdisablerrewritefabric.client.culling;

import com.victorgponce.entityrenderdisablerrewritefabric.client.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.block.BlockState;
import net.minecraft.world.World;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;

public final class CampfireCulling {
    private static ClientWorld previousWorld;
    private static int previousHidden;
    private static final ThreadLocal<BlockState> SMOKE_SOURCE = new ThreadLocal<>();

    private CampfireCulling() {
    }

    public static void tick(MinecraftClient client) {
        int hidden = ModConfig.getHiddenCampfires();
        if (client.world == previousWorld && client.world != null && hidden != previousHidden) {
            client.worldRenderer.reload();
        }
        previousWorld = client.world;
        previousHidden = hidden;
    }

    public static boolean isSoundSuppressed(SoundInstance sound, ClientWorld world) {
        return ModConfig.getHiddenCampfires() != 0 && !sound.isRelative()
                && sound.getId().equals(SoundEvents.BLOCK_CAMPFIRE_CRACKLE.id())
                && ModConfig.isBlockHidden(world.getBlockState(BlockPos.ofFloored(sound.getX(), sound.getY(), sound.getZ())));
    }

    public static void emitSmoke(World world, BlockPos pos, Runnable emission) {
        BlockState state = world.getBlockState(pos);
        if (world.isClient() && ModConfig.isBlockHidden(state)) {
            return;
        }
        BlockState previous = SMOKE_SOURCE.get();
        SMOKE_SOURCE.set(state);
        try {
            emission.run();
        } finally {
            if (previous == null) {
                SMOKE_SOURCE.remove();
            } else {
                SMOKE_SOURCE.set(previous);
            }
        }
    }

    public static BlockState getSmokeSource() {
        return SMOKE_SOURCE.get();
    }
}
