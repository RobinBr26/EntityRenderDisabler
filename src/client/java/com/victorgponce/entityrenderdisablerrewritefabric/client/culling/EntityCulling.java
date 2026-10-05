package com.victorgponce.entityrenderdisablerrewritefabric.client.culling;

import com.victorgponce.entityrenderdisablerrewritefabric.client.config.ModConfig;
import com.victorgponce.entityrenderdisablerrewritefabric.client.mixin.EntityAccessor;
import com.victorgponce.entityrenderdisablerrewritefabric.client.mixin.ClientPlayNetworkHandlerAccessor;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.PositionInterpolator;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.ExperimentalMinecartController;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.entity.EntityChangeListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EntityCulling {
    private final ClientWorld world;
    private final Int2ObjectOpenHashMap<Entity> dormant = new Int2ObjectOpenHashMap<>();
    private final Long2ObjectOpenHashMap<Set<Entity>> sections = new Long2ObjectOpenHashMap<>();
    private final Set<Entity> retainedVehicles = Collections.newSetFromMap(new IdentityHashMap<>());
    private long revision = -1;
    private Entity detaching;
    private boolean restoring;
    private Entity previousCamera;

    public EntityCulling(ClientWorld world) {
        this.world = world;
    }

    public static EntityCulling get(ClientWorld world) {
        return ((CullingWorld) world).entityrenderdisabler$getCulling();
    }

    public static boolean isHidden(Entity entity) {
        if (entity == null || !ModConfig.isTypeHidden(entity.getType()) || !(entity.getEntityWorld() instanceof ClientWorld)) {
            return false;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        return entity != client.player && entity != client.getCameraEntity();
    }

    public Entity getDormant(int id) {
        return dormant.get(id);
    }

    public int getDormantCount() {
        return dormant.size();
    }

    public long getAppliedRevision() {
        return revision;
    }

    public boolean isDormant(Entity entity) {
        return dormant.get(entity.getId()) == entity;
    }

    public boolean isDetaching(Entity entity) {
        return detaching == entity;
    }

    public boolean interceptSpawn(Entity entity) {
        if (restoring) {
            return false;
        }
        removeDormant(entity.getId(), Entity.RemovalReason.DISCARDED);
        if (!isHidden(entity) || hasVisiblePassenger(entity)) {
            return false;
        }
        world.removeEntity(entity.getId(), Entity.RemovalReason.DISCARDED);
        retain(entity);
        return true;
    }

    public boolean removeDormant(int id, Entity.RemovalReason reason) {
        retainedVehicles.removeIf(entity -> entity.getId() == id);
        Entity entity = dormant.get(id);
        if (entity == null) {
            return false;
        }
        entity.setRemoved(reason);
        entity.onRemoved();
        return true;
    }

    public void tick() {
        Entity camera = MinecraftClient.getInstance().getCameraEntity();
        if (revision != ModConfig.getRevision() || previousCamera != camera) {
            reconcile();
            revision = ModConfig.getRevision();
            previousCamera = camera;
        }
        if (!retainedVehicles.isEmpty()) {
            for (Entity entity : List.copyOf(retainedVehicles)) {
                reconcileEntity(entity);
            }
        }
    }

    public void refreshRidingGroup(Entity entity) {
        if (entity == null) {
            return;
        }
        Entity root = entity.getRootVehicle();
        List<Entity> group = root.streamSelfAndPassengers().toList();
        for (Entity member : group) {
            reconcileEntity(member);
        }
    }

    public void reconcile() {
        List<Entity> entities = new ArrayList<>(dormant.values());
        world.getEntities().forEach(entities::add);
        for (Entity entity : entities) {
            reconcileEntity(entity);
        }
    }

    private void reconcileEntity(Entity entity) {
        if (entity.isRemoved()) {
            retainedVehicles.remove(entity);
            return;
        }
        boolean hidden = isHidden(entity);
        boolean active = !hidden || hasVisiblePassenger(entity);
        if (active && isDormant(entity)) {
            restore(entity);
        } else if (!active && !isDormant(entity)) {
            suspend(entity);
        }
        if (hidden && active) {
            retainedVehicles.add(entity);
        } else {
            retainedVehicles.remove(entity);
        }
    }

    private boolean hasVisiblePassenger(Entity entity) {
        for (Entity passenger : entity.getPassengersDeep()) {
            if (!isHidden(passenger)) {
                return true;
            }
        }
        return false;
    }

    private void suspend(Entity entity) {
        detaching = entity;
        try {
            ((EntityAccessor) entity).entityrenderdisabler$getChangeListener().remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
        } finally {
            detaching = null;
        }
        retain(entity);
    }

    private void retain(Entity entity) {
        dormant.put(entity.getId(), entity);
        retainedVehicles.remove(entity);
        DormantListener listener = new DormantListener(entity);
        entity.setChangeListener(listener);
        listener.updateEntityPosition();
        PositionInterpolator interpolator = entity.getInterpolator();
        if (interpolator != null) {
            entity.refreshPositionAndAngles(interpolator.getLerpedPos(), interpolator.getLerpedYaw(), interpolator.getLerpedPitch());
            interpolator.clear();
        }
        if (entity instanceof AbstractMinecartEntity minecart
                && minecart.getController() instanceof ExperimentalMinecartController controller) {
            if (!controller.stagingLerpSteps.isEmpty()) {
                ExperimentalMinecartController.Step step = controller.stagingLerpSteps.getLast();
                entity.refreshPositionAndAngles(step.position(), step.yRot(), step.xRot());
                entity.setVelocity(step.movement());
            }
            controller.stagingLerpSteps.clear();
        }
        entity.resetPosition();
    }

    private void restore(Entity entity) {
        ((EntityAccessor) entity).entityrenderdisabler$getChangeListener().remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
        entity.resetPosition();
        restoring = true;
        try {
            world.addEntity(entity);
            MinecraftClient client = MinecraftClient.getInstance();
            if (!isHidden(entity) && client.world == world && client.getNetworkHandler() != null) {
                ((ClientPlayNetworkHandlerAccessor) client.getNetworkHandler()).entityrenderdisabler$playSpawnSound(entity);
            }
        } finally {
            restoring = false;
        }
    }

    public boolean hasSuppressedSource(double x, double y, double z, EntityType<?> type) {
        int sectionX = ChunkSectionPos.getSectionCoord(MathHelper.floor(x));
        int sectionY = ChunkSectionPos.getSectionCoord(MathHelper.floor(y));
        int sectionZ = ChunkSectionPos.getSectionCoord(MathHelper.floor(z));
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    Set<Entity> entities = sections.get(ChunkSectionPos.asLong(sectionX + dx, sectionY + dy, sectionZ + dz));
                    if (entities != null) {
                        for (Entity entity : entities) {
                            if ((type == null || entity.getType() == type) && entity.getBoundingBox().expand(0.75).contains(x, y, z)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        for (Entity entity : retainedVehicles) {
            if ((type == null || entity.getType() == type) && entity.getBoundingBox().expand(0.75).contains(x, y, z)) {
                return true;
            }
        }
        return false;
    }

    public boolean suppressPositionalSound(Identifier sound, double x, double y, double z) {
        if (!ModConfig.hasHiddenTypes() || !sound.getPath().startsWith("entity.")) {
            return false;
        }
        EntityType<?> owner = EntitySoundOwners.get(sound);
        MinecraftClient client = MinecraftClient.getInstance();
        Entity camera = client.getCameraEntity();
        if (camera != null && (owner == null || camera.getType() == owner)
                && camera.getBoundingBox().expand(0.75).contains(x, y, z)) {
            return false;
        }
        if (owner != null && ModConfig.isTypeHidden(owner) && hasSuppressedSource(x, y, z, owner)) {
            return true;
        }
        if (!hasSuppressedSource(x, y, z, null)) {
            return false;
        }
        Box sourceArea = new Box(x - 1, y - 1, z - 1, x + 1, y + 1, z + 1);
        return world.getOtherEntities(null, sourceArea, entity -> !isHidden(entity)
                && entity.getBoundingBox().expand(0.75).contains(x, y, z)).isEmpty();
    }

    private final class DormantListener implements EntityChangeListener {
        private final Entity entity;
        private long section;
        private boolean indexed;

        private DormantListener(Entity entity) {
            this.entity = entity;
        }

        @Override
        public void updateEntityPosition() {
            long nextSection = ChunkSectionPos.toLong(entity.getBlockPos());
            if (!indexed || nextSection != section) {
                removeFromSection();
                section = nextSection;
                sections.computeIfAbsent(section, key -> Collections.newSetFromMap(new IdentityHashMap<>())).add(entity);
                indexed = true;
            }
        }

        @Override
        public void remove(Entity.RemovalReason reason) {
            removeFromSection();
            dormant.remove(entity.getId(), entity);
            entity.setChangeListener(EntityChangeListener.NONE);
        }

        private void removeFromSection() {
            if (indexed) {
                Set<Entity> entities = sections.get(section);
                entities.remove(entity);
                if (entities.isEmpty()) {
                    sections.remove(section);
                }
                indexed = false;
            }
        }
    }

    private static final class EntitySoundOwners {
        private static final Map<Identifier, EntityType<?>> OWNERS = createOwners();

        private static Map<Identifier, EntityType<?>> createOwners() {
            Map<Identifier, EntityType<?>> owners = new HashMap<>();
            Registries.SOUND_EVENT.getIds().forEach(sound -> {
                String path = sound.getPath();
                if (path.startsWith("entity.")) {
                    int end = path.indexOf('.', 7);
                    if (end > 7) {
                        Identifier entityId = Identifier.of(sound.getNamespace(), path.substring(7, end));
                        if (Registries.ENTITY_TYPE.containsId(entityId)) {
                            owners.put(sound, Registries.ENTITY_TYPE.get(entityId));
                        }
                    }
                }
            });
            return Map.copyOf(owners);
        }

        private static EntityType<?> get(Identifier sound) {
            return OWNERS.get(sound);
        }
    }
}
