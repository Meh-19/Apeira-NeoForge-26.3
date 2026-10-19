# Apeira

Modular NeoForge mod for Minecraft 26.3. See [CLAUDE.md](CLAUDE.md) for the vision and architecture.

## Layout

| Module | Mod id | Package | Role |
| --- | --- | --- | --- |
| `apeira-core` | `apeira_core` | `com.apeira.core` | Registries, networking payloads, datagen bases |
| `apeira-decor` | `apeira_decor` | `com.apeira.decor` | Furniture shapes, seating, painting, cosmetics |
| `apeira-story` | `apeira_story` | `com.apeira.story` | Quests, NPC dialogue, boss state machines |
| `apeira-archae` | `apeira_archae` | `com.apeira.archae` | Archaeology content (the legacy Argon mod); depends on core and decor |
| `apeira-main` | `apeira` | `com.apeira.main` | Game content; depends on the three libraries |

Shared build logic lives in the root `build.gradle`; versions are in the root `gradle.properties`
and each module's own `gradle.properties` holds its mod id, name and description.

## Building and running

Requires JDK 25 (Minecraft 26.x runs on Java 25). Gradle downloads it automatically if it is missing.

- `./gradlew build` builds all five mod jars.
- Run configurations live in `apeira-main` and load all five mods: `runClient`, `runServer`,
  `runGameTestServer`, `runData`, `runDecorData` and `runArchaeData` (the last two write
  `apeira-decor/src/generated/resources` and `apeira-archae/src/generated/resources`). IntelliJ picks them up after a
  Gradle sync.
