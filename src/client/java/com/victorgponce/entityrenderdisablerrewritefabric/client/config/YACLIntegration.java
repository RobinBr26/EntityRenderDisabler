package com.victorgponce.entityrenderdisablerrewritefabric.client.config;

import com.victorgponce.entityrenderdisablerrewritefabric.client.EntityrenderdisablerrewritefabricClient;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.EntityType;
import net.minecraft.text.Text;

import java.util.Map;

public final class YACLIntegration {
    private YACLIntegration() {
    }

    public static Screen createConfigScreen(Screen parent) {
        YetAnotherConfigLib.Builder builder = YetAnotherConfigLib.createBuilder()
                .title(Text.translatable("config.entityrenderdisabler.title"))
                .save(ModConfig::save)
                .category(ConfigCategory.createBuilder()
                        .name(Text.translatable("config.entityrenderdisabler.general"))
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.translatable("config.entityrenderdisabler.enabled"))
                                .description(OptionDescription.of(Text.translatable("config.entityrenderdisabler.enabled.description")))
                                .binding(true, ModConfig::isModEnabled, ModConfig::setModEnabled)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.translatable("config.entityrenderdisabler.soundProtection"))
                                .description(OptionDescription.of(Text.translatable("config.entityrenderdisabler.soundProtection.description")))
                                .binding(true, ModConfig::getSoundProtection, ModConfig::setSoundProtection)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .build());

        for (Map.Entry<String, Map<String, EntityType<?>>> mod : EntityrenderdisablerrewritefabricClient.getEntitiesByMod().entrySet()) {
            OptionGroup.Builder group = OptionGroup.createBuilder().name(Text.literal(mod.getKey()));
            mod.getValue().forEach((id, type) -> group.option(Option.<Boolean>createBuilder()
                    .name(type.getName())
                    .description(OptionDescription.of(Text.translatable("config.entityrenderdisabler.description"), Text.literal(id)))
                    .binding(true, () -> ModConfig.isEntityVisible(id), value -> ModConfig.setEntityVisible(id, value))
                    .controller(TickBoxControllerBuilder::create)
                    .build()));
            builder.category(ConfigCategory.createBuilder()
                    .name(Text.literal(mod.getKey()))
                    .group(group.build())
                    .build());
        }
        return builder.build().generateScreen(parent);
    }
}
