package com.victorgponce.entityrenderdisablerrewritefabric.client.mixin;

import com.victorgponce.entityrenderdisablerrewritefabric.client.config.ModConfig;
import com.victorgponce.entityrenderdisablerrewritefabric.client.culling.CampfireCulling;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.block.BlockState;
import net.minecraft.block.CampfireBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CampfireBlock.class)
public abstract class CampfireBlockMixin {
    @Inject(method = "randomDisplayTick", at = @At("HEAD"), cancellable = true)
    private void entityrenderdisabler$displayTick(BlockState state, World world, BlockPos pos, Random random, CallbackInfo ci) {
        if (world.isClient() && ModConfig.isBlockHidden(state)) {
            ci.cancel();
        }
    }

    @WrapMethod(method = "spawnSmokeParticle")
    private static void entityrenderdisabler$smoke(World world, BlockPos pos, boolean signalFire, boolean lotsOfSmoke, Operation<Void> original) {
        CampfireCulling.emitSmoke(world, pos, () -> original.call(world, pos, signalFire, lotsOfSmoke));
    }
}
