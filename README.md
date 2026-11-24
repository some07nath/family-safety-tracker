# Family Safety & Real-Time Location Tracker (Android)

A modern Android application built with **Jetpack Compose**, **Google Maps**, **Room Local Database**, **WorkManager**, and **Google Play Services (Fused Location & Activity Recognition)**.

---

## 🌟 Features Implemented

1. **Real-Time Google Maps Location Sharing:**
   - Multi-member live map tracking with smooth camera transitions and custom status markers.
   - Adaptive GPS polling intervals to maximize battery life (e.g., 5s during driving, 60s when stationary).

2. **Offline Data Storage & Auto-Sync Engine:**
   - When internet connectivity is lost, timestamped location points are queued locally inside an encrypted **Room SQLite Database**.
   - When internet connectivity restores, **WorkManager** automatically uploads the queued points in the background.

3. **Multi-Member Family Circle:**
   - Dedicated family view showing all circle members.
   - Live location coordinates, distance, heading, and last update relative timestamps.

4. **Emergency SOS with Automatic SMS Fallback:**
   - Prominent Emergency SOS button with a 5-second countdown to avoid accidental triggers.
   - Sends a high-priority cloud emergency alert when online.
   - **Offline SMS Fallback:** If internet is down, automatically dispatches an SMS with Google Maps coordinates and battery percentage to predefined emergency contacts.

5. **Battery & Charging Telemetry:**
   - Displays real-time battery percentage and charging status (⚡) for every family member.

6. **Activity State Recognition:**
   - Detects whether each family member is **Stationary (Still)**, **Walking**, **Running**, **Cycling**, or **In Vehicle (Driving)**.
   - Displays current activity badges and vehicle speeds (km/h).

7. **Timeline & Route Playback:**
   - Historic route visualization on Google Maps with start/end markers and polylines.
   - Interactive time slider scrubber with Play/Pause animation simulating member movements throughout the day.

---

## 🚀 How to Build the APK on GitHub

This repository is pre-configured with **GitHub Actions** (`.github/workflows/build-apk.yml`) to automatically compile and build the Android APK without needing Android Studio or SDK installed locally.

### Step 1: Initialize Git and Push to GitHub

Open a terminal or command prompt in this project folder and run:

```bash
# 1. Initialize git repository
git init -b main

# 2. Add all project files
git add .

# 3. Commit the changes
git commit -m "Initial commit: Family Safety Android App with CI/CD"

# 4. Link to your GitHub repository (replace with your repo URL)
git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPOSITORY.git

# 5. Push to GitHub
git push -u origin main
```

### Step 2: Download Your Built APK

1. Go to your repository on **GitHub**.
2. Click on the **Actions** tab at the top.
3. You will see the **Build Android APK** workflow running.
4. Once completed (typically 2-4 minutes), click on the workflow run.
5. Under the **Artifacts** section at the bottom, click on **FamilySafety-Debug-APK** to download the ready-to-install `.apk` file!
6. Transfer the `.apk` file to your Android phone and tap to install.

---

## 🔑 Google Maps API Key Setup (Optional for Map Tiles)

To render Google Maps tiles properly:
1. Obtain an API key from the [Google Cloud Console](https://console.cloud.google.com/) with **Maps SDK for Android** enabled.
2. In your GitHub repository:
   - Go to **Settings** > **Secrets and variables** > **Actions**.
   - Add a repository secret named `MAPS_API_KEY` with your API key value.
   - Or add `MAPS_API_KEY=your_key_here` to `local.properties` for local development.

---

## 📱 Tech Stack & Libraries
- **Language:** Kotlin 1.9.24
- **UI Framework:** Jetpack Compose with Material Design 3
- **Local Persistence:** Room Database 2.6.1
- **Background Synchronization:** Android Jetpack WorkManager 2.9.0
- **Location & Sensors:** Google Play Services Fused Location & Activity Recognition
- **CI/CD:** GitHub Actions runner with Temurin JDK 17 & Gradle 8.7
