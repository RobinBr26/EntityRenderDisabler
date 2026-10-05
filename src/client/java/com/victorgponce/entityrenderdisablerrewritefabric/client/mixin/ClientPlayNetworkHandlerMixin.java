package com.victorgponce.entityrenderdisablerrewritefabric.client.mixin;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.victorgponce.entityrenderdisablerrewritefabric.client.culling.EntityCulling;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.network.packet.s2c.play.EntityPassengersSetS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket;
import net.minecraft.network.packet.s2c.play.MoveMinecartAlongTrackS2CPacket;
import net.minecraft.entity.vehicle.ExperimentalMinecartController;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
    @Shadow
    private ClientWorld world;

    @WrapWithCondition(method = "onEntitySpawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;playSpawnSound(Lnet/minecraft/entity/Entity;)V"))
    private boolean entityrenderdisabler$spawnSound(ClientPlayNetworkHandler handler, Entity entity) {
        return !EntityCulling.isHidden(entity);
    }

    @WrapOperation(method = "onEntityStatus", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/s2c/play/EntityStatusS2CPacket;getEntity(Lnet/minecraft/world/World;)Lnet/minecraft/entity/Entity;"))
    private Entity entityrenderdisabler$status(EntityStatusS2CPacket packet, World world, Operation<Entity> original) {
        Entity entity = original.call(packet, world);
        return EntityCulling.isHidden(entity) ? null : entity;
    }

    @WrapOperation(method = {"onEntityDamage", "onDamageTilt"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientWorld;getEntityById(I)Lnet/minecraft/entity/Entity;"))
    private Entity entityrenderdisabler$damage(ClientWorld world, int id, Operation<Entity> original) {
        Entity entity = original.call(world, id);
        return EntityCulling.isHidden(entity) ? null : entity;
    }

    @Inject(method = "onItemPickupAnimation", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/network/PacketApplyBatcher;)V", shift = At.Shift.AFTER), cancellable = true)
    private void entityrenderdisabler$pickup(ItemPickupAnimationS2CPacket packet, CallbackInfo ci) {
        Entity entity = world.getEntityById(packet.getEntityId());
        if (!EntityCulling.isHidden(entity)) {
            return;
        }
        if (entity instanceof ItemEntity item) {
            item.getStack().decrement(packet.getStackAmount());
            if (item.getStack().isEmpty()) {
                world.removeEntity(entity.getId(), Entity.RemovalReason.DISCARDED);
            }
        } else if (!(entity instanceof ExperienceOrbEntity)) {
            world.removeEntity(entity.getId(), Entity.RemovalReason.DISCARDED);
        }
        ci.cancel();
    }

    @Inject(method = "onEntityPassengersSet", at = @At("RETURN"))
    private void entityrenderdisabler$passengers(EntityPassengersSetS2CPacket packet, CallbackInfo ci) {
        EntityCulling.get(world).refreshRidingGroup(world.getEntityById(packet.getEntityId()));
    }

    @WrapOperation(method = "onMoveMinecartAlongTrack", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/s2c/play/MoveMinecartAlongTrackS2CPacket;getEntity(Lnet/minecraft/world/World;)Lnet/minecraft/entity/Entity;"))
    private Entity entityrenderdisabler$minecart(MoveMinecartAlongTrackS2CPacket packet, World world, Operation<Entity> original) {
        Entity entity = original.call(packet, world);
        if (entity != null && EntityCulling.get((ClientWorld) world).isDormant(entity)) {
            if (!packet.lerpSteps().isEmpty()) {
                ExperimentalMinecartController.Step step = packet.lerpSteps().getLast();
                entity.refreshPositionAndAngles(step.position(), step.yRot(), step.xRot());
                entity.setVelocity(step.movement());
            }
            return null;
        }
        return entity;
    }
}
