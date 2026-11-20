# UnifiedCinemaAPK

Standalone Android APK project for a unified cinema/video streaming app supporting both mobile and Android TV.

## Project Structure

```
UnifiedCinemaAPK/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/unifiedcinema/
│   │   │   ├── UnifiedCinemaApplication.kt
│   │   │   ├── MainActivity.kt           # Phone/Tablet UI
│   │   │   ├── TVMainActivity.kt         # Android TV UI (Leanback)
│   │   │   └── media/
│   │   │       └── MediaPlaybackService.kt
│   │   ├── res/
│   │   │   ├── values/
│   │   │   │   ├── strings.xml
│   │   │   │   ├── colors.xml
│   │   │   │   └── themes.xml
│   │   │   ├── xml/
│   │   │   │   ├── backup_rules.xml
│   │   │   │   └── data_extraction_rules.xml
│   │   │   ├── drawable/
│   │   │   │   ├── tv_banner.xml         # TV banner (320x180)
│   │   │   │   └── ic_launcher_monochrome.xml
│   │   │   ├── mipmap-*/
│   │   │   └── mipmap-anydpi-v26/
│   │   │       ├── ic_launcher.xml
│   │   │       └── ic_launcher_round.xml
│   │   └── AndroidManifest.xml
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── .github/workflows/
│   └── build-apk.yml                     # GitHub Actions CI/CD
├── build.gradle.kts                      # Project-level
├── settings.gradle.kts
├── gradle.properties
└── README.md
```

## Dependencies

- **Kotlin** 1.9.22
- **Android Gradle Plugin** 8.4.0
- **Jetpack Compose** (Material3) + BOM 2024.02.00
- **AndroidX Leanback** (TV support)
- **Media3 ExoPlayer** 1.3.1
- **OkHttp** 4.12.0 + Logging Interceptor
- **Kotlinx Serialization** 1.6.2
- **Coroutines** 1.7.3
- **Navigation Compose** 2.7.6

## Android Manifest Features

- `android.software.leanback` (required="false")
- `android.hardware.touchscreen` (required="false")
- Dual launcher: `LAUNCHER` (phone) + `LEANBACK_LAUNCHER` (TV)
- `INTERNET` + `ACCESS_NETWORK_STATE` permissions
- Foreground service for media playback

## Build

### Local Build
```bash
./gradlew assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

### GitHub Actions
Automatically builds on push/PR to main/master. Uses:
- JDK 17 (Temurin)
- Gradle 8.7 (via `gradle/actions/setup-gradle@v4`)
- No wrapper JAR required

Artifact: `app-debug-apk` (retained 30 days)

## Next Steps (Not Implemented Yet)

1. **UI Screens** - Compose screens for movie/series browsing
2. **Video Player** - ExoPlayer integration with Media3
3. **Network Layer** - OkHttp + Serialization for API calls
4. **Data Models** - Kotlin data classes for media content
5. **Repository Pattern** - Offline-first with caching
6. **TV Navigation** - Leanback-style browse fragments (or Compose for TV)
7. **Settings/Preferences** - DataStore for user preferences