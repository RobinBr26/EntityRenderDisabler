# Entity Render Disabler

Fabric client mod for Minecraft **1.21.11**. Original author: **Ponchisao326**. **Optimized by RobinBr26**.

Press **O** to configure entity types and **G** to toggle suppression. A checked entity type stays visible; an unchecked type is suppressed. The existing `config/entityrenderdisabler.json` format remains supported. Fabric Loader **0.17.3 or newer**, Fabric API and Yet Another Config Lib 3 are required; Mod Menu is optional.

Suppressed entities are held outside the client's active entity manager. They do not participate in normal world rendering, entity ticks, spatial queries or targeting. This also avoids building render states, names, shadows and outlines for them. Server updates still maintain their position, velocity, equipment and tracked data, so enabling a type or switching the mod off restores the same entities without reconnecting.

Entity sound packets, positional sounds with identifiable entity types, spawn sounds, status effects and pickup animations are suppressed. Existing sounds are stopped when the configuration changes, and particle emitters stop producing new particles. Shared generic entity sounds use a spatial index to locate suppressed sources. Experimental minecart movement retains the latest position rather than accumulating interpolation packets.

Your own player and the camera entity stay active. Hidden vehicles carrying visible passengers keep the ticks required for riding; their rendering, sounds and tick-generated particles remain suppressed. This prevents freezing a visible rider or breaking player movement.

This is client-side suppression. Entities still exist on the server and can cause damage, collisions, drops and server load. An integrated singleplayer server still simulates them. Independent server particle packets and custom effects without an identifiable entity source cannot always be attributed safely. Particles already emitted can finish their lifetime. Suppression does not guarantee a fixed FPS improvement or eliminate server-side lag.

## Build and verification

Use Java 21:

```powershell
.\gradlew.bat build
.\gradlew.bat runClientGameTest
```

The distributable mod is `build/libs/entityrenderdisablerrewritefabric-2.0.0.jar`.

The client integration test creates temporary worlds and checks active-world removal, spatial queries, server movement updates, restoration, entity ID reuse, destruction, player and camera protection, riding, sounds, particle suppression and a population of 5,000 entities. The test mod is kept in its own source set and is excluded from the distributable JAR.

The original project's copyright notice remains in `LICENSE.txt`.
