# Geotify

![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Hilt](https://img.shields.io/badge/DI-Dagger%20Hilt-0052CC?style=for-the-badge&logo=dagger&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)

**Geotify** is a modern, privacy-focused, location-aware Android application designed for creating and managing intelligent geofenced reminders. Built strictly with modern Android development standards, Geotify pairs a rich **Jetpack Compose** Material Design 3 interface with open-source **OpenStreetMap (osmdroid)** maps and system-level **Jetpack AppFunctions** integration for AI assistants.

> [!TIP]
> **Join the Open Beta on Google Play!** 🚀  
> Geotify is available in Open Beta. You can download and test the app directly from the [Google Play Store](https://play.google.com/store/apps/details?id=dev.arrase.geotify).

---

## Key Features

- **<i class="fa-solid fa-map-location-dot"></i> Interactive Location Management**: Save exact GPS coordinates or select custom locations interactively with an embedded **Map Picker**. Customize alias names (*'Home'*, *'Office'*, *'Gym'*), geofence radii (50m to 5km), and notification responsiveness (0 to 10 minutes).
- **<i class="fa-solid fa-bell"></i> Dynamic Geofenced Reminders**: Set arrival (enter) or departure (exit) triggers for saved locations.
- **<i class="fa-solid fa-sliders"></i> Sliding Window Engine**: Overcomes the system-level 100 geofence limit on Android by dynamically active-monitoring the 99 nearest points of interest (POIs) alongside 1 Master Geofence.
- **<i class="fa-solid fa-robot"></i> Jetpack AppFunctions (AI Ready)**: Exposes discoverable system APIs, enabling on-device Large Language Models (LLMs) and voice assistants to query, save locations, and create reminders autonomously.
- **<i class="fa-solid fa-map"></i> OpenStreetMap (osmdroid)**: Fully offline-capable, embedded map rendering with theme-aware tile filtering (automatic light/dark map tiles).
- **<i class="fa-solid fa-palette"></i> Material Design 3 UI**: Clean, fluid Compose layout supporting dynamic colors, micro-animations, light/dark mode switching, and banner notifications for required system permissions.
- **<i class="fa-solid fa-globe"></i> Full Internationalization (i18n)**: Native localized UI in English, Spanish, German, French, Italian, and Portuguese.

---

## Screenshot Gallery

| Reminders List | Reminders Map |
| :---: | :---: |
| ![Reminders List](screenshots/reminders_en.png) | ![Reminders Map](screenshots/reminders_map_en.png) |

| Locations List | Map Picker | Settings |
| :---: | :---: | :---: |
| ![Locations List](screenshots/locations_en.png) | ![Locations Map Picker](screenshots/map_en.png) | ![Settings Screen](screenshots/settings_en.png) |

---

## Documentation Sections

Explore the technical architecture and features of Geotify:

- [**Architecture**](architecture.md): Technical breakdown of MVVM + Clean Architecture, component interaction flow, tech stack, and osmdroid Compose integration.
- [**Geofencing Engine**](geofencing.md): Deep dive into the Sliding Window algorithm, spatial bounding box math formulas, Master Geofence vs POIs, WorkManager scheduling, and transition receivers.
- [**AppFunctions (AI Integration)**](appfunctions.md): Overview of Jetpack AppFunctions schemas, callable APIs, and assistant integration.
- [**Database Schema**](database.md): Room DB entities (`LocationEntity`, `ReminderEntity`), indexes, and DataStore preferences.
- [**System Permissions**](permissions.md): Permission handling for background location, notification posts, activity recognition, and boot triggers.
- [**UI & Screen Guide**](screens.md): Overview of screens, ViewModels, UI state management, and custom Compose map overlays.
- [**Building & Development**](building.md): Prerequisites, build scripts, Gradle dependencies, and developer environment setup.
