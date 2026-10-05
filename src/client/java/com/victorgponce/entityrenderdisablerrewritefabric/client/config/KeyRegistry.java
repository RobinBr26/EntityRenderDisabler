package com.victorgponce.entityrenderdisablerrewritefabric.client.config;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class KeyRegistry {
    private KeyRegistry() {
    }

    public static void register() {
        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of("entityrenderdisabler", "general"));
        KeyBinding config = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.entityrenderdisabler.openConfig", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_O, category));
        KeyBinding toggle = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.entityrenderdisabler.toggleRender", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (config.wasPressed()) {
                client.setScreen(YACLIntegration.createConfigScreen(client.currentScreen));
            }
            while (toggle.wasPressed()) {
                boolean enabled = !ModConfig.isModEnabled();
                ModConfig.setModEnabled(enabled);
                ModConfig.save();
                if (client.world != null) {
                    Text status = Text.translatable(enabled
                            ? "message.entityrenderdisabler.enabled" : "message.entityrenderdisabler.disabled")
                            .formatted(enabled ? Formatting.GREEN : Formatting.RED);
                    client.inGameHud.setOverlayMessage(
                            Text.translatable("message.entityrenderdisabler.toggleRender").append(" ").append(status), false);
                }
            }
        });
    }
}
