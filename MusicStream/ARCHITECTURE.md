# MusicStream — Architecture Guide

## Overview

MusicStream adalah Android Native music player app yang dibangun dari nol, terinspirasi konsep dari MetroList namun dengan implementasi original yang lebih ringan dan sederhana.

```
UI Layer (Jetpack Compose)
    ↕ ViewModels (Hilt + StateFlow)
Domain Layer (Use Cases + Models)
    ↕ Repositories
Data Layer (API + Room + DataStore)
    ↕ ExoPlayer (Media3)
```

---

## Struktur Modul (Single Module)

```
app/
└── com.musicstream/
    ├── data/
    │   ├── api/              # Retrofit + InnertubeApi + DTOs
    │   ├── local/            # Room DB, DAOs, Entities, DataStore
    │   └── repository/       # MusicRepository, SearchRepository, PlaylistRepository
    ├── domain/
    │   ├── model/            # Pure Kotlin domain models
    │   └── usecase/          # PlaySongUseCase, DownloadSongUseCase, ManagePlaylistUseCase
    ├── player/               # MusicService, QueueManager, PlayerController
    ├── download/             # DownloadManager, DownloadWorker (WorkManager)
    ├── cache/                # CacheManager (OkHttp cache + DB eviction)
    ├── recommendation/       # RecommendationEngine
    ├── lyrics/               # LyricsEngine
    ├── ui/
    │   ├── theme/            # Material3 theme, typography, colors
    │   ├── navigation/       # NavGraph (Jetpack Navigation Compose)
    │   ├── screens/          # Home, Search, Player, Queue, Album, Artist,
    │   │                     # Playlist, Library, Lyrics, Settings
    │   └── viewmodel/        # Per-screen ViewModels
    ├── di/                   # Hilt AppModule (Network, DB, ExoPlayer, MediaSession)
    └── util/                 # Extension functions
```

---

## Core Modules

### 1. API Layer (`data/api/`)
- **MusicApiService** — Retrofit interface untuk Innertube (YouTube Music internal API)
- **InnertubeApi** — High-level wrapper: search, browse, player, next, lyrics
- **ApiResult<T>** — Sealed class: Success, Error, Loading — forces exhaustive handling
- **DTOs** — Immutable data classes untuk parsing JSON response

### 2. Repository Pattern (`data/repository/`)
- **MusicRepository** — Cache-first strategy: Room DB → API fallback
- **SearchRepository** — Query dengan search history
- **PlaylistRepository** — Local (Room) + remote playlists

### 3. Player Layer (`player/`)
- **MusicService** — `MediaSessionService` (Media3) yang berjalan sebagai foreground service
- **PlayerController** — Facade: UI ↔ ExoPlayer. Mengelola state sebagai `StateFlow<PlayerState>`
- **QueueManager** — Queue dengan shuffle (Fisher-Yates), repeat ONE/ALL/OFF, history

### 4. Download Manager (`download/`)
- **DownloadManager** — Schedules `WorkManager` jobs, observes progress via `Flow`
- **DownloadWorker** — Hilt-injected Worker: download byte stream → file system → Room

### 5. Cache Manager (`cache/`)
- OkHttp disk cache (50 MB) untuk network responses
- Room TTL-based eviction untuk album/stream cache
- Coil image cache (dikelola otomatis)

### 6. Recommendation Engine (`recommendation/`)
- Primary: API `next` seeds dari current song
- Fallback: shuffle liked songs (offline mode)
- Daily Mix: blend dari 3 random liked song seeds

### 7. Lyrics Engine (`lyrics/`)
- Fetch dari Innertube lyrics endpoint
- In-memory session cache
- Time-sync: line highlighting berdasarkan `positionMs`

---

## State Management

```
PlayerController.playerState: StateFlow<PlayerState>
    ↓ (collected by)
PlayerViewModel.playerState: StateFlow<PlayerState>
    ↓ (collected by)
PlayerScreen, MiniPlayerBar, LyricsScreen, QueueScreen
```

---

## Tech Stack

| Layer | Library |
|-------|---------|
| UI | Jetpack Compose + Material3 |
| DI | Hilt |
| Player | Media3 (ExoPlayer + MediaSession) |
| Network | Retrofit2 + OkHttp4 + Gson |
| Image | Coil 2 |
| DB | Room + Drizzle-inspired schema |
| Prefs | DataStore Preferences |
| BG Jobs | WorkManager + Hilt Worker |
| Async | Coroutines + Flow |
| Nav | Navigation Compose |

---

## Build & Run

```bash
# Clone & open in Android Studio
# Minimum SDK: 26 (Android 8.0)
# Target SDK: 35 (Android 15)
# Kotlin: 2.1.0
# AGP: 8.7.3

./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
```

---

## Fitur Utama

| Fitur | Status | Modul |
|-------|--------|-------|
| Search | ✅ | SearchRepository + SearchScreen |
| Album Browse | ✅ | MusicRepository + AlbumScreen |
| Artist Browse | ✅ | MusicRepository + ArtistScreen |
| Playlist | ✅ | PlaylistRepository + PlaylistScreen |
| Queue | ✅ | QueueManager + QueueScreen |
| Recommendation | ✅ | RecommendationEngine |
| Radio | ✅ | PlayerViewModel.startRadio() |
| Lyrics | ✅ | LyricsEngine + LyricsScreen |
| Download | ✅ | DownloadManager + DownloadWorker |
| Offline Playback | ✅ | DownloadDao + local file URI |
| Background Playback | ✅ | MusicService (foreground) |
| Like Songs | ✅ | SongDao.likeSong() |
| Local Playlists | ✅ | PlaylistRepository |
| Settings | ✅ | PreferencesDataStore + SettingsScreen |
| Search History | ✅ | SearchHistoryDao |
| Stream Cache | ✅ | StreamCacheEntity (TTL-based) |
