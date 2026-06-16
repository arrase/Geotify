# Privacy Policy for Geotify

**Effective Date:** June 17, 2026

Geotify ("the Application") is built as an open-source, location-aware Android application. This Privacy Policy informs users of our policies regarding the collection, use, and disclosure of personal information when using the Application.

---

## 1. Information Collection and Use

**Geotify does not collect, store, transmit, track, or share any personal data.** 

All information you input or configure within the Application remains strictly on your local device.

### Location Data & Permissions
To provide its core functionality (geofenced reminders), the Application requires the following permissions:
- **Approximate & Precise Location (`ACCESS_COARSE_LOCATION` & `ACCESS_FINE_LOCATION`)**: Used to fetch the device's current coordinates when you save a location.
- **Background Location (`ACCESS_BACKGROUND_LOCATION`)**: Required by the Android operating system to monitor geofences (detecting when you enter or exit a saved area) even when the Application is closed or not in use.

**Important details about your Location Data:**
- All location processing is handled **locally on your device** by the Android operating system's built-in Geofencing API (via Google Play Services).
- **No location data is ever transmitted, uploaded, or shared with the developer or any third-party server.**

### Local Storage
All your saved locations, reminders, and settings are stored locally on your device using:
- **Room Database**: For storing your custom location coordinates and reminders.
- **Preferences DataStore**: For storing user interface preferences (such as Light/Dark theme settings).

This data never leaves your device and is completely deleted when you uninstall the Application.

---

## 2. Third-Party Services

The Application does not integrate with any third-party services that collect information used to identify you.
- No ads or ad networks.
- No analytics or tracking libraries.
- No cloud backend or external API connections (map tiles are rendered from OpenStreetMap and cached locally on the device).

---

## 3. Changes to This Privacy Policy

We may update our Privacy Policy from time to time. You are advised to review this page periodically for any changes. Changes to this Privacy Policy are effective when they are posted on this page.

---

## 4. Contact Us

If you have any questions or suggestions about this Privacy Policy, do not hesitate to contact us by opening an issue on our GitHub repository:
[Geotify GitHub Repository](https://github.com/arrase/Geotify)
