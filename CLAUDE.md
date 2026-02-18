# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Ombi Mobile is a native Android app (Kotlin + Jetpack Compose) that serves as a mobile frontend for [Ombi](https://github.com/Ombi-app/Ombi), a self-hosted media request manager. It communicates exclusively with the Ombi REST API — no direct database access.

- **Package:** `com.ombi.mobile`
- **Min SDK:** 29 (Android 10) — all devices support adaptive icons and full Material You
- **Target SDK:** 35
- **Default branch:** `develop` → PRs target `main`

## Build & Run

Open the project root in **Android Studio** (Hedgehog or newer). Android Studio handles Gradle sync, emulator management, and deployment.

From the command line (after Android Studio has synced once):

```bash
# Debug build
./gradlew :app:assembleDebug

# Release build (requires signing config)
./gradlew :app:assembleRelease

# Run all unit tests
./gradlew :app:testDebugUnitTest

# Run a single test class
./gradlew :app:testDebugUnitTest --tests "com.ombi.mobile.SomeTest"

# Lint
./gradlew :app:lint
```

## Architecture

### Layer diagram
```
UI (Compose screens + ViewModels)
        │  StateFlow<UiState>
        ▼
Repository (OmbiRepository, AuthRepository)
        │  suspend funs returning Result<T>
        ▼
API service (Retrofit / OmbiApiService)
        │  OkHttp (auth interceptor + dynamic URL interceptor)
        ▼
Ombi REST API  (/api/v1/* and /api/v2/*)
```

### Dependency Injection
Hilt (`@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltViewModel`). All DI modules are in `di/`:
- `NetworkModule` — provides `Moshi`, `OkHttpClient`, `Retrofit`, `OmbiApiService`

Repositories, `AuthManager`, and `UserPreferences` are `@Singleton` with `@Inject constructor` — no explicit module bindings needed.

### Dynamic server URL
The user configures the Ombi server URL in Settings. The `DynamicUrlInterceptor` (inside `NetworkModule`) reads the URL from `UserPreferences.getServerUrlSync()` on every OkHttp request and rewrites the host. Retrofit's base URL is a placeholder (`http://localhost/`).

### Auth
`AuthManager` stores the JWT token in `EncryptedSharedPreferences` (Android Keystore / AES256-GCM). The `authInterceptor` in `NetworkModule` attaches `Authorization: Bearer <token>` to every request. `AuthManager.isLoggedIn` is a `StateFlow<Boolean>` that drives the nav graph start destination.

### Navigation
Single-activity. Two-level navigation:
1. **Outer `NavHost`** (`OmbiNavGraph`): `server_setup` → `login` → `main`
2. **Inner `NavHost`** (`MainScreen`): `home` / `search` / `requests` / `settings` behind a `NavigationBar`

### UI patterns
- Each screen owns a `*ViewModel` (Hilt) and a `*UiState` data class
- ViewModels expose a single `StateFlow<UiState>` collected with `collectAsState()`
- `async { } + await()` is used in ViewModels to fan-out parallel API calls
- Debounced search (400 ms) via `Flow.debounce` in `SearchViewModel`

### Key shared components
| Component | Location |
|---|---|
| `MediaCard` | `ui/components/MediaCard.kt` — poster card with optional status badge |
| `StatusBadge` | `ui/components/StatusBadge.kt` — color-coded availability chip |
| `MediaRow` | inside `MediaCard.kt` — labeled horizontal scroll row |

### Ombi API reference
| Feature | Endpoint |
|---|---|
| Login | `POST /api/v1/token` |
| Plex login | `POST /api/v1/token/plextoken` |
| Current user | `GET /api/v1/identity` |
| Multi-search | `GET /api/v2/search/multi/{term}` |
| Popular/Upcoming/Trending | `GET /api/v2/search/movie\|tv/{category}/{pos}/{count}` |
| Recently added | `GET /api/v1/recentlyadded/movies` + `/tv` |
| Request movie | `POST /api/v2/requests/movie` |
| Request TV | `POST /api/v2/requests/tv` |
| List requests | `GET /api/v2/requests/movie\|tv/{count}/{pos}/{sort}/{order}` |
| Cancel request | `DELETE /api/v1/request/movie\|tv/{id}` |

TMDB poster images are not served by Ombi directly — prepend `https://image.tmdb.org/t/p/w342` to the `poster` field returned by the API.

## Key dependencies (see `gradle/libs.versions.toml`)
- Compose BOM 2024.12.01 + Material 3
- Navigation Compose 2.8.5
- Hilt 2.53.1 + KSP 2.1.0-1.0.29
- Retrofit 2.11.0 + Moshi 1.15.2 (KSP codegen)
- Coil 2.7.0 (image loading + disk cache)
- DataStore Preferences 1.1.2 (server URL, theme)
- Security-Crypto 1.1.0-alpha06 (EncryptedSharedPreferences for JWT)
