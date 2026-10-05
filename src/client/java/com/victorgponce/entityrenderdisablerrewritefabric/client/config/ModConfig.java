package com.victorgponce.entityrenderdisablerrewritefabric.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.victorgponce.entityrenderdisablerrewritefabric.client.EntityrenderdisablerrewritefabricClient;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public final class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir()
            .resolve(EntityrenderdisablerrewritefabricClient.MOD_ID + ".json");
    private static final Map<String, Boolean> ENTITY_STATES = new TreeMap<>();
    private static final Set<EntityType<?>> HIDDEN_TYPES = Collections.newSetFromMap(new IdentityHashMap<>());
    private static boolean modEnabled = true;
    private static boolean soundProtection = true;
    private static long revision;

    private ModConfig() {
    }

    public static boolean isEntityVisible(String entityId) {
        return ENTITY_STATES.getOrDefault(entityId, true);
    }

    public static boolean isTypeHidden(EntityType<?> type) {
        return modEnabled && HIDDEN_TYPES.contains(type);
    }

    public static boolean hasHiddenTypes() {
        return modEnabled && !HIDDEN_TYPES.isEmpty();
    }

    public static void setEntityVisible(String entityId, boolean visible) {
        if (isEntityVisible(entityId) == visible) {
            return;
        }
        ENTITY_STATES.put(entityId, visible);
        Identifier id = Identifier.tryParse(entityId);
        if (id != null && Registries.ENTITY_TYPE.containsId(id)) {
            EntityType<?> type = Registries.ENTITY_TYPE.get(id);
            if (visible) {
                HIDDEN_TYPES.remove(type);
            } else {
                HIDDEN_TYPES.add(type);
            }
        }
        revision++;
    }

    public static boolean isModEnabled() {
        return modEnabled;
    }

    public static boolean isSoundProtectionEnabled() {
        return modEnabled && soundProtection;
    }

    public static boolean getSoundProtection() {
        return soundProtection;
    }

    public static void setSoundProtection(boolean enabled) {
        if (soundProtection != enabled) {
            soundProtection = enabled;
            revision++;
        }
    }

    public static void setModEnabled(boolean enabled) {
        if (modEnabled != enabled) {
            modEnabled = enabled;
            revision++;
        }
    }

    public static long getRevision() {
        return revision;
    }

    public static void load() {
        ENTITY_STATES.clear();
        HIDDEN_TYPES.clear();
        modEnabled = true;
        soundProtection = true;
        if (Files.exists(CONFIG_FILE)) {
            try (var reader = Files.newBufferedReader(CONFIG_FILE, StandardCharsets.UTF_8)) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                if (json != null) {
                    if (json.has("modEnabled") && json.get("modEnabled").isJsonPrimitive()
                            && json.getAsJsonPrimitive("modEnabled").isBoolean()) {
                        modEnabled = json.get("modEnabled").getAsBoolean();
                    }
                    if (json.has("entities") && json.get("entities").isJsonObject()) {
                        json.getAsJsonObject("entities").entrySet().forEach(entry -> {
                            if (entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isBoolean()) {
                                ENTITY_STATES.put(entry.getKey(), entry.getValue().getAsBoolean());
                            }
                        });
                    }
                    if (json.has("soundProtection") && json.get("soundProtection").isJsonPrimitive()
                            && json.getAsJsonPrimitive("soundProtection").isBoolean()) {
                        soundProtection = json.get("soundProtection").getAsBoolean();
                    }
                }
            } catch (IOException | RuntimeException exception) {
                EntityrenderdisablerrewritefabricClient.LOGGER.error("Failed to load entity configuration", exception);
            }
        } else {
            save();
        }
        ENTITY_STATES.forEach((entityId, visible) -> {
            Identifier id = Identifier.tryParse(entityId);
            if (!visible && id != null && Registries.ENTITY_TYPE.containsId(id)) {
                HIDDEN_TYPES.add(Registries.ENTITY_TYPE.get(id));
            }
        });
        revision++;
    }

    public static void save() {
        Path temporary = CONFIG_FILE.resolveSibling(CONFIG_FILE.getFileName() + ".tmp");
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            JsonObject json = new JsonObject();
            json.addProperty("modEnabled", modEnabled);
            json.addProperty("soundProtection", soundProtection);
            json.add("entities", GSON.toJsonTree(ENTITY_STATES));
            try (var writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                GSON.toJson(json, writer);
            }
            try {
                Files.move(temporary, CONFIG_FILE, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException exception) {
                Files.move(temporary, CONFIG_FILE, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            EntityrenderdisablerrewritefabricClient.LOGGER.error("Failed to save entity configuration", exception);
        }
    }

    public static Map<String, Boolean> getEntityRenderStates() {
        return Map.copyOf(ENTITY_STATES);
    }
}
