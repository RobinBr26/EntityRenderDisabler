package com.victorgponce.entityrenderdisablerrewritefabric.client.mixin;

import com.victorgponce.entityrenderdisablerrewritefabric.client.config.ModConfig;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.CampfireBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CampfireBlockEntity.class)
public abstract class CampfireBlockEntityMixin {
    @Inject(method = "clientTick", at = @At("HEAD"), cancellable = true)
    private static void entityrenderdisabler$clientTick(World world, BlockPos pos, BlockState state, CampfireBlockEntity campfire, CallbackInfo ci) {
        if (world.isClient() && ModConfig.isBlockHidden(state)) {
            ci.cancel();
        }
    }
}
