package com.victorgponce.entityrenderdisablerrewritefabric.client;

import com.victorgponce.entityrenderdisablerrewritefabric.client.config.KeyRegistry;
import com.victorgponce.entityrenderdisablerrewritefabric.client.config.ModConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import com.victorgponce.entityrenderdisablerrewritefabric.client.culling.CampfireCulling;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.TreeMap;

public class EntityrenderdisablerrewritefabricClient implements ClientModInitializer {
    public static final String MOD_ID = "entityrenderdisabler";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final Map<String, Map<String, EntityType<?>>> ENTITIES_BY_MOD = new TreeMap<>();

    @Override
    public void onInitializeClient() {
        ModConfig.load();
        Registries.ENTITY_TYPE.forEach(entityType -> {
            Identifier id = Registries.ENTITY_TYPE.getId(entityType);
            ENTITIES_BY_MOD.computeIfAbsent(id.getNamespace(), key -> new TreeMap<>()).put(id.toString(), entityType);
        });
        KeyRegistry.register();
        ClientTickEvents.END_CLIENT_TICK.register(CampfireCulling::tick);
        LOGGER.info("Entity Render Disabler initialized");
    }

    public static Map<String, Map<String, EntityType<?>>> getEntitiesByMod() {
        return ENTITIES_BY_MOD;
    }
}
