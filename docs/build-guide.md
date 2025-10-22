# Weight Tracker Build Guide

## Project Stack Snapshot
- **Android Gradle Plugin**: 8.5.1 (see `build.gradle.kts` at the project root).
- **Kotlin**: 1.9.24 via the `org.jetbrains.kotlin.android` plugin.
- **Compile/Target SDK**: 35, minimum SDK 26.
- **Compose**: Enabled with compiler extension 1.5.14.
- **Java Toolchain**: Source and target compatibility are both set to Java 17.
- **Health Connect SDK**: 1.1.0-alpha10 (requires compileSdk 35+).

## Prerequisites
- Android Studio Giraffe (2022.3.1) or newer with the Android SDK 35 platform and build tools installed.
- A working JDK 17 (bundled with modern Android Studio releases; otherwise install separately and set `JAVA_HOME`).
  - **macOS users**: Install via Homebrew: `brew install openjdk@17`
  - After installation, set `JAVA_HOME` before running Gradle: `export JAVA_HOME=/opt/homebrew/opt/openjdk@17`
- (Optional) An Android device or emulator running API 26+ for instrumented tests.

## Building with Android Studio
1. Open Android Studio and choose **File ▸ Open…**, then select the repository root (`weight_tracker`).
2. Allow Gradle to sync; this also materializes the Gradle wrapper if it is missing.
3. Use the **Build Variants** tool window to select the desired variant (default is `app` / `debug`).
4. Trigger **Build ▸ Make Project** to compile everything, or **Run ▸ Run 'app'** to deploy to a connected device/emulator.

## Building from the Command Line
All commands should be executed from the repository root with the Gradle wrapper.

**Important**: Set `JAVA_HOME` before running Gradle commands:
```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17  # macOS with Homebrew
# or export JAVA_HOME=/path/to/your/jdk17
```

Then run the build commands:
```bash
# Assemble the debug APK (outputs to app/build/outputs/apk/debug/app-debug.apk)
./gradlew assembleDebug

# (Optional) Assemble a release APK; configure signing in app/build.gradle.kts or gradle.properties first
./gradlew assembleRelease

# Generate a release bundle suitable for Play Store uploads
./gradlew bundleRelease

# Quick build and install to connected device
./gradlew assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Verification & Quality Gates
Run these before committing or opening a PR to keep CI green and catch regressions early:

```bash
# JVM unit tests (lives under app/src/test)
./gradlew testDebugUnitTest

# Instrumentation tests on a device/emulator (requires one to be available)
./gradlew connectedDebugAndroidTest

# Static analysis (lint) for Compose, XML, and general Android issues
./gradlew lint
```

## Common Build Artifacts
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `app/build/outputs/apk/release/app-release-unsigned.apk`
- App Bundle: `app/build/outputs/bundle/release/app-release.aab`
- Test reports: under `app/build/reports/` (e.g., `tests/testDebugUnitTest/index.html`)
- Lint report: `app/build/reports/lint-results.html`

## Troubleshooting Tips
- Use `./gradlew --no-daemon <task>` if daemon-related cache issues occur.
- Run `./gradlew clean` between major dependency upgrades to clear cached intermediates.
- For Compose preview/rendering issues inside Android Studio, ensure the IDE uses the bundled JDK 17 and re-sync Gradle.
- If Gradle sync fails after pulling changes, invalidate caches via **File ▸ Invalidate Caches / Restart…** in Android Studio.
