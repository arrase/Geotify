---
hide:
  - toc
---

<div class="hero-section">
  <div class="hero-eyebrow">
    <i class="fa-solid fa-map-location-dot" aria-hidden="true"></i>
    <span>Location-Aware Android Reminders</span>
  </div>
  <h1>Geotify</h1>
  <p class="hero-subtitle">A modern, privacy-focused Android application for intelligent geofenced reminders, pairing a Jetpack Compose Material 3 UI and offline-capable OpenStreetMap rendering with system-level Jetpack AppFunctions for AI assistants.</p>
  <div class="hero-badges">
    <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=flat&amp;logo=android&amp;logoColor=white" alt="Android">
    <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?style=flat&amp;logo=kotlin&amp;logoColor=white" alt="Kotlin">
    <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=flat&amp;logo=jetpackcompose&amp;logoColor=white" alt="Jetpack Compose">
    <img src="https://img.shields.io/badge/DI-Dagger%20Hilt-0052CC?style=flat&amp;logo=dagger&amp;logoColor=white" alt="Hilt">
    <img src="https://img.shields.io/badge/License-MIT-blue?style=flat" alt="License">
  </div>
  <div class="hero-badges">
    <a href="https://sonarcloud.io/summary/new_code?id=arrase_Geotify"><img src="https://img.shields.io/sonar/quality_gate/arrase_Geotify?server=https%3A%2F%2Fsonarcloud.io&amp;logo=sonarcloud" alt="Quality Gate Status"></a>
    <a href="https://sonarcloud.io/summary/new_code?id=arrase_Geotify"><img src="https://img.shields.io/sonar/coverage/arrase_Geotify?server=https%3A%2F%2Fsonarcloud.io&amp;logo=sonarcloud" alt="Coverage"></a>
    <a href="https://sonarcloud.io/summary/new_code?id=arrase_Geotify"><img src="https://img.shields.io/sonar/bugs/arrase_Geotify?server=https%3A%2F%2Fsonarcloud.io&amp;logo=sonarcloud" alt="Bugs"></a>
    <a href="https://sonarcloud.io/summary/new_code?id=arrase_Geotify"><img src="https://img.shields.io/sonar/vulnerabilities/arrase_Geotify?server=https%3A%2F%2Fsonarcloud.io&amp;logo=sonarcloud" alt="Vulnerabilities"></a>
    <a href="https://sonarcloud.io/summary/new_code?id=arrase_Geotify"><img src="https://img.shields.io/sonar/code_smells/arrase_Geotify?server=https%3A%2F%2Fsonarcloud.io&amp;logo=sonarcloud" alt="Code Smells"></a>
    <a href="https://sonarcloud.io/summary/new_code?id=arrase_Geotify"><img src="https://img.shields.io/sonar/tech_debt/arrase_Geotify?server=https%3A%2F%2Fsonarcloud.io&amp;logo=sonarcloud" alt="Tech Debt"></a>
    <a href="https://sonarcloud.io/summary/new_code?id=arrase_Geotify"><img src="https://img.shields.io/sonar/duplicated_lines_density/arrase_Geotify?server=https%3A%2F%2Fsonarcloud.io&amp;logo=sonarcloud" alt="Duplicated Lines"></a>
    <a href="https://sonarcloud.io/summary/new_code?id=arrase_Geotify"><img src="https://img.shields.io/sonar/security_rating/arrase_Geotify?server=https%3A%2F%2Fsonarcloud.io&amp;logo=sonarcloud" alt="Security Rating"></a>
    <a href="https://sonarcloud.io/summary/new_code?id=arrase_Geotify"><img src="https://img.shields.io/sonar/reliability_rating/arrase_Geotify?server=https%3A%2F%2Fsonarcloud.io&amp;logo=sonarcloud" alt="Reliability Rating"></a>
    <a href="https://sonarcloud.io/summary/new_code?id=arrase_Geotify"><img src="https://img.shields.io/sonar/sqale_rating/arrase_Geotify?server=https%3A%2F%2Fsonarcloud.io&amp;logo=sonarcloud" alt="Maintainability Rating"></a>
  </div>
</div>

