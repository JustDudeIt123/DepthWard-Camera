# DepthWard Camera

Client-side third-person camera cutaway for Minecraft 1.20.1 / Forge. Current version: **1.0.18 Beta**.

- [English description](release-notes/description-en.md)
- [Описание на русском](release-notes/description-ru.md)
- [Changelog](release-notes/changelog.md)

Active shader packs are unsupported. Optional integrations target Shoulder Surfing Reloaded 5.2.0, Embeddium 0.3.31, and Epic Fight 20.15.2. See the descriptions for known rendering limitations. License: **All Rights Reserved**.

## Сборка из исходников

Minecraft 1.20.1 / Forge 47.4.28 / Java 17. Лицензия соответствует метаданным JAR: All Rights Reserved. Предоставление исходников не меняет её автоматически.

Архив содержит исходники мода, ресурсы, тесты и собственные Java-инструменты сборки. Сторонние JAR, Minecraft, кэши, игровые миры, настройки пользователя и старые версии исключены.

## Проверенный локальный способ сборки

Требуются Python 3, JDK 17 и установленные библиотеки ATLauncher для Minecraft 1.20.1 / Forge 47.4.28. Скрипт использует локальные библиотеки и mappings из папки launcher/libraries, включая ASM 9.9.1 и srgutils 0.5.6. Самостоятельно зависимости не скачивает. Структуру папок архива необходимо сохранить: папка tools/java находится рядом с depthward-camera.

Из корня распакованного архива:

```powershell
python depthward-camera/tools/build_local.py --launcher "D:/ATLauncher" --jdk "D:/ATLauncher/runtimes/minecraft/java-runtime-gamma/windows-x64/java-runtime-gamma" --cache "./build-cache"
```

Замените пути на свои. Результат: depthward-camera/build/libs/depthward-camera-1.0.18.jar. Скрипт компилирует, remap-ит только собственные классы и проверяет ссылки Minecraft API. Сторонние моды не изменяет.

Для обычной разработки также приложены build.gradle/settings.gradle с ForgeGradle 6.0.54 и official mappings 1.20.1. Gradle Wrapper не включён; этот путь сборки в данной подготовке не проверялся. Проверенный путь — Python-скрипт выше.

## Тесты

CameraGraphicsTest.java и tools/test_graphics.py создают скрытое OpenGL-окно и проверяют шейдеры. Для извлечения исходных шейдеров нужны установленные Embeddium 0.3.31 и Epic Fight 20.15.2 в папке mods рядом с depthward-camera, а также библиотеки LWJGL из ATLauncher. Эти JAR не входят в архив.

WallBoundaryTest.java проверяет расчёт дальней грани стены; CameraHookCheck.java проверяет точки Mixin по runtime JAR. Полный игровой клиент эти тесты не заменяют.

Ограничения и пользовательское описание находятся в release-notes/.
