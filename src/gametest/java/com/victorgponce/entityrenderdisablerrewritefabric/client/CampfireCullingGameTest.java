package com.victorgponce.entityrenderdisablerrewritefabric.client;

import com.victorgponce.entityrenderdisablerrewritefabric.client.config.ModConfig;
import com.victorgponce.entityrenderdisablerrewritefabric.client.culling.CampfireCulling;
import com.victorgponce.entityrenderdisablerrewritefabric.client.culling.EntitySounds;
import com.victorgponce.entityrenderdisablerrewritefabric.client.mixin.ParticleManagerTestAccessor;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableMesh;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.entity.CampfireBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.CampfireSmokeParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.render.block.entity.BlockEntityRenderManager;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

import java.util.List;
import java.util.Queue;

public final class CampfireCullingGameTest {
    private CampfireCullingGameTest() {
    }

    public static void runTests(MinecraftClient client) {
        BlockPos normalPos = client.player.getBlockPos().add(2, 0, 0);
        BlockPos soulPos = normalPos.add(2, 0, 0);
        BlockState previousNormal = client.world.getBlockState(normalPos);
        BlockState previousSoul = client.world.getBlockState(soulPos);
        boolean normalVisible = ModConfig.isBlockVisible("minecraft:campfire");
        boolean soulVisible = ModConfig.isBlockVisible("minecraft:soul_campfire");
        try {
            ModConfig.setBlockVisible("minecraft:campfire", true);
            ModConfig.setBlockVisible("minecraft:soul_campfire", true);
            CampfireCulling.tick(client);
            client.world.setBlockState(normalPos, Blocks.CAMPFIRE.getDefaultState(), Block.NOTIFY_ALL);
            client.world.setBlockState(soulPos, Blocks.SOUL_CAMPFIRE.getDefaultState(), Block.NOTIFY_ALL);
            CampfireBlockEntity normal = (CampfireBlockEntity) client.world.getBlockEntity(normalPos);
            CampfireBlockEntity soul = (CampfireBlockEntity) client.world.getBlockEntity(soulPos);
            check(normal != null && soul != null, "Campfire block entities did not load");
            normal.getItemsBeingCooked().set(0, new ItemStack(Items.BEEF, 1));
            BlockEntityRenderManager renders = client.getBlockEntityRenderDispatcher();
            renders.configure(client.gameRenderer.getCamera());
            check(renders.getRenderState(normal, 0, null) != null, "Visible campfire item render state was missing");
            check(geometry(client, normalPos) > 0 && geometry(client, soulPos) > 0, "Visible campfire geometry was missing");
            BlockStateModel cached = client.getBlockRenderManager().getModel(normal.getCachedState());
            Object visibleKey = cached.createGeometryKey(client.world, normalPos, normal.getCachedState(), Random.create(1));
            Particle smoke = spawnSmoke(client, normalPos);
            Particle soulSmoke = spawnSmoke(client, soulPos);
            Particle independent = client.particleManager.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    normalPos.getX() + 0.5, normalPos.getY() + 0.5, normalPos.getZ() + 0.5, 0, 0, 0);

            WorldRendererTestAccess renderer = (WorldRendererTestAccess) client.worldRenderer;
            int reloads = renderer.entityrenderdisabler$getReloads();
            ModConfig.setBlockVisible("minecraft:campfire", false);
            CampfireCulling.tick(client);
            check(renderer.entityrenderdisabler$getReloads() == reloads + 1, "Campfire visibility did not rebuild world render data");
            CampfireCulling.tick(client);
            check(renderer.entityrenderdisabler$getReloads() == reloads + 1, "World render data rebuilt without a visibility change");
            check(geometry(client, normalPos) == 0 && geometry(client, soulPos) > 0, "Campfire types were not independently hidden");
            check(cached.getParts(Random.create(1)).isEmpty(), "Cached vanilla campfire model still emitted geometry");
            Object hiddenKey = cached.createGeometryKey(client.world, normalPos, normal.getCachedState(), Random.create(1));
            check(hiddenKey != null && !hiddenKey.equals(visibleKey), "Geometry cache key did not change with visibility");
            check(renders.getRenderState(normal, 0, null) == null, "Hidden campfire still built cooking-item render states");
            check(renders.getRenderState(soul, 0, null) != null, "Visible soul campfire item renderer was suppressed");
            check(client.world.getBlockEntity(normalPos) == normal && !normal.getItemsBeingCooked().getFirst().isEmpty(), "Campfire inventory or block entity was removed");
            check(client.world.getBlockState(normalPos).isOf(Blocks.CAMPFIRE), "Campfire was removed from client world data");
            check(!normal.getCachedState().getCollisionShape(client.world, normalPos).isEmpty(), "Campfire collision shape was lost");
            check(normal.getCachedState().getLuminance() > 0, "Campfire lighting was changed");
            smoke.tick();
            soulSmoke.tick();
            independent.tick();
            check(!smoke.isAlive(), "Existing smoke from a hidden campfire was not culled");
            check(soulSmoke.isAlive() && independent.isAlive(), "Unrelated smoke was culled");
            testEffects(client, normalPos, normal);
            PositionedSoundInstance normalCrackle = crackle(normalPos);
            PositionedSoundInstance soulCrackle = crackle(soulPos);
            check(EntitySounds.isSuppressed(normalCrackle) && !EntitySounds.isSuppressed(soulCrackle), "Campfire crackling did not respect the selected type");
            check(!EntitySounds.isSuppressed(new PositionedSoundInstance(SoundEvents.BLOCK_STONE_BREAK, SoundCategory.BLOCKS,
                    1, 1, Random.create(1), normalPos.getX() + 0.5, normalPos.getY() + 0.5, normalPos.getZ() + 0.5)), "Unrelated block sound was muted");

            ModConfig.setBlockVisible("minecraft:soul_campfire", false);
            check(geometry(client, soulPos) == 0 && EntitySounds.isSuppressed(soulCrackle), "Soul campfire was not hidden");
            ModConfig.setModEnabled(false);
            CampfireCulling.tick(client);
            check(geometry(client, normalPos) > 0 && geometry(client, soulPos) > 0, "Global toggle did not restore campfire geometry");
            check(renders.getRenderState(normal, 0, null) != null && !EntitySounds.isSuppressed(normalCrackle), "Global toggle did not restore campfire effects");
            ModConfig.setModEnabled(true);
            CampfireCulling.tick(client);
            check(geometry(client, normalPos) == 0 && geometry(client, soulPos) == 0, "Re-enabling suppression did not restore the campfire choices");
            ModConfig.setBlockVisible("minecraft:campfire", true);
            ModConfig.setBlockVisible("minecraft:soul_campfire", true);
            CampfireCulling.tick(client);
            check(geometry(client, normalPos) > 0 && renders.getRenderState(normal, 0, null) != null, "Campfire restoration failed");
            check(!cached.getParts(Random.create(1)).isEmpty(), "Cached model did not restore geometry");
            check(cached.particleSprite() != null, "Campfire particle sprite was lost");
        } finally {
            client.world.setBlockState(normalPos, previousNormal, Block.NOTIFY_ALL);
            client.world.setBlockState(soulPos, previousSoul, Block.NOTIFY_ALL);
            ModConfig.setModEnabled(true);
            ModConfig.setBlockVisible("minecraft:campfire", normalVisible);
            ModConfig.setBlockVisible("minecraft:soul_campfire", soulVisible);
            CampfireCulling.tick(client);
        }
        EntityrenderdisablerrewritefabricClient.LOGGER.info("Campfire tests passed: separate visibility, geometry, cooking items, smoke, sounds and restoration");
    }

    private static void testEffects(MinecraftClient client, BlockPos pos, CampfireBlockEntity campfire) {
        Queue<Particle> queued = ((ParticleManagerTestAccessor) client.particleManager).entityrenderdisabler$getNewParticles();
        int count = queued.size();
        for (int i = 0; i < 100; i++) {
            CampfireBlock.spawnSmokeParticle(client.world, pos, true, true);
            CampfireBlockEntity.clientTick(client.world, pos, campfire.getCachedState(), campfire);
            campfire.getCachedState().getBlock().randomDisplayTick(campfire.getCachedState(), client.world, pos, Random.create(i));
        }
        check(queued.size() == count, "Hidden campfire continued generating particles");
        check(CampfireCulling.getSmokeSource() == null, "Campfire particle scope leaked");
    }

    private static Particle spawnSmoke(MinecraftClient client, BlockPos pos) {
        Queue<Particle> particles = ((ParticleManagerTestAccessor) client.particleManager).entityrenderdisabler$getNewParticles();
        int count = particles.size();
        CampfireBlock.spawnSmokeParticle(client.world, pos, true, false);
        check(particles.size() > count, "Visible campfire failed to emit smoke");
        Particle particle = List.copyOf(particles).getLast();
        check(particle instanceof CampfireSmokeParticle, "Campfire did not produce a smoke particle");
        return particle;
    }

    private static int geometry(MinecraftClient client, BlockPos pos) {
        BlockState state = client.world.getBlockState(pos);
        MutableMesh mesh = Renderer.get().mutableMesh();
        client.getBlockRenderManager().getModel(state).emitQuads(mesh.emitter(), client.world, pos, state, Random.create(1), direction -> false);
        return mesh.size();
    }

    private static PositionedSoundInstance crackle(BlockPos pos) {
        return new PositionedSoundInstance(SoundEvents.BLOCK_CAMPFIRE_CRACKLE, SoundCategory.BLOCKS,
                1, 1, Random.create(1), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
