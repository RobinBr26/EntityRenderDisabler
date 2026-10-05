package com.victorgponce.entityrenderdisablerrewritefabric.client.mixin;

import com.victorgponce.entityrenderdisablerrewritefabric.client.culling.EntityCulling;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "updateTrackedPositionAndAngles(Ljava/util/Optional;Ljava/util/Optional;Ljava/util/Optional;)V", at = @At("HEAD"), cancellable = true)
    private void entityrenderdisabler$updateDormantPosition(Optional<Vec3d> position, Optional<Float> yaw, Optional<Float> pitch, CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if (entity.getEntityWorld() instanceof ClientWorld world && EntityCulling.get(world).isDormant(entity)) {
            position.ifPresent(entity::setPosition);
            yaw.ifPresent(value -> entity.setYaw(value % 360.0F));
            pitch.ifPresent(value -> entity.setPitch(value % 360.0F));
            entity.resetPosition();
            ci.cancel();
        }
    }
}
