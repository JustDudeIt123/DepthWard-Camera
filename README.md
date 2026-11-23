# DepthWard Camera

Client-side circular third-person camera cutaway for Minecraft 1.20.1 / Forge. Current release: **1.0.38 Beta**.

The opening grows as the camera approaches the player. The local player fades using dithering at close range. One/two-block full walls limit the camera near their far face; partial collision shapes use their actual surface. The camera may enter thick terrain and move below the floor.

This is the user-approved circular version. It excludes the cylinder and later whole-block/smoothing experiments. Wall and privacy checks remain approximate: clipping, black surfaces and views into hidden areas may occur in complex geometry. It is not an anti-xray guarantee. Shader packs are unsupported. Performance has not been benchmarked.

Optional integrations target Shoulder Surfing Reloaded 5.2.0, Embeddium 0.3.31 and Epic Fight 20.15.2. They are not bundled dependencies. Java 17 is required. License: All Rights Reserved.

- [Changelog](release-notes/CHANGELOG.txt)
- [Release details](release-notes/description-en.md)

## Build

Verified local build uses Python 3, JDK 17 and an ATLauncher installation containing Minecraft 1.20.1 / Forge 47.4.28 libraries, mappings, ASM 9.9.1 and srgutils 0.5.6. The builder does not download dependencies or modify third-party JARs. Preserve the directory structure, including tools/java.

```powershell
python depthward-camera/tools/build_local.py --launcher "D:/ATLauncher" --jdk "D:/ATLauncher/runtimes/minecraft/java-runtime-gamma/windows-x64/java-runtime-gamma" --cache "./build-cache"
```

Output: depthward-camera/build/libs/depthward-camera-1.0.38.jar.

ForgeGradle project files are also provided; Gradle Wrapper is not included and that build path has not been verified here.

## Validation

The restored sources were rebuilt and every JAR entry matched the checkpoint runtime content. GPU tests compile 52 fragment shaders and check circle clipping, wall planes, floor transitions and player fade. Running them requires locally installed Embeddium and Epic Fight JARs next to the module plus LWJGL libraries in ATLauncher:

```powershell
python depthward-camera/tools/test_graphics.py --launcher "D:/ATLauncher" --jdk "D:/ATLauncher/runtimes/minecraft/java-runtime-gamma/windows-x64/java-runtime-gamma"
```

The user confirmed the 1.0.38 camera/floor behavior. This does not replace comprehensive full-client compatibility testing.
