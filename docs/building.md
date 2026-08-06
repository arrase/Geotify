# Building & Development Guide

Requirements, build commands, and project directory structure for Geotify.

---

## System Requirements

| Requirement | Version | Notes |
| :--- | :--- | :--- |
| **JDK** | OpenJDK 17+ | Required for Gradle 8.x+ and AGP 9.3.x. |
| **Android SDK** | API 37 (`compileSdk = 37`) | Min SDK: `24` (Android 7.0), Target SDK: `36` (Android 15). |
| **Android Studio** | Ladybug (2024.2.1+) | Recommended for Compose live previews. |
| **Gradle Wrapper** | Included (`./gradlew`) | Root wrapper script. |

---

## Build Commands

Run commands from the repository root:

### Debug Build
```bash
./gradlew assembleDebug
```
Output APK location: `app/build/outputs/apk/debug/app-debug.apk`

### Run Unit Tests
```bash
./gradlew test
```
Test report location: `app/build/reports/tests/testDebugUnitTest/index.html`

### Run Instrumented Tests
```bash
./gradlew connectedAndroidTest
```

### Release Bundle & APK
```bash
./gradlew assembleRelease
./gradlew bundleRelease
```

---

## Project Structure

```text
Geotify/
├── app/
│   ├── build.gradle.kts           # Module build configuration
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/dev/arrase/geotify/
│       │   │   ├── appfunction/   # androidx.appfunctions implementations & DTOs
│       │   │   ├── data/          # Room DB, DAOs, Entities, Repositories
│       │   │   ├── di/            # Hilt Dependency Injection modules
│       │   │   ├── geofence/      # Sliding window engine & BroadcastReceivers
│       │   │   ├── location/      # LocationProvider integration
│       │   │   ├── permission/    # PermissionGate onboarding state machine
│       │   │   ├── ui/            # Compose screens, ViewModels, Themes
│       │   │   ├── GeotifyApplication.kt
│       │   │   └── MainActivity.kt
│       │   └── res/               # Drawables, layouts, strings, metadata
│       └── test/                  # Unit tests
├── docs/                          # MkDocs technical documentation source
├── gradle/
│   └── libs.versions.toml         # Version catalog
├── build.gradle.kts               # Root build script
└── mkdocs.yml                     # MkDocs configuration
```
