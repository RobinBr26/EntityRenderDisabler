package com.victorgponce.entityrenderdisablerrewritefabric.client.mixin;

import com.victorgponce.entityrenderdisablerrewritefabric.client.culling.CampfireModel;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.block.BlockModels;
import net.minecraft.client.render.model.BlockStateModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(BlockModels.class)
public abstract class BlockModelsMixin {
    @Unique
    private final Map<BlockState, BlockStateModel> entityrenderdisabler$campfireModels = new ConcurrentHashMap<>();

    @Inject(method = "getModel", at = @At("RETURN"), cancellable = true)
    private void entityrenderdisabler$campfireModel(BlockState state, CallbackInfoReturnable<BlockStateModel> cir) {
        if (state.isOf(Blocks.CAMPFIRE) || state.isOf(Blocks.SOUL_CAMPFIRE)) {
            BlockStateModel model = cir.getReturnValue();
            cir.setReturnValue(entityrenderdisabler$campfireModels.computeIfAbsent(state, key -> new CampfireModel(key, model)));
        }
    }

    @Inject(method = "setModels", at = @At("HEAD"))
    private void entityrenderdisabler$reloadModels(Map<BlockState, BlockStateModel> models, CallbackInfo ci) {
        entityrenderdisabler$campfireModels.clear();
    }
}
