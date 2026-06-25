# MLKitOcr — Agent Guide

## Project State

This is a **fresh Android Studio scaffolding project** — only the default "Hello World" `MainActivity.kt` exists. The name "MLKitOcr" is aspirational; **no ML Kit dependencies or OCR code have been added yet**.

## Build System

| Item | Value |
|---|---|
| Gradle | 9.4.1 (wrapper) |
| AGP | 9.2.1 |
| JDK | 21 (toolchain via foojay-resolver-convention) |
| Java source/target | 11 |
| Kotlin version | Managed by AGP (not declared explicitly in project) |

### Version Catalog
All dependency versions are in `gradle/libs.versions.toml`. **Never hardcode versions** in `build.gradle.kts` — always add entries to the TOML and reference via `libs.*`.

### Newer DSL Syntax
```kotlin
// AGP 9.x uses this syntax — NOT `compileSdk = 36`
compileSdk {
    version = release(36) {
        minorApiLevel = 1
    }
}
```

### SDK Location
`sdk.dir=D:\AndroidSdk` (set in `local.properties` — Windows host).

## Package & Module

- Single module: `:app`
- Namespace / applicationId: `com.example.mlkitocr`
- Source root: `app/src/main/java/com/example/mlkitocr/`
- minSdk = 24, targetSdk = 36

## Adding ML Kit OCR

When adding OCR functionality, the dependency to add to `gradle/libs.versions.toml` is:

```toml
[versions]
mlkit-ocr = "18.8.0"  # or latest

[libraries]
mlkit-text-recognition = { group = "com.google.mlkit", name = "text-recognition", version.ref = "mlkit-ocr" }
```

Then reference as `implementation(libs.mlkit.text.recognition)` in `app/build.gradle.kts`.

## Key Architecture Notes

- **No Jetpack Compose** — the default template uses XML layouts (`activity_main.xml`) with `AppCompatActivity`.
- **Material3** theme with `NoActionBar` — any new screens should use `Material3` components and themes.
- **Edge-to-edge** — `enableEdgeToEdge()` is called in `MainActivity`. New activities/fragments should follow the same pattern.
- **No DI framework** yet — if adding Hilt/Dagger, add the KSP/Hilt plugin and version-catalog entries.

## Testing

| Test Type | Location | Command |
|---|---|---|
| Unit tests (host) | `app/src/test/` | `./gradlew test` |
| Instrumented (device) | `app/src/androidTest/` | `./gradlew connectedCheck` |

Tests are currently the default boilerplate (`addition_isCorrect`, `useAppContext`). New OCR tests will need device/emulator access (ML Kit requires Camera or image input).

## Gotchas

- **No ProGuard rules yet** — `proguard-rules.pro` is empty. If minify is enabled, add ML Kit keep rules.
- **No Kotlin compiler args configured** — add to `build.gradle.kts` via `kotlinOptions` block if needed.
- **Gradle daemon JVM is JDK 21** — ensure JDK 21 is on PATH or the toolchain resolver can download it.
- **ML Kit CameraX/Viewfinder** — for live OCR, you'll also need `camera-camera2` and `camera-lifecycle` dependencies (not yet in the catalog).
