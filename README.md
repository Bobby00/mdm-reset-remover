# MDM Reset Remover

Native Android app to remove the "Automatically resetting soon" message on phones enrolled in MDM / Enterprise Enrollment.

## What it does

1. **Checks ADB debugging** — detects whether USB debugging is enabled on the device
2. **Checks Device Owner status** — verifies if this app is set as the device owner (required for system-level changes)
3. **Clears Google Play Services data & cache** — removes data for `com.google.android.gms`
4. **Disables Google Play Store** — hides `com.android.vending`
5. **Downloads & installs Aurora Store** — fetches the latest APK from Aurora OSS and installs it

## Requirements

- **Android 10+** (API 29+)
- **Device Owner** — this app must be set as device owner via ADB to disable apps and clear system data
- **ADB Debugging** — must be enabled on the device to set device owner

## Build

### Prerequisites (macOS)

Install Android SDK command-line tools and set `ANDROID_HOME`:

```bash
# Install via Homebrew
brew install --cask android-commandlinetools

# Accept SDK licenses
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses

# Install required platforms
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager "platforms;android-34" "build-tools;34.0.0"
```

### Build the APK

```bash
cd mdm-reset-remover

# Generate the Gradle wrapper jar (needed once)
gradle wrapper --gradle-version 8.2

# Build debug APK
./gradlew assembleDebug

# The APK will be at:
# app/build/outputs/apk/debug/app-debug.apk
```

### Build release APK

You need a signing keystore. Create one with:

```bash
keytool -genkey -v -keystore mdm-reset-remover.keystore -alias mdm -keyalg RSA -keysize 2048 -validity 10000
```

Then add signing config to `app/build.gradle.kts` and run:

```bash
./gradlew assembleRelease
```

## Setup on Device (required for full features)

1. Enable **Developer Options** (Settings > About Phone > tap Build Number 7×)
2. Enable **USB Debugging** (Settings > System > Developer Options)
3. Connect to computer via USB and authorize ADB
4. Set this app as Device Owner:

   ```bash
   adb shell dpm set-device-owner com.mdmresetremover.app/.DeviceAdminReceiver
   ```

   > ⚠️ Device Owner can only be set on a factory-reset / newly provisioned device, or if no other device owner exists.

5. Install the app and grant all requested permissions

## Aurora Store Download

The app downloads Aurora Store from:
`https://www.auroraoss.com/files/AuroraStore/Release`

You can update the APK URL in `MainActivity.kt` (`AURORA_STORE_APK_URL`) to point to the latest release.

## Landing Page

The landing page is hosted separately on Netlify. See the `landing-page/` directory.

## Disclaimer

This app modifies system-level settings. Use at your own risk. The developer is not responsible for any data loss, device malfunction, or policy violations resulting from use of this app.