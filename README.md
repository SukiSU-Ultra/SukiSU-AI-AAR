# SukiSU AI AAR

Minimal Gradle project that builds the optional AI assistant AAR. It contains
only the `:ai` library module and its `:ai-api` bridge dependency; the host app,
kernel and the SukiSU manager sources are not part of this project.


## One-click build (Windows)

Double-click `build-aar.bat` (builds both variants), or run:

```bat
build-aar.bat            :: debug + release
build-aar.bat debug      :: debug only
build-aar.bat release    :: release only
build-aar.bat both --no-pause
```

The script builds with the Gradle wrapper and copies the AAR to the project
root as `ai.aar`:

```text
ai.aar          # debug build, release build, or release after "both"
```

The original variant outputs remain available:

```text
ai/build/outputs/aar/ai-debug.aar
ai/build/outputs/aar/ai-release.aar
```

## Build

Both variants:

```bash
./gradlew :ai:assembleAar
```

Release only:

```bash
./gradlew :ai:assembleAarRelease
# output: ai/build/outputs/aar/ai-release.aar
```

Debug only:

```bash
./gradlew :ai:assembleAarDebug
# output: ai/build/outputs/aar/ai-debug.aar
```

In Android Studio, open this directory as the Gradle project and use the Gradle
tool window: `ai > Tasks > build > assembleAar` (both), `assembleAarDebug`, or
`assembleAarRelease`. No terminal is required.

Tests:

```bash
./gradlew :ai:testDebugUnitTest
```

## Deploy to the manager app

`build-aar.bat` copies the root `ai.aar` to:

```text
C:\Users\Shirkneko\Documents\GitHub\SukiSU-Ultra\manager\app\libs\ai.aar
```

If the directory does not exist, the script creates it. Change `DEPLOY_DIR` at
the top of `build-aar.bat` if the SukiSU-Ultra checkout lives elsewhere.

Manual equivalent:

```bat
mkdir C:\Users\Shirkneko\Documents\GitHub\SukiSU-Ultra\manager\app\libs
copy /Y C:\Users\Shirkneko\Documents\GitHub\SukiSU-AI-AAR\ai.aar C:\Users\Shirkneko\Documents\GitHub\SukiSU-Ultra\manager\app\libs\ai.aar
```

Keep `manager/ai-api` and the Host adapter in `manager/app/.../ai/host`; the AAR
does not bundle them.

The manager uses a trimmed `material-icons-custom.jar` instead of
`material-icons-extended`, but the AAR compiles against the full icon set. Every
Material icon the AAR references must exist in that jar at runtime. The icon
generator scans `manager/app/libs/ai.aar` and merges its references, so rebuild
the jar after adding an icon to the AAR:

```powershell
cd C:\Users\Shirkneko\Documents\GitHub\SukiSU-Ultra
powershell -ExecutionPolicy Bypass -File scripts/generate_material_icons.ps1
```

If the SukiSU-Ultra checkout is on the `ai_chat` branch, `app/libs/ai.aar` takes
precedence over the `:ai` source module. Remove that file when you want to build
from source again.

## Use the AAR

Copy `ai.aar` to the consuming app as `app/libs/ai.aar`. The consuming
host must also include the `:ai-api` bridge module (the AAR declares it but does
not bundle its classes) and implement `AiHost`; when the AAR is absent,
`AiFeatureBridge.isAvailable` is false and the host hides every AI entry.
