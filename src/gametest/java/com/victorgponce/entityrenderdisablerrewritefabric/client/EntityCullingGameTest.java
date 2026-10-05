package com.victorgponce.entityrenderdisablerrewritefabric.client;

import com.victorgponce.entityrenderdisablerrewritefabric.client.config.ModConfig;
import com.victorgponce.entityrenderdisablerrewritefabric.client.config.YACLIntegration;
import com.victorgponce.entityrenderdisablerrewritefabric.client.culling.CullingEffects;
import com.victorgponce.entityrenderdisablerrewritefabric.client.culling.EntityCulling;
import com.victorgponce.entityrenderdisablerrewritefabric.client.culling.EntitySounds;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.EntityTrackingSoundInstance;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.client.sound.MovingMinecartSoundInstance;
import net.minecraft.client.sound.EntityRidingSoundInstance;
import net.minecraft.client.sound.PassiveBeeSoundInstance;
import net.minecraft.client.sound.GuardianAttackSoundInstance;
import net.minecraft.client.sound.SnifferDigSoundInstance;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.entity.passive.SnifferEntity;
import net.minecraft.entity.passive.DonkeyEntity;
import net.minecraft.entity.mob.GuardianEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.ExperimentalMinecartController;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.MoveMinecartAlongTrackS2CPacket;
import net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPassengersSetS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionSyncS2CPacket;
import net.minecraft.entity.EntityPosition;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.List;

