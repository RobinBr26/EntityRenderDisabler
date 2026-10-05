package com.victorgponce.entityrenderdisablerrewritefabric.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.victorgponce.entityrenderdisablerrewritefabric.client.culling.CullingEffects;
import com.victorgponce.entityrenderdisablerrewritefabric.client.culling.CullingWorld;
import com.victorgponce.entityrenderdisablerrewritefabric.client.culling.EntityCulling;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientWorld.class)
public abstract class ClientWorldMixin implements CullingWorld {
    @Unique
    private EntityCulling entityrenderdisabler$culling;

    @Override
    public EntityCulling entityrenderdisabler$getCulling() {
        if (entityrenderdisabler$culling == null) {
            entityrenderdisabler$culling = new EntityCulling((ClientWorld) (Object) this);
        }
        return entityrenderdisabler$culling;
    }

    @Inject(method = "addEntity", at = @At("HEAD"), cancellable = true)
    private void entityrenderdisabler$addEntity(Entity entity, CallbackInfo ci) {
        if (entityrenderdisabler$getCulling().interceptSpawn(entity)) {
            ci.cancel();
        }
    }

    @Inject(method = "removeEntity", at = @At("HEAD"))
    private void entityrenderdisabler$removeEntity(int id, Entity.RemovalReason reason, CallbackInfo ci) {
        entityrenderdisabler$getCulling().removeDormant(id, reason);
    }

    @Inject(method = "getEntityById", at = @At("RETURN"), cancellable = true)
    private void entityrenderdisabler$getEntity(int id, CallbackInfoReturnable<Entity> cir) {
        if (cir.getReturnValue() == null) {
            cir.setReturnValue(entityrenderdisabler$getCulling().getDormant(id));
        }
    }

    @Inject(method = "tickEntities", at = @At("HEAD"))
    private void entityrenderdisabler$reconcile(CallbackInfo ci) {
        entityrenderdisabler$getCulling().tick();
    }

    @WrapOperation(method = "tickEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;tick()V"))
    private void entityrenderdisabler$tick(Entity entity, Operation<Void> original) {
        CullingEffects.run(EntityCulling.isHidden(entity), () -> original.call(entity));
    }

    @WrapOperation(method = "tickPassenger", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;tickRiding()V"))
    private void entityrenderdisabler$tickPassenger(Entity entity, Operation<Void> original) {
        if (!entityrenderdisabler$getCulling().isDormant(entity)) {
            CullingEffects.run(EntityCulling.isHidden(entity), () -> original.call(entity));
        }
    }

    @Inject(method = "tickPassenger", at = @At("HEAD"), cancellable = true)
    private void entityrenderdisabler$skipDormantPassenger(Entity vehicle, Entity passenger, CallbackInfo ci) {
        if (entityrenderdisabler$getCulling().isDormant(passenger)) {
            ci.cancel();
        }
    }
}
