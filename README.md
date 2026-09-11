# Velvet Music 🎵,

An Android music player designed with rich velvet aesthetics, deep smoky ash glass, and dynamic sound reactive visuals.

## 🚀 Building & Installing the APK from GitHub

This repository is fully configured with an automated GitHub Actions workflow (`.github/workflows/build-apk.yml`) to automatically compile and distribute the Android APK.

---

### Option 1: Automatic Build via GitHub Actions (Recommended)

1. **Push this repository to GitHub**:
   - In AI Studio, you can push directly to GitHub from the top-right menu or commit via git:
     ```bash
     git add .
     git commit -m "Configure GitHub Actions APK build"
     git push origin main
     ```
2. **Go to the "Actions" tab in your GitHub repository**:
   - Click on the **Build and Release Android APK** workflow on the left side.
   - Click **Run workflow** -> Select `main` branch -> Click the green **Run workflow** button (ensure *Publish as a GitHub Release* is checked).
3. **Download and Install the APK on your Phone**:
   - **From Releases (Easiest)**: Go to the **Releases** section on your GitHub repository page from your phone's browser. Under the latest build, tap **`Velvet-Music.apk`** to download it directly.
   - **From Actions Artifacts**: In the workflow run details, scroll down to **Artifacts** and download **`Velvet-Music-APK`**.

---

### Option 2: Build Locally with Gradle

If you have cloned the repository to your local computer:

1. Ensure **JDK 17 or JDK 21** and the Android SDK are installed.
2. Run the Gradle build:
   ```bash
   ./gradlew :app:assembleDebug
   ```
   (On Windows: `gradlew.bat :app:assembleDebug`)
3. The compiled APK will be located at:
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```
4. Transfer `app-debug.apk` to your phone or install via ADB:
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

---

### 📲 Installing on Your Android Phone

1. Once downloaded onto your Android phone, tap the notification or open your **Files / Downloads** app and tap **`Velvet-Music.apk`**.
2. If Android displays a prompt saying *"For your security, your phone is not allowed to install unknown apps from this source"*:
   - Tap **Settings**.
   - Toggle on **Allow from this source**.
3. Tap **Install**, then tap **Open** to launch Velvet Music!
