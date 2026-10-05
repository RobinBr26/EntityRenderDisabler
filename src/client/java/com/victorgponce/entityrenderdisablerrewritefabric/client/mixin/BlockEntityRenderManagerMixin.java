package com.victorgponce.entityrenderdisablerrewritefabric.client.mixin;

import com.victorgponce.entityrenderdisablerrewritefabric.client.config.ModConfig;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.CampfireBlockEntity;
import net.minecraft.client.render.block.entity.BlockEntityRenderManager;
import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;
import net.minecraft.client.render.command.ModelCommandRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockEntityRenderManager.class)
public abstract class BlockEntityRenderManagerMixin {
    @Inject(method = "getRenderState", at = @At("HEAD"), cancellable = true)
    private void entityrenderdisabler$campfireItems(BlockEntity entity, float tickProgress,
                                                   ModelCommandRenderer.CrumblingOverlayCommand overlay,
                                                   CallbackInfoReturnable<BlockEntityRenderState> cir) {
        if (entity instanceof CampfireBlockEntity && ModConfig.isBlockHidden(entity.getCachedState())) {
            cir.setReturnValue(null);
        }
    }
}
