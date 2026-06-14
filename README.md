# Geotify

Geotify is a modern, location-aware Android application that allows users to create and manage geofenced reminders. Designed with modern Android development practices, it features a beautiful Jetpack Compose interface and exposes advanced system-level **Jetpack AppFunctions**, making it capable of being driven by on-device LLMs or voice assistants.

---

## Features

- **Location Management**: Save current GPS coordinates with custom, easy-to-remember aliases (e.g., *'home'*, *'gym'*, *'mom's house'*).
- **Geofenced Reminders**: Create triggers that display notifications when arriving or departing from any saved location.
- **Jetpack AppFunctions**: Exposes system-discoverable APIs, enabling assistant-driven or LLM-driven actions directly inside the app.
- **Persistent Local Storage**: Built on **Room Database** to store locations, active geofences, and reminder configurations securely on-device.
- **Reliable Background Execution**: Integrates Google Play Services Geofencing API and registers a `BroadcastReceiver` to handle location transitions even when the app is closed.
- **Boot Recovery**: Automatically re-registers geofences on device boot.
- **Material 3 Design**: Features a fully responsive user interface utilizing Jetpack Compose and Material Design 3 guidelines.

---

## Expose AppFunctions (AI / Agentic Integration)

Geotify implements **Jetpack AppFunctions** (via the `androidx.appfunctions` APIs). This acts as a bridge that allows system services, voice assistants, and local Large Language Models (LLMs) to discover and execute actions within the app context:

- **`saveCurrentLocation(alias: String)`**: Automatically fetches the current high-accuracy GPS coordinates in the background and saves them under the given alias.
- **`createGeofenceReminder(targetAlias: String, payloadMessage: String, triggerOnArrival: Boolean)`**: Creates a reminder linked to a saved location alias, specifying whether it should fire on entry (arrival) or exit (departure).
- **`listLocations()`**: Retrieves all saved locations, showing their names and coordinates.
- **`deleteLocation(alias: String)`**: Deletes a saved location along with all associated reminders and active geofences.
- **`deleteReminder(targetAlias: String, message: String?)`**: Cancels and removes active geofence triggers matching the location. The message is optional; if omitted, it will delete the reminder if only one exists for that location, or return a list of active reminders to resolve ambiguity if multiple exist.
- **`listActiveReminders()`**: Retrieves all currently active reminders, detailing their IDs, location aliases, messages, and triggers.

---

## Tech Stack & Architecture

Geotify is built on a clean MVVM (Model-View-ViewModel) architecture:

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/compose) (with Material Design 3)
- **Local DB**: [Room Database](https://developer.android.com/training/data-storage/room)
- **Location & Geofencing**: Google Play Services Location APIs (`FusedLocationProviderClient`, `GeofencingClient`)
- **Integration**: Jetpack AppFunctions (`androidx.appfunctions`) with KSP code-generation
- **Asynchronous Flow**: Kotlin Coroutines & Flows
- **Dependency Resolution**: Gradle Version Catalogs (`libs.versions.toml`)

---

## System Permissions Required

To function correctly in the background, Geotify requests the following permissions:

- `ACCESS_FINE_LOCATION` & `ACCESS_COARSE_LOCATION`: To obtain the device's coordinates for saving locations.
- `ACCESS_BACKGROUND_LOCATION`: Required by the system to monitor geofences in the background when the app is minimized or closed.
- `POST_NOTIFICATIONS`: To show reminders when geofence transitions occur.
- `RECEIVE_BOOT_COMPLETED`: To automatically restore geofences when the device restarts.
- `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_LOCATION`: To perform foreground operations when fetching single-shot locations.

---

## Building the Project

Ensure you have Android Studio installed.

1. Clone this repository:
   ```bash
   git clone git@github.com:arrase/Geotify.git
   cd Geotify
   ```
2. Build the project using Gradle:
   ```bash
   ./gradlew assembleDebug
   ```
3. Run the application on an emulator or a physical Android device with Google Play Services enabled.
