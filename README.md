# Steam Manifest Downloader (Android Edition) 🎮📱

An Android port of **[Steam-Manifest-Downloader](https://github.com/MCbabel/Steam-Manifest-Downloader)**, designed and built natively for Android using Kotlin and Jetpack Compose.

It brings the exact 5-step desktop workflow directly to Android devices, with the requested custom enhancement: **interactive server selection** (allowing you to choose which server/source to download manifests and keys from).

---

## ✨ Features (Ported from Desktop SMD)

| Feature | Desktop AppImage / SMD | Android Edition |
|---|---|---|
| 📂 **Upload Manifests** | Drag & drop `.lua` or `.st` files | Android system file picker & native intent filter (`.lua`, `.st`) |
| 🔍 **Search by App ID / Name** | Steam Store Search & App ID lookup | Built-in Steam Store API & Steamcmd PICS search |
| 🌐 **Choose Download Server** | Queries in priority order | **Direct Server Selector** on search screen + Dedicated Server Manager |
| 🏷️ **Depot Metadata** | Windows / Linux / macOS, 32/64-bit, language tags | Live depot metadata tags via Steam PICS mirror |
| 🧭 **Like Steam Mode** | Auto-selects base game depots for platform/language | One-tap "Like Steam" auto-depot filter button |
| 📊 **Real-time Download Engine** | Rust steam-vent & chunk downloader | Foreground Service with notifications, speed calculation & ETA |
| 🔐 **Decryption & Decompression** | AES-256-ECB depot key chunk decryption | Native Java/Kotlin AES-256-ECB chunk decryptor & zlib decompressor |
| 📋 **Download Queue** | Multi-game queue across restarts | Persistent Queue stored via Android DataStore |
| 🗂️ **History** | Cover art, search, status filter | Persistent History with Steam grid art & status indicators |
| 🐢 **Speed Limit** | Cap bandwidth (e.g. 10 MB/s) | Configurable speed throttle in Settings |
| 🌐 **Proxy Support** | SOCKS5 / HTTP proxy with test button | OkHttp proxy integration with real-time test button |
| 🌙 **Theme** | Dark & Light Steam-inspired theme | Material 3 Steam Dark & Light themes |

---

## 🚀 Added Feature: Choose Which Server to Download From

You can choose your download source dynamically:
1. **Auto Mode**: Queries all enabled sources in priority order.
2. **Specific Server Mode**: Pick a specific server from the dropdown before searching:
   - **ManifestHub (archive.org)**: The complete archive of manifests & keys
   - **GitHub – SteamAutoCracks**: GitHub-hosted .lua repository
   - **ManifestHub API**: Direct API with your ManifestHub token
   - **Hubcap**: Hubcap API with custom key
   - **Ryuu**: Ryuu generator API
   - **Custom Servers**: Add your own plain HTTPS or custom manifest API endpoints in **Settings → Manage Download Sources**.

---

## 🛠️ Project Structure

```
SteamManifestDownloader/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml          # Permissions, foreground service, intent filters
│   │   ├── java/com/smd/android/
│   │   │   ├── MainActivity.kt          # Compose root & intent receiver
│   │   │   ├── model/
│   │   │   │   └── Models.kt            # DepotEntry, ParsedManifest, AppSettings, etc.
│   │   │   ├── network/
│   │   │   │   └── SteamApiClient.kt    # Store API, PICS, Archive.org, GitHub, Ryuu, Hubcap
│   │   │   ├── repository/
│   │   │   │   └── Repositories.kt      # DataStore preferences & history storage
│   │   │   ├── service/
│   │   │   │   └── DownloadService.kt   # Foreground download service with AES-ECB decryption
│   │   │   ├── ui/
│   │   │   │   ├── navigation/AppNavHost.kt
│   │   │   │   ├── screens/
│   │   │   │   │   ├── UploadScreen.kt         # Step 1: Upload or Search with Server Selector
│   │   │   │   │   ├── DepotSelectScreen.kt    # Step 2: Depot selection & Like Steam mode
│   │   │   │   │   ├── DownloadScreen.kt       # Step 3: Progress, speed, ETA, and live logs
│   │   │   │   │   ├── HistoryScreen.kt        # Download history with covers
│   │   │   │   │   ├── QueueScreen.kt          # Download queue management
│   │   │   │   │   ├── SettingsScreen.kt       # Speed limit, proxies, theme, keys
│   │   │   │   │   └── ServerManagerScreen.kt  # Add, reorder & configure servers
│   │   │   │   └── theme/Theme.kt              # Steam dark/light theme
│   │   │   ├── util/
│   │   │   │   ├── FormatUtil.kt               # Byte, speed, and time formatters
│   │   │   │   └── ManifestParsers.kt          # .lua and .st manifest parsers
│   │   │   └── viewmodel/MainViewModel.kt      # Unified state management
│   │   └── res/                                # Adaptive icons, strings, themes
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/
│   ├── libs.versions.toml                      # Version catalog
│   └── wrapper/gradle-wrapper.properties
├── build.gradle.kts
└── settings.gradle.kts
```

---

## 📦 How to Build the APK

### Prerequisites:
- Android SDK (API 35)
- Java 17 or Java 21
- Gradle 8.11+ (or use Android Studio)

### Build Commands:
```bash
cd SteamManifestDownloader

# Build Debug APK:
./gradlew assembleDebug

# Output APK will be at:
# app/build/outputs/apk/debug/app-debug.apk
```

### Or Open in Android Studio:
1. Open **Android Studio**.
2. Select **Open an Existing Project** and choose the `/home/deck/Downloads/gg/SteamManifestDownloader` directory.
3. Click **Build → Build Bundle(s) / APK(s) → Build APK(s)** or click the **Run** button to launch on an Android device or emulator.
