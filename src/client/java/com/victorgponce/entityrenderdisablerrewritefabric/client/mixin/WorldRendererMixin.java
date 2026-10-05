package com.victorgponce.entityrenderdisablerrewritefabric.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.victorgponce.entityrenderdisablerrewritefabric.client.culling.EntityCulling;
import com.victorgponce.entityrenderdisablerrewritefabric.client.config.ModConfig;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Iterator;
import java.util.NoSuchElementException;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
    @WrapOperation(method = "fillEntityRenderStates", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientWorld;getEntities()Ljava/lang/Iterable;"))
    private Iterable<Entity> entityrenderdisabler$visibleEntities(ClientWorld world, Operation<Iterable<Entity>> original) {
        Iterable<Entity> entities = original.call(world);
        if (!ModConfig.hasHiddenTypes()) {
            return entities;
        }
        return () -> new Iterator<>() {
            private final Iterator<Entity> iterator = entities.iterator();
            private Entity next;

            @Override
            public boolean hasNext() {
                while (next == null && iterator.hasNext()) {
                    Entity candidate = iterator.next();
                    if (!EntityCulling.isHidden(candidate)) {
                        next = candidate;
                    }
                }
                return next != null;
            }

            @Override
            public Entity next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                Entity result = next;
                next = null;
                return result;
            }
        };
    }
}
