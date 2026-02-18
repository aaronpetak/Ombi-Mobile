# Ombi Mobile

A native Android app for [Ombi](https://github.com/Ombi-app/Ombi) — a self-hosted media request manager. Browse, search, and request movies and TV shows from your phone with a polished Material You interface.

## Features

- **Search** — Search movies and TV shows simultaneously with real-time results and availability status
- **One-tap requests** — Request a movie or full TV series instantly; select specific seasons/episodes
- **My Requests** — View, track, and cancel your pending requests with pull-to-refresh
- **Home / Discover** — Recently added content and trending/popular/upcoming rows
- **Multi-server** — Configurable server URL; works with any Ombi instance
- **Auth** — Ombi local account login and Plex token login; sessions persist between launches
- **Material You** — Dynamic color theming, dark/light mode, bottom navigation

## Requirements

- Android 10 (API 29) or higher
- A running [Ombi](https://github.com/Ombi-app/Ombi) instance accessible from your device

## Getting Started

### 1. Clone the repo

```bash
git clone https://github.com/your-username/Ombi-Mobile.git
cd Ombi-Mobile
```

### 2. Open in Android Studio

Open the project root in **Android Studio Hedgehog (2023.1.1) or newer**. Android Studio will handle Gradle sync and SDK setup automatically.

### 3. Run

Select a device or emulator and press **Run**. On first launch, enter your Ombi server URL (e.g. `https://ombi.example.com`), then sign in with your Ombi or Plex credentials.

## Build

```bash
# Debug APK
./gradlew :app:assembleDebug

# Release APK (requires signing configuration)
./gradlew :app:assembleRelease
```

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Navigation | Compose Navigation |
| Networking | Retrofit 2 + OkHttp + Moshi |
| Image loading | Coil |
| Dependency injection | Hilt |
| Auth storage | EncryptedSharedPreferences (Android Keystore) |
| Preferences | Jetpack DataStore |
| Async | Kotlin Coroutines + Flow |

## Architecture

MVVM with a Repository pattern. Each screen has a dedicated ViewModel that exposes a single `StateFlow<UiState>`. The data layer communicates exclusively with the Ombi REST API — no direct database access.

```
UI (Compose) → ViewModel → Repository → Retrofit API → Ombi
```

See [CLAUDE.md](CLAUDE.md) for a full architecture reference and API endpoint map.

## License

[GNU General Public License v3.0](LICENSE)