public class EntityCullingGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        try (var singleplayer = context.worldBuilder().create()) {
            singleplayer.getClientWorld().waitForChunksDownload();
            context.runOnClient(this::testLifecycle);
            context.runOnClient(this::testRidingAndCamera);
            context.runOnClient(this::testSoundsAndEffects);
            context.runOnClient(this::testSpecialSoundsAndPackets);
            context.runOnClient(this::testPopulation);
            context.runOnClient(client -> check(YACLIntegration.createConfigScreen(null) != null, "Configuration screen failed"));
        }
        try (var singleplayer = context.worldBuilder().create()) {
            context.runOnClient(client -> check(EntityCulling.get(client.world).getDormantCount() == 0, "Dormant entities leaked into a new world"));
        }
        EntityrenderdisablerrewritefabricClient.LOGGER.info("Entity culling integration tests passed");
    }

    private void testLifecycle(MinecraftClient client) {
        ClientWorld world = client.world;
        EntityCulling culling = EntityCulling.get(world);
        ModConfig.setEntityVisible("minecraft:zombie", true);
        ModConfig.setModEnabled(true);
        ZombieEntity zombie = zombie(world, 2_000_000_001, client.player.getEntityPos().add(3, 0, 0));
        world.addEntity(zombie);
        check(contains(world, zombie), "Visible spawn was dropped");
        zombie.updateTrackedPositionAndAngles(zombie.getEntityPos().add(1, 0, 0), 90, 0);
        ModConfig.setEntityVisible("minecraft:zombie", false);
        culling.tick();
        check(culling.isDormant(zombie), "Existing entity was not suspended");
        check(!zombie.isRemoved(), "Suspension marked an entity as destroyed");
        check(!contains(world, zombie) && !world.hasEntity(zombie), "Suspended entity remained active");
        check(world.getEntityById(zombie.getId()) == zombie, "Network lookup lost the suspended entity");
        check(!world.getOtherEntities(null, zombie.getBoundingBox().expand(1)).contains(zombie), "Suspended entity remained in spatial queries");
        check(!zombie.isInterpolating(), "Old interpolation was not cleared");
        int age = zombie.age;
        world.tickEntities();
        world.tickEntities();
        check(zombie.age == age, "Suspended entity still ticked");
        Vec3d target = client.player.getEntityPos().add(48, 0, 0);
        client.getNetworkHandler().onEntityPositionSync(new EntityPositionSyncS2CPacket(zombie.getId(), new EntityPosition(target, Vec3d.ZERO, 45, 15), true));
        check(zombie.getEntityPos().equals(target), "Server position update was lost");
        zombie.updateTrackedPositionAndAngles(target.add(2, 0, 0), 80, 20);
        check(zombie.getX() == target.x + 2 && zombie.getYaw() == 80, "Dormant relative movement did not apply immediately");
        check(culling.hasSuppressedSource(zombie.getX(), zombie.getY(), zombie.getZ(), EntityType.ZOMBIE), "Sound index did not follow movement");
        ModConfig.setModEnabled(false);
        culling.tick();
        check(contains(world, zombie) && world.getEntityById(zombie.getId()) == zombie, "Toggle failed to restore the same entity");
        check(culling.getDormantCount() == 0, "Restored entity remained dormant");
        ModConfig.setModEnabled(true);
        culling.tick();
        ZombieEntity replacement = zombie(world, zombie.getId(), target);
        world.addEntity(replacement);
        check(zombie.isRemoved() && world.getEntityById(zombie.getId()) == replacement, "Reused entity ID retained stale state");
        world.removeEntity(replacement.getId(), Entity.RemovalReason.DISCARDED);
        check(world.getEntityById(replacement.getId()) == null && culling.getDormantCount() == 0, "Destroyed dormant entity was retained");
        ModConfig.setEntityVisible("minecraft:zombie", true);
    }

    private void testRidingAndCamera(MinecraftClient client) {
        ClientWorld world = client.world;
        EntityCulling culling = EntityCulling.get(world);
        ModConfig.setEntityVisible("minecraft:pig", false);
        PigEntity vehicle = new PigEntity(EntityType.PIG, world);
        vehicle.setId(2_000_000_010);
        vehicle.refreshPositionAndAngles(client.player.getEntityPos().add(3, 0, 0), 0, 0);
        world.addEntity(vehicle);
        check(culling.isDormant(vehicle), "Hidden vehicle spawn was active");
        check(client.player.startRiding(vehicle, true, false), "Could not attach local player");
        client.getNetworkHandler().onEntityPassengersSet(new EntityPassengersSetS2CPacket(vehicle));
        check(!culling.isDormant(vehicle) && contains(world, vehicle), "Local vehicle was not restored");
        check(client.player.getVehicle() == vehicle && EntityCulling.isHidden(vehicle), "Riding state or vehicle suppression was lost");
        ModConfig.setEntityVisible("minecraft:player", false);
        check(!EntityCulling.isHidden(client.player), "Local player was suppressed");
        client.player.stopRiding();
        culling.tick();
        check(culling.isDormant(vehicle), "Unoccupied hidden vehicle remained active");
        client.setCameraEntity(vehicle);
        culling.tick();
        check(!culling.isDormant(vehicle) && !EntityCulling.isHidden(vehicle), "Camera entity was suppressed");
        client.setCameraEntity(client.player);
        culling.tick();
        check(culling.isDormant(vehicle), "Former camera entity remained active");
        ModConfig.setEntityVisible("minecraft:pig", true);
        ModConfig.setEntityVisible("minecraft:player", true);
        culling.tick();
        world.removeEntity(vehicle.getId(), Entity.RemovalReason.DISCARDED);
    }

    private void testSoundsAndEffects(MinecraftClient client) {
        ClientWorld world = client.world;
        ModConfig.setEntityVisible("minecraft:zombie", false);
        ZombieEntity zombie = zombie(world, 2_000_000_020, client.player.getEntityPos().add(3, 0, 0));
        world.addEntity(zombie);
        EntityTrackingSoundInstance bound = new EntityTrackingSoundInstance(SoundEvents.ENTITY_ZOMBIE_AMBIENT, SoundCategory.HOSTILE, 1, 1, zombie, 1);
        PositionedSoundInstance positional = new PositionedSoundInstance(SoundEvents.ENTITY_ZOMBIE_AMBIENT, SoundCategory.HOSTILE, 1, 1, Random.create(1), zombie.getX(), zombie.getY(), zombie.getZ());
        PositionedSoundInstance unrelated = new PositionedSoundInstance(SoundEvents.BLOCK_STONE_BREAK, SoundCategory.BLOCKS, 1, 1, Random.create(1), zombie.getX(), zombie.getY(), zombie.getZ());
        check(EntitySounds.isSuppressed(bound) && EntitySounds.isSuppressed(positional), "Entity sounds were not suppressed");
        check(!EntitySounds.isSuppressed(unrelated), "Unrelated block sound was suppressed");
        PositionedSoundInstance distant = new PositionedSoundInstance(SoundEvents.ENTITY_ZOMBIE_AMBIENT, SoundCategory.HOSTILE, 1, 1, Random.create(1), zombie.getX() + 100, zombie.getY(), zombie.getZ());
        check(!EntitySounds.isSuppressed(distant), "Entity sound without a suppressed source was muted");
        check(client.getSoundManager().play(bound) == SoundSystem.PlayResult.NOT_STARTED, "Hidden sound reached the audio engine");
        CullingEffects.run(true, () -> {
            check(EntitySounds.isSuppressed(unrelated), "Entity effect scope did not suppress sounds");
            check(client.particleManager.addParticle(ParticleTypes.FLAME, zombie.getX(), zombie.getY(), zombie.getZ(), 0, 0, 0) == null, "Entity effect scope did not suppress particles");
        });
        try {
            CullingEffects.run(true, () -> { throw new IllegalStateException("test"); });
        } catch (IllegalStateException expected) {
            check(!CullingEffects.isSuppressed(), "Effect suppression leaked after an exception");
        }
        ModConfig.setEntityVisible("minecraft:zombie", true);
        EntityCulling.get(world).tick();
        check(!EntitySounds.isSuppressed(bound) && !EntitySounds.isSuppressed(positional), "Entity sounds did not recover after toggling");
        world.removeEntity(zombie.getId(), Entity.RemovalReason.DISCARDED);
    }

    private void testPopulation(MinecraftClient client) {
        ClientWorld world = client.world;
        EntityCulling culling = EntityCulling.get(world);
        ModConfig.setEntityVisible("minecraft:zombie", false);
        List<ZombieEntity> entities = new ArrayList<>();
        for (int i = 0; i < 5000; i++) {
            ZombieEntity zombie = zombie(world, 1_900_000_000 + i, client.player.getEntityPos().add(3, 0, 0));
            world.addEntity(zombie);
            entities.add(zombie);
        }
        check(culling.getDormantCount() == 5000, "Mass spawn did not retain the correct population");
        for (Entity entity : world.getEntities()) {
            check(entity.getType() != EntityType.ZOMBIE, "Hidden population remained in the render iteration");
        }
        culling.tick();
        world.tickEntities();
        check(entities.stream().allMatch(entity -> entity.age == 0), "Hidden population still ticked");
        ModConfig.setEntityVisible("minecraft:zombie", true);
        culling.tick();
        check(culling.getDormantCount() == 0 && entities.stream().allMatch(entity -> contains(world, entity)), "Mass restore lost entities");
        for (ZombieEntity zombie : entities) {
            world.removeEntity(zombie.getId(), Entity.RemovalReason.DISCARDED);
        }
    }

    private void testSpecialSoundsAndPackets(MinecraftClient client) {
        ClientWorld world = client.world;
        EntityCulling culling = EntityCulling.get(world);
        ModConfig.setEntityVisible("minecraft:chest_minecart", false);
        AbstractMinecartEntity minecart = (AbstractMinecartEntity) EntityType.CHEST_MINECART.create(world, SpawnReason.LOAD);
        minecart.setId(2_000_000_030);
        minecart.refreshPositionAndAngles(client.player.getEntityPos().add(3, 0, 0), 0, 0);
        world.addEntity(minecart);
        check(EntitySounds.isSuppressed(new MovingMinecartSoundInstance(minecart)), "Minecart subtype sound was not suppressed");
        check(EntitySounds.isSuppressed(new EntityRidingSoundInstance(client.player, minecart, false, SoundEvents.ENTITY_MINECART_RIDING, SoundCategory.NEUTRAL, 0, 1, 1)), "Riding sound was not suppressed");
        Vec3d position = minecart.getEntityPos().add(30, 0, 0);
        var step = new ExperimentalMinecartController.Step(position, Vec3d.ZERO, 45, 5, 1);
        for (int i = 0; i < 100; i++) {
            client.getNetworkHandler().onMoveMinecartAlongTrack(new MoveMinecartAlongTrackS2CPacket(minecart.getId(), List.of(step)));
        }
        check(minecart.getEntityPos().equals(position), "Dormant minecart lost its latest movement step");
        if (minecart.getController() instanceof ExperimentalMinecartController controller) {
            check(controller.stagingLerpSteps.isEmpty(), "Dormant minecart accumulated movement steps");
        }
        world.removeEntity(minecart.getId(), Entity.RemovalReason.DISCARDED);
        ModConfig.setEntityVisible("minecraft:chest_minecart", true);

        ModConfig.setEntityVisible("minecraft:bee", false);
        BeeEntity bee = new BeeEntity(EntityType.BEE, world);
        check(EntitySounds.isSuppressed(new PassiveBeeSoundInstance(bee)), "Bee loop was not suppressed");
        ModConfig.setEntityVisible("minecraft:bee", true);
        check(!EntitySounds.isSuppressed(new PassiveBeeSoundInstance(bee)), "Visible bee loop was suppressed");

        ModConfig.setEntityVisible("minecraft:elder_guardian", false);
        GuardianEntity guardian = (GuardianEntity) EntityType.ELDER_GUARDIAN.create(world, SpawnReason.LOAD);
        check(EntitySounds.isSuppressed(new GuardianAttackSoundInstance(guardian)), "Guardian subtype loop was not suppressed");
        ModConfig.setEntityVisible("minecraft:elder_guardian", true);

        ModConfig.setEntityVisible("minecraft:sniffer", false);
        SnifferEntity sniffer = new SnifferEntity(EntityType.SNIFFER, world);
        check(EntitySounds.isSuppressed(new SnifferDigSoundInstance(sniffer)), "Sniffer sound was not suppressed");
        ModConfig.setEntityVisible("minecraft:sniffer", true);

        ModConfig.setEntityVisible("minecraft:donkey", false);
        DonkeyEntity donkey = new DonkeyEntity(EntityType.DONKEY, world);
        donkey.setId(2_000_000_032);
        donkey.refreshPositionAndAngles(client.player.getEntityPos().add(6, 0, 0), 0, 0);
        world.addEntity(donkey);
        PositionedSoundInstance shared = new PositionedSoundInstance(SoundEvents.ENTITY_HORSE_EAT, SoundCategory.NEUTRAL, 1, 1, Random.create(1), donkey.getX(), donkey.getY(), donkey.getZ());
        check(EntitySounds.isSuppressed(shared), "Shared horse sound from a hidden donkey was audible");
        ModConfig.setEntityVisible("minecraft:donkey", true);
        culling.tick();
        check(!EntitySounds.isSuppressed(shared), "Shared horse sound from a visible donkey was muted");
        world.removeEntity(donkey.getId(), Entity.RemovalReason.DISCARDED);

        ModConfig.setEntityVisible("minecraft:item", false);
        ItemEntity item = new ItemEntity(world, client.player.getX() + 3, client.player.getY(), client.player.getZ(), new ItemStack(Items.APPLE, 3));
        item.setId(2_000_000_031);
        world.addEntity(item);
        client.getNetworkHandler().onItemPickupAnimation(new ItemPickupAnimationS2CPacket(item.getId(), client.player.getId(), 1));
        check(item.getStack().getCount() == 2 && culling.isDormant(item), "Hidden item partial pickup lost its stack state");
        client.getNetworkHandler().onItemPickupAnimation(new ItemPickupAnimationS2CPacket(item.getId(), client.player.getId(), 2));
        check(world.getEntityById(item.getId()) == null, "Empty hidden pickup remained in the client");
        ModConfig.setEntityVisible("minecraft:item", true);
    }

    private static ZombieEntity zombie(ClientWorld world, int id, Vec3d pos) {
        ZombieEntity entity = new ZombieEntity(EntityType.ZOMBIE, world);
        entity.setId(id);
        entity.refreshPositionAndAngles(pos, 0, 0);
        entity.getTrackedPosition().setPos(pos);
        return entity;
    }

    private static boolean contains(ClientWorld world, Entity entity) {
        for (Entity candidate : world.getEntities()) {
            if (candidate == entity) {
                return true;
            }
        }
        return false;
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
