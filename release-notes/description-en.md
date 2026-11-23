# DepthWard Camera 1.0.38 Beta

An adaptive circular cutaway helps keep your character visible when terrain obstructs the third-person camera. The opening expands at close range and the local player fades using a fine dither pattern.

Full walls one or two blocks thick allow the camera inside but stop it shortly before the far face. Fences, stairs and slabs use actual collision shapes. Thick terrain permits camera passage, including below ground. Front and side limits help restrict unwanted views; complex geometry can still expose hidden areas or produce black clipping.

Minecraft 1.20.1, Forge 47.4.x, Java 17. Client-side only; restart after installation. Development used Forge 47.4.28 with optional Shoulder Surfing Reloaded 5.2.0, Embeddium 0.3.31, Epic Fight 20.15.2, and Oculus 1.8.0 with shader packs disabled. Active shader packs disable the feature.

Configuration: config/depthward_camera-client.toml. Base circle radius is 0.253 of screen height; automatic proximity scaling reaches up to 2.2x. Feather defaults to 0.15, depthMargin to 0.02. The legacy cylinderRadius parameter is retained but does not size this circular version. Spectator bypasses camera collision limits and black interior caps.

This is a Beta with approximate geometric checks, not guaranteed anti-xray protection. Performance has not been benchmarked. The release excludes the cylinder and whole-block experiments and the later smoothing changes.
