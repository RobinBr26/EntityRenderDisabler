package com.victorgponce.entityrenderdisablerrewritefabric.client.mixin;

import com.victorgponce.entityrenderdisablerrewritefabric.client.culling.EntityBoundSound;
import net.minecraft.client.sound.GuardianAttackSoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.GuardianEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(GuardianAttackSoundInstance.class)
public abstract class GuardianSoundMixin implements EntityBoundSound {
    @Shadow @Final
    private GuardianEntity guardian;

    @Override
    public Entity entityrenderdisabler$getEntity() {
        return guardian;
    }
}
