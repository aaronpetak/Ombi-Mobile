# Ombi Mobile

A native Android client for [Ombi](https://github.com/Ombi-app/Ombi) — a self-hosted, open-source media request management platform. Ombi Mobile connects to **any Ombi server** by URL, giving users a polished Material You interface to browse content, submit requests, and track their queue from anywhere.

> **Generic by design** — the app stores only your server URL and a JWT token. There is no proprietary backend. All data comes directly from your own Ombi instance.

---

## Table of Contents

- [Features](#features)
- [Screenshots](#screenshots)
- [Requirements](#requirements)
- [Getting Started](#getting-started)
- [Building](#building)
- [Installing on a Device](#installing-on-a-device)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [API Reference](#api-reference)
- [Contributing](#contributing)
- [License](#license)

---

## Features

### Discovery
- **Home / Discover screen** with five curated content rows loaded in parallel:
  - Recently Added Movies
  - Recently Added TV Shows
  - Popular Movies
  - Trending TV Shows
  - Upcoming Movies
- **Pull-to-refresh** on the Home screen to reload all rows simultaneously.

### Search
- **Real-time multi-search** across movies and TV shows with a 400 ms debounce to minimise API calls.
- Minimum 2-character threshold before a search fires.
- **Filter chips** to narrow results to All / Movies only / TV only.
- Poster grid layout (adaptive columns, minimum 110 dp per card).
- Availability and request status loaded in the background after opening a result card so the sheet appears instantly.

### Media Detail
- **Bottom sheet** with poster image, title, release year, media type, rating, and overview.
- Colour-coded **status badge**: Available (green) · Requested (blue) · Processing (orange) · Denied (red).
- **One-tap request** for movies and full TV series.
- Request button is disabled automatically when the item is already available, pending, or denied.
- Inline success / failure message after submitting a request.

### My Requests
- View **all of your requests** — movies and TV shows — in a single screen.
- **Primary tab**: Movies vs. TV
- **Secondary tab**: Pending (in-progress) vs. Processed (available or denied), each showing a live count.
- **Pull-to-refresh** to sync with the server.
- **Admin users** see a delete icon on pending requests to cancel them directly from the app.

### Settings
- **Account info** — displays your logged-in username and current server URL.
- **Appearance** — choose between System default, Dark, and Light themes. Change takes effect immediately without restarting the app.
- **Sign Out** with a confirmation dialog. Clears your stored token securely.

### Authentication & Server Setup
- **One-time server setup** on first launch — enter any Ombi server URL (`https://ombi.example.com`).
- "Change Server" link on the login screen to point the app at a different instance.
- **Ombi local account login** — username and optional password (some accounts have no password).
- JWT token stored securely in **EncryptedSharedPreferences** (backed by Android Keystore).
- Session persists across app restarts; no re-login needed until you sign out.

---

## Screenshots

*(Add screenshots here)*

---

## Requirements

| Requirement | Minimum |
| --- | --- |
| Android version | Android 10 (API 29) |
| Ombi version | v4.x (API v1 and v2) |
| Network | HTTPS recommended; HTTP supported |

---

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/your-username/ombi-frontend.git
cd ombi-frontend
```

### 2. Open in Android Studio

Open the project root in **Android Studio Hedgehog (2023.1.1) or newer**. Gradle sync and SDK setup happen automatically.

### 3. Run on an emulator or device

Select a target in the device picker and press **Run ▶**. On first launch:

1. Enter your Ombi server URL (e.g. `https://ombi.example.com`).
2. Sign in with your Ombi username and password (password may be left blank for passwordless accounts).

---

## Building

```bash
# Debug APK
./gradlew :app:assembleDebug

# Release APK (requires keystore signing configuration in app/build.gradle.kts)
./gradlew :app:assembleRelease

# Run unit tests
./gradlew :app:test

# Run lint
./gradlew :app:lint
```

Output APKs are written to `app/build/outputs/apk/`.

---

## Installing on a Device

**Via ADB (debug build):**

```bash
# Build and install directly to a connected device
./gradlew :app:installDebug

# Or install an existing APK
adb install app/build/outputs/apk/debug/app-debug.apk
```

**Via Android Studio:**

Select **Run > Run 'app'** with a physical device connected over USB or Wi-Fi (ensure USB debugging is enabled in Developer Options).

---

## Architecture

Ombi Mobile follows **MVVM with a Repository pattern**, the recommended Android architecture for Compose applications.

```
UI Layer            Data Layer
─────────────────   ──────────────────────────────────────────
Composable Screen ──▶ ViewModel (StateFlow<UiState>)
                           │
                           ▼
                       Repository
                           │
                      ┌────┴────┐
                      │        │
                  API Service  Preferences / Auth
                  (Retrofit)   (DataStore + EncryptedSharedPrefs)
                      │
                      ▼
                  Ombi Server
```

### Key principles

- **Single source of truth**: Each screen has one `UiState` data class. The ViewModel updates it atomically; composables read it reactively via `collectAsState()`.
- **Unidirectional data flow**: Events flow up (user taps → ViewModel function). State flows down (ViewModel → UI).
- **Repository abstraction**: Composables and ViewModels never call Retrofit directly. All network access goes through `OmbiRepository` or `AuthRepository`, which wrap results in `Result<T>`.
- **Concurrent loading**: `async { } / await()` fan-out pattern for pages that load multiple independent data sources simultaneously (Home loads 5 rows in parallel; Requests loads movies, TV, and user profile in parallel).
- **Dynamic base URL**: The Retrofit base URL is a placeholder. An OkHttp interceptor rewrites the host/port on every request using the value stored in DataStore, so the user can change servers without reinstalling or restarting.

---

## Tech Stack

| Layer | Technology |
| --- | --- |
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Navigation | Compose Navigation |
| Networking | Retrofit 2 + OkHttp 4 + Moshi |
| Image loading | Coil |
| Dependency injection | Hilt (Dagger) |
| Auth token storage | EncryptedSharedPreferences (Android Keystore) |
| User preferences | Jetpack DataStore (Proto) |
| Async / concurrency | Kotlin Coroutines + Flow |
| Build system | Gradle (Kotlin DSL) |

---

## Project Structure

```
app/src/main/java/com/ombi/mobile/
├── OmbiApplication.kt          # Hilt application entry point
├── MainActivity.kt             # Single-activity host; applies theme from preferences
│
├── data/
│   ├── api/
│   │   ├── OmbiApiService.kt   # Retrofit interface (all endpoints)
│   │   └── models/             # Moshi-annotated API request/response models
│   │       ├── AuthModels.kt
│   │       ├── RequestModels.kt
│   │       ├── SearchModels.kt
│   │       ├── RecentlyAddedModels.kt
│   │       └── UserModels.kt
│   ├── auth/
│   │   └── AuthManager.kt      # Token + username in EncryptedSharedPreferences
│   ├── preferences/
│   │   └── UserPreferences.kt  # Server URL + theme in DataStore
│   └── repository/
│       ├── AuthRepository.kt   # Login / logout
│       └── OmbiRepository.kt   # All content + request operations
│
├── di/
│   └── NetworkModule.kt        # Hilt module: Moshi, OkHttp, Retrofit, OmbiApiService
│
└── ui/
    ├── components/
    │   └── MediaCard.kt        # Reusable poster card + horizontal row
    ├── model/
    │   └── MediaItem.kt        # Unified UI model for movies and TV shows
    ├── navigation/
    │   ├── Screen.kt           # Sealed class of routes + BottomNavItem enum
    │   ├── NavGraph.kt         # Root nav graph (ServerSetup → Login → Main)
    │   ├── MainScreen.kt       # Bottom-nav shell with 4 tabs
    │   └── NavViewModel.kt     # Minimal VM to inject UserPreferences into NavGraph
    ├── screens/
    │   ├── home/               # HomeScreen + HomeViewModel
    │   ├── search/             # SearchScreen + SearchViewModel
    │   ├── requests/           # RequestsScreen + RequestsViewModel
    │   ├── settings/           # SettingsScreen + SettingsViewModel
    │   ├── login/              # LoginScreen + LoginViewModel
    │   ├── serversetup/        # ServerSetupScreen + ServerSetupViewModel
    │   └── detail/             # MediaDetailSheet (shared bottom sheet)
    └── theme/
        ├── Color.kt            # Brand colour tokens
        ├── Type.kt             # Typography scale
        └── Theme.kt            # OmbiTheme composable (Material You + static fallbacks)
```

---

## API Reference

Ombi Mobile uses the **Ombi V1 and V2 REST APIs**. No API key is required — all calls are authenticated with the JWT returned by the login endpoint.

### Authentication

| Method | Endpoint | Description |
| --- | --- | --- |
| POST | `/api/v1/token` | Obtain a JWT using username + password |

### User / Identity

| Method | Endpoint | Description |
| --- | --- | --- |
| GET | `/api/v1/identity` | Get current user profile (used to detect admin role) |

### Search

| Method | Endpoint | Description |
| --- | --- | --- |
| POST | `/api/v2/search/multi/{term}` | Search movies and TV simultaneously |
| GET | `/api/v2/search/movie/{theMovieDbId}` | Movie detail with request status |
| GET | `/api/v2/search/Tv/moviedb/{theMovieDbId}` | TV detail by TMDB ID |
| GET | `/api/v2/search/tv/{tvDbId}` | TV detail by TVDb ID |

### Discover

| Method | Endpoint | Description |
| --- | --- | --- |
| GET | `/api/v2/search/movie/popular/{pos}/{count}` | Popular movies |
| GET | `/api/v2/search/movie/upcoming/{pos}/{count}` | Upcoming movies |
| GET | `/api/v2/search/tv/trending/{pos}/{count}` | Trending TV shows |
| GET | `/api/v1/recentlyadded/movies` | Recently added movies |
| GET | `/api/v1/recentlyadded/tv` | Recently added TV shows |

### Requests

| Method | Endpoint | Description |
| --- | --- | --- |
| POST | `/api/v1/request/movie` | Submit a movie request |
| GET | `/api/v2/requests/movie/{count}/{pos}/{sort}/{order}` | List movie requests |
| DELETE | `/api/v1/request/movie/{requestId}` | Cancel a movie request |
| POST | `/api/v2/requests/tv` | Submit a TV request |
| GET | `/api/v2/requests/tv/{count}/{pos}/{sort}/{order}` | List TV requests |
| DELETE | `/api/v1/request/tv/{parentRequestId}` | Cancel a TV request (uses **parent** ID) |

> **Note on TV deletion**: The DELETE endpoint expects the *parent* request ID (the top-level show), not the child/season ID. The app resolves this automatically from `parentRequest.id`.

---

## Contributing

1. Fork the repository and create a feature branch off `develop`.
2. Make your changes with clear, descriptive commits.
3. Open a pull request targeting `develop`. A maintainer will review and merge to `main` for releases.

---

## License

[GNU General Public License v3.0](LICENSE)