!!! tip "Join the Open Beta on Google Play! 🚀"
    Geotify is available in Open Beta. You can download and test the app directly from the [Google Play Store](https://play.google.com/store/apps/details?id=dev.arrase.geotify).

<div class="section-header">
  <h2>Key Features</h2>
  <span>Geofencing engine &amp; AI assistant integration</span>
</div>

<div class="projects-grid">
  <div class="feature-card">
    <i class="fa-solid fa-map-location-dot feature-icon" aria-hidden="true"></i>
    <h3>Interactive Location Management</h3>
    <p>Save exact GPS coordinates or select custom locations interactively with an embedded Map Picker. Customize alias names, geofence radii (50m to 5km), and notification responsiveness (0 to 10 minutes).</p>
  </div>

  <div class="feature-card">
    <i class="fa-solid fa-bell feature-icon" aria-hidden="true"></i>
    <h3>Dynamic Geofenced Reminders</h3>
    <p>Configure arrival (enter) or departure (exit) triggers for saved locations with custom reminder notes and reliable background notifications.</p>
  </div>

  <div class="feature-card">
    <i class="fa-solid fa-sliders feature-icon" aria-hidden="true"></i>
    <h3>Sliding Window Engine</h3>
    <p>Overcomes the system-level 100 geofence limit on Android by dynamically active-monitoring the 99 nearest points of interest (POIs) alongside 1 Master Geofence.</p>
  </div>

  <div class="feature-card">
    <i class="fa-solid fa-robot feature-icon" aria-hidden="true"></i>
    <h3>Jetpack AppFunctions (AI Ready)</h3>
    <p>Exposes discoverable system APIs, enabling on-device Large Language Models (LLMs) and voice assistants to query, save locations, and create reminders autonomously.</p>
  </div>

  <div class="feature-card">
    <i class="fa-solid fa-map feature-icon" aria-hidden="true"></i>
    <h3>OpenStreetMap (osmdroid)</h3>
    <p>Fully offline-capable, embedded map rendering with theme-aware tile filtering for automatic light and dark map tiles.</p>
  </div>

  <div class="feature-card">
    <i class="fa-solid fa-palette feature-icon" aria-hidden="true"></i>
    <h3>Material Design 3 &amp; i18n</h3>
    <p>Clean, fluid Compose layout supporting dynamic colors, micro-animations, permission banners, and native localization in English, Spanish, German, French, Italian, and Portuguese.</p>
  </div>
</div>

<div class="section-header">
  <h2>Screenshot Gallery</h2>
  <span>Material Design 3 &amp; interactive OpenStreetMap UI</span>
</div>

| Reminders List | Reminders Map |
| :---: | :---: |
| ![Reminders List](screenshots/reminders_en.png) | ![Reminders Map](screenshots/reminders_map_en.png) |

| Locations List | Map Picker | Settings |
| :---: | :---: | :---: |
| ![Locations List](screenshots/locations_en.png) | ![Locations Map Picker](screenshots/map_en.png) | ![Settings Screen](screenshots/settings_en.png) |

<div class="section-header">
  <h2>Documentation</h2>
  <span>Architecture, geofencing math &amp; Android setup</span>
</div>

<div class="projects-grid">
  <a href="architecture/" class="feature-card">
    <div class="card-top">
      <i class="fa-solid fa-sitemap feature-icon" aria-hidden="true"></i>
      <span class="card-badge">Read →</span>
    </div>
    <h3>Architecture</h3>
    <p>Technical breakdown of MVVM + Clean Architecture, component interaction flow, tech stack, and osmdroid Compose integration.</p>
  </a>

  <a href="geofencing/" class="feature-card">
    <div class="card-top">
      <i class="fa-solid fa-location-crosshairs feature-icon" aria-hidden="true"></i>
      <span class="card-badge">Read →</span>
    </div>
    <h3>Geofencing Engine</h3>
    <p>Deep dive into the Sliding Window algorithm, spatial bounding box math formulas, Master Geofence vs POIs, WorkManager scheduling, and transition receivers.</p>
  </a>

  <a href="appfunctions/" class="feature-card">
    <div class="card-top">
      <i class="fa-solid fa-robot feature-icon" aria-hidden="true"></i>
      <span class="card-badge">Read →</span>
    </div>
    <h3>AppFunctions (AI Integration)</h3>
    <p>Overview of Jetpack AppFunctions schemas, callable APIs, and assistant integration.</p>
  </a>

  <a href="database/" class="feature-card">
    <div class="card-top">
      <i class="fa-solid fa-database feature-icon" aria-hidden="true"></i>
      <span class="card-badge">Read →</span>
    </div>
    <h3>Database Schema</h3>
    <p>Room DB entities (LocationEntity, ReminderEntity), indexes, and DataStore preferences.</p>
  </a>

  <a href="permissions/" class="feature-card">
    <div class="card-top">
      <i class="fa-solid fa-shield-halved feature-icon" aria-hidden="true"></i>
      <span class="card-badge">Read →</span>
    </div>
    <h3>System Permissions</h3>
    <p>Permission handling for background location, notification posts, activity recognition, and boot triggers.</p>
  </a>

  <a href="screens/" class="feature-card">
    <div class="card-top">
      <i class="fa-solid fa-mobile-screen-button feature-icon" aria-hidden="true"></i>
      <span class="card-badge">Read →</span>
    </div>
    <h3>UI &amp; Screen Guide</h3>
    <p>Overview of screens, ViewModels, UI state management, and custom Compose map overlays.</p>
  </a>

  <a href="building/" class="feature-card">
    <div class="card-top">
      <i class="fa-solid fa-hammer feature-icon" aria-hidden="true"></i>
      <span class="card-badge">Read →</span>
    </div>
    <h3>Building &amp; Development</h3>
    <p>Prerequisites, build scripts, Gradle dependencies, and developer environment setup.</p>
  </a>
</div>
