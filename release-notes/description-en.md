# DepthWard Camera

Keep your character in view when nearby terrain gets between the player and the third-person camera.

DepthWard Camera creates a soft-edged circular opening in obstructing terrain. The opening grows when the camera moves closer to the player, while the local player gradually fades using a fine dither pattern.

## Features

- Circular terrain cutaway with a soft, dithered edge.
- A larger opening as the camera is pushed closer to the player.
- Close-range fading for your own character, including an Epic Fight shader path.
- A camera limit for detected one-block-thick walls: the camera can enter the wall but stops shortly before its far face.
- Floor-aware cutaway rules intended to preserve nearby support surfaces and carpets.
- Dark interior surfaces to cover exposed block interiors. These black areas are part of the current rendering approach.
- Black interior masks and the thin-wall camera limit are disabled in spectator mode.

## Requirements and installation

- Minecraft **1.20.1**.
- **Forge 47.4.x**; development and local checks used **47.4.28**.
- **Java 17**.
- Install the JAR in the client `mods` folder and restart Minecraft. No server installation is required.
- Use a third-person view. The cutaway is not active in first person.

## Compatibility

Integrations were developed against **Shoulder Surfing Reloaded 5.2.0**, **Embeddium 0.3.31**, and **Epic Fight 20.15.2**. These mods are optional, not bundled dependencies. This is a tested development configuration, not a guarantee for every combination or version.

**Active shader packs are not supported.** When an active shader pack is detected through the supported Iris/Oculus API, the camera feature disables itself. Local development used Oculus 1.8.0 with shader packs disabled.

## Configuration

Settings are stored in `config/depthward_camera-client.toml`:

- `enabled`: enable or disable the camera feature.
- `radius`: base circle radius relative to viewport height; default **0.253**.
- `feather`: soft-edge width; default **0.15**.
- `depthMargin`: small clearance behind the player bounds; default **0.02**.

The circle also scales with camera proximity automatically. Player fading uses a visible dither pattern rather than conventional smooth alpha blending. The F8 diagnostic key cycles the two black-mask passes; a fourth press returns to normal, and leaving the world resets the mode. The key can be rebound under **DepthWard Camera**.

## Beta status and limitations

This is an experimental rendering mod. Thin-wall detection and floor preservation use geometric approximations. Complex block shapes, unusual camera offsets, resource packs, and other rendering mods can produce clipping or black polygons. Not every reported visual case has been reproduced in a full client test.

The privacy mask is local and approximate; it is not a guarantee that every hidden space stays concealed. Performance depends on resolution and scene complexity, particularly while the cutaway is active. No controlled in-game FPS comparison has been completed.

The release was checked with Java compilation, Minecraft API linkage checks, Mixin target checks, and synthetic GPU tests, including the Epic Fight fragment shader. Source code is supplied separately. The current project license is **All Rights Reserved**.
