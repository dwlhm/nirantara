# Nirantara Launcher

[![CI Status](https://github.com/dwlhm/nirantara/actions/workflows/ci.yml/badge.svg)](https://github.com/dwlhm/nirantara/actions/workflows/ci.yml)
[![GitHub Release](https://img.shields.io/github/v/release/dwlhm/nirantara?include_prereleases&style=flat-square)](https://github.com/dwlhm/nirantara/releases)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-26%20(Android%208.0)-blue.svg?style=flat-square)](https://developer.android.com/about/versions/oreo)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-35%20(Android%2015)-green.svg?style=flat-square)](https://developer.android.com/about/versions/15)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202024.11.00-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg?style=flat-square)](LICENSE)

**Nirantara** is a minimalist, ergonomic, and blazingly fast Android launcher engineered from the ground up with 100% Kotlin and Jetpack Compose. Built with an ergonomic wave alphabet scrollbar and thumb-first interaction architecture, Nirantara empowers one-handed accessibility and fluid gesture navigation on modern Android devices.

---

## ✨ Features

- 🌊 **Wave Alphabet Scrollbar**: Physics-based radial wave scrolling engine designed for seamless one-handed navigation through all installed applications.
- ⚡ **Ultra Lightweight & Fast**: Clean architecture with minimal footprint, hardware-accelerated animations, and zero clutter.
- 🎨 **Adaptive Material Theming**: Dynamic wallpaper color extraction, AMOLED Pure Black mode, Light/Dark system synchronization, and custom solid palettes.
- 🔍 **Instant Smart Search**: Fuzzy app indexing, learning-based recent search history, and fallback web query integration.
- 📌 **Pinned Favorites & Quick Launch**: Direct home-screen access to primary apps and custom quick launch configurations.
- 🧩 **Pop-up & Header Widgets**: Embed AppWidgets directly on your home header or bind contextual pop-up widgets to individual applications.
- 🔒 **Hidden Apps Management**: Easily curate your application drawer and hide unused system utilities or sensitive apps with complete privacy.
- 📳 **Haptic Wave Dynamics**: Configurable tactile vibrations providing physical tactile feedback as you glide across the alphabet index.

---

## 📲 Download & Installation

You can grab the latest production-ready APK directly from GitHub Releases:

1. Navigate to the **[Releases page](https://github.com/dwlhm/nirantara/releases)**.
2. Download the latest `Nirantara-v*.apk` asset.
3. Open the APK file on your Android device (Android 8.0 Oreo or higher required).
4. Follow system prompts to allow installation from unknown sources if needed.
5. Set Nirantara as your default Home application in device Settings.

---

## 🛠️ Building from Source

### Prerequisites

- **JDK**: Java Development Kit 17 (Eclipse Temurin recommended)
- **Android SDK**: Build Tools `35.0.0`, Compile SDK `35`, Platform API `35`
- **Gradle**: 8.11.1 (included via Gradle Wrapper `./gradlew`)

### Build Steps

1. **Clone the repository:**
   ```bash
   git clone https://github.com/dwlhm/nirantara.git
   cd nirantara
   ```

2. **Run lint and unit tests:**
   ```bash
   ./gradlew lintDebug testDebugUnitTest
   ```

3. **Build Debug APK:**
   ```bash
   ./gradlew assembleDebug
   ```
   The generated APK will be available at `app/build/outputs/apk/debug/app-debug.apk`.

4. **Build Release APK & App Bundle (AAB):**
   ```bash
   ./gradlew assembleRelease bundleRelease
   ```
   For configuring keystore credentials, see the [Release Signing Guide](#-release-signing-configuration).

---

## 🔐 Release Signing Configuration

Release workflows support automated signing via GitHub Actions Secrets:

| Secret Name | Description |
| :--- | :--- |
| `KEYSTORE_BASE64` | Base64-encoded release keystore `.jks` file |
| `KEYSTORE_PASSWORD` | Password for the release keystore |
| `KEY_ALIAS` | Alias of the signing key inside the keystore |
| `KEY_PASSWORD` | Password for the signing key |

If these secrets are not configured in repository settings, the CI/CD pipeline falls back to debug keystore signing so artifact generation never breaks.

---

## 🤝 Contributing

We welcome contributions from the community! Please read our **[CONTRIBUTING.md](CONTRIBUTING.md)** guide to understand our branch strategy, Conventional Commits standard, code styling rules, and pull request workflow.

Please also review our **[CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md)** before participating.

---

## 🛡️ Security

To report security vulnerabilities, please refer to our **[SECURITY.md](SECURITY.md)** policy.

---

## 📄 License

```text
Copyright 2026 Nirantara Contributors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
