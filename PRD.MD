# Sonara — Product Requirements Document (PRD)

## 1. Overview

**Product Name:** Sonara

**Tagline:** Personal Sound Engine

**Platform:** Android (min SDK 26 / Android 8.0, target SDK 34 / Android 14)

**Version:** 1.0.0

Sonara is a next-generation mobile audio application that understands the user's music, recognizes their headphones, and optimizes sound in real-time. It combines intelligent track analysis (via Last.fm and a local AI fallback), automatic headphone correction (AutoEQ), a professional-grade 10-band equalizer, and a rich preset system into a single, seamless audio experience.

---

## 2. Problem Statement

Smartphone users listen to music through a wide variety of headphones and speakers, each with different frequency response characteristics. Most listeners lack the expertise or patience to manually adjust equalizer settings for every song, headphone, and listening context. Existing solutions are either too simplistic (single-slider bass boost) or too complex (studio-grade parametric EQ with no guidance).

**Sonara solves this by:**
- Automatically detecting the connected headphone model and applying a correction profile.
- Analyzing the currently playing track's genre, mood, and energy to generate intelligent EQ suggestions.
- Providing a curated library of 23 built-in presets organized by genre, mood, and use-case.
- Exposing a full 10-band equalizer for users who want manual control.
- Operating transparently in the background via a foreground service.

---

## 3. Goals & Success Metrics

### Goals
| # | Goal | Description |
|---|------|-------------|
| G1 | Effortless audio optimization | Users should hear improved sound within seconds of installing the app, with zero configuration. |
| G2 | Headphone-aware processing | The app should detect and compensate for the connected headphone's frequency response automatically. |
| G3 | Context-aware EQ | EQ settings should adapt to the genre, mood, and energy of the currently playing track. |
| G4 | Power-user flexibility | Advanced users should have full manual control over a 10-band EQ, preamp, bass boost, virtualizer, and loudness. |
| G5 | Minimal battery impact | Background processing should be lightweight and not noticeably affect battery life. |

### Success Metrics
| Metric | Target |
|--------|--------|
| Time to first optimized playback | < 5 seconds after app launch |
| Headphone detection accuracy | ≥ 90% for the 26 supported models |
| Track genre classification accuracy | ≥ 80% (when Last.fm data available) |
| Crash-free rate | ≥ 99.5% |
| User-created presets per active user | ≥ 1 within first week |

---

## 4. Target Users

### Primary Persona — "The Everyday Listener"
- Listens to 1–3 hours of music daily on a smartphone.
- Uses consumer Bluetooth headphones (AirPods, Galaxy Buds, Sony WH-series).
- Wants better sound but doesn't know how to configure an EQ.
- Values a "set-it-and-forget-it" experience.

### Secondary Persona — "The Audiophile Tinkerer"
- Owns multiple pairs of headphones/IEMs.
- Understands frequency response and EQ concepts.
- Wants per-headphone profiles and fine-grained control.
- Appreciates transparency (insights, confidence scores, AI reasoning).

### Tertiary Persona — "The Podcast / Vocal Listener"
- Primarily listens to podcasts, audiobooks, or vocal music.
- Needs vocal clarity enhancement and speech-optimized presets.
- Values simple preset switching (e.g., "Podcast", "Vocal").

---

## 5. Features & Requirements

### 5.1 Intelligent Sound Processing

**Description:** The core differentiator — Sonara automatically analyzes the currently playing track and generates EQ adjustments based on genre, mood, and energy.

| ID | Requirement | Priority |
|----|------------|----------|
| F1.1 | Detect currently playing track (title, artist, album, artwork) via `NotificationListenerService`. | P0 |
| F1.2 | Resolve track metadata through a multi-source pipeline: Last.fm track lookup → Last.fm artist fallback → Local AI analyzer. | P0 |
| F1.3 | Classify tracks into one of 14 genres: Rock, Pop, Hip-Hop, Electronic, R&B, Jazz, Classical, Metal, Country, Folk, Blues, Reggae, Latin, Other. | P0 |
| F1.4 | Determine track mood: Energetic, Chill, Melancholic, Happy, Intense, Romantic, Neutral. | P0 |
| F1.5 | Compute energy level on a 0.0–1.0 scale derived from tags and heuristics. | P1 |
| F1.6 | Assign a confidence score (0.0–1.0) based on Last.fm tag count and diversity. | P1 |
| F1.7 | Cache analysis results locally with a 30-day TTL to avoid redundant API calls. | P0 |
| F1.8 | Generate AI EQ suggestions (per-band adjustments) based on detected genre, mood, and energy. | P0 |
| F1.9 | Apply EQ adjustments via the layered pipeline: AutoEQ → Preset → AI Adjustment → Safety Limiter. | P0 |
| F1.10 | Provide a user toggle to enable/disable AI-driven EQ adjustments. | P1 |

### 5.2 10-Band Equalizer

**Description:** A professional-grade equalizer with real-time control and visualization.

| ID | Requirement | Priority |
|----|------------|----------|
| F2.1 | Provide 10 frequency bands: 31 Hz, 62 Hz, 125 Hz, 250 Hz, 500 Hz, 1 kHz, 2 kHz, 4 kHz, 8 kHz, 16 kHz. | P0 |
| F2.2 | Allow per-band gain adjustment from -12 dB to +12 dB. | P0 |
| F2.3 | Provide a preamp control from -12 dB to +12 dB. | P0 |
| F2.4 | Display a real-time frequency response curve. | P1 |
| F2.5 | Support smooth animated transitions between profiles (1-second duration, 20 steps). | P2 |
| F2.6 | Implement a safety limiter that prevents clipping when combined gain exceeds 10 dB. | P0 |
| F2.7 | Provide Bass Boost effect (0–1000 strength). | P1 |
| F2.8 | Provide Virtualizer effect (0–1000 strength). | P1 |
| F2.9 | Provide Loudness Enhancer effect (0–2000 gain). | P1 |

### 5.3 AutoEQ — Headphone Detection & Correction

**Description:** Automatically detect connected headphones and apply frequency-correction profiles to flatten or improve their response.

| ID | Requirement | Priority |
|----|------------|----------|
| F3.1 | Detect connected audio output devices (wired 3.5 mm, Bluetooth A2DP, Bluetooth LE, USB-C). | P0 |
| F3.2 | Read device name via Android `AudioManager` API. | P0 |
| F3.3 | Match device name against a database of 26+ headphone correction profiles. | P0 |
| F3.4 | Support exact match (confidence 0.9) and fuzzy/partial match (confidence 0.5). | P1 |
| F3.5 | Store per-headphone 10-band correction values. | P0 |
| F3.6 | Listen for headphone connect/disconnect events and update the active profile accordingly. | P0 |
| F3.7 | Provide a user toggle to enable/disable AutoEQ. | P1 |

**Supported Headphone Profiles (v1.0):**

| Brand | Models |
|-------|--------|
| Apple | AirPods Max, AirPods Pro, AirPods 2nd Gen |
| Sony | WH-1000XM4, WH-1000XM5, WF-1000XM4, WF-1000XM5 |
| Samsung | Galaxy Buds Pro, Galaxy Buds 2 |
| Sennheiser | HD 600, HD 650 |
| Beyerdynamic | DT 770, DT 990 |
| Audio-Technica | ATH-M50X |
| JBL | Tune 510 |
| Bose | QuietComfort, 700 |
| Beats | Solo, Studio |
| Google | Pixel Buds |
| Nothing | Ear |
| Moondrop | Aria |
| KZ | ZSN Pro |
| Xiaomi | Buds |
| Huawei | FreeBuds |

### 5.4 Preset System

**Description:** A library of curated and user-created audio profiles for one-tap sound customization.

| ID | Requirement | Priority |
|----|------------|----------|
| F4.1 | Ship 23 built-in presets across 8 categories (Neutral, Bass, Treble, Fun, Vocal, Genre, Mood, Device). | P0 |
| F4.2 | Allow users to create custom presets from the current EQ state. | P0 |
| F4.3 | Allow users to duplicate, rename, and delete custom presets. | P1 |
| F4.4 | Support favoriting/starring presets. | P2 |
| F4.5 | Track recently used presets (LRU). | P2 |
| F4.6 | Filter presets by category. | P1 |
| F4.7 | Export presets as JSON. | P2 |
| F4.8 | Import presets from JSON. | P2 |
| F4.9 | Compose final EQ output by blending Preset + AutoEQ + AI Suggestion bands via `ProfileComposer`. | P0 |

**Built-In Preset Catalog:**

| Category | Presets |
|----------|---------|
| Neutral | Flat |
| Bass | Bass Boost, Bass Heavy |
| Treble | Treble Boost |
| Fun | V-Shape, Loudness |
| Vocal | Vocal, Podcast |
| Genre | Rock, Pop, Hip-Hop, Jazz, Classical, Electronic, R&B, Acoustic |
| Mood | Late Night, Morning, Workout, Chill |
| Device | Small Speaker, Headphone, Earbud Fix |

### 5.5 Insights Dashboard

**Description:** A transparency layer that shows users exactly what Sonara is doing and why.

| ID | Requirement | Priority |
|----|------------|----------|
| F5.1 | Display the current sound pipeline state: active preset, all 10 band values, preamp, and active effects. | P1 |
| F5.2 | Show the detected genre, mood, energy level (0–100%), and confidence score (0–100%). | P1 |
| F5.3 | Show the analysis source (Last.fm, Last.fm Artist, Local AI, Cache). | P2 |
| F5.4 | Show AI reasoning text (e.g., "Hip-Hop: Enhanced sub-bass and presence"). | P2 |
| F5.5 | Display a clipping warning when the safety limiter is active. | P1 |
| F5.6 | Show cache statistics (total cached tracks, storage usage) with a "Clear Cache" action. | P2 |
| F5.7 | Render a frequency response curve graph. | P1 |

### 5.6 Now Playing Integration

**Description:** Persistent background service that monitors media playback across all music apps.

| ID | Requirement | Priority |
|----|------------|----------|
| F6.1 | Run a foreground service (`SonaraService`) that survives app backgrounding. | P0 |
| F6.2 | Display a persistent notification showing current track info and a "Stop" action. | P0 |
| F6.3 | Monitor media player notifications via `NotificationListenerService`. | P0 |
| F6.4 | Extract track title, artist, album, and artwork from notifications. | P0 |
| F6.5 | Detect playback state changes (playing / paused). | P1 |
| F6.6 | Broadcast now-playing updates via Kotlin Flow for real-time UI updates. | P0 |

### 5.7 UI & Theming

**Description:** A modern, dark-first UI built with Jetpack Compose and Material 3.

| ID | Requirement | Priority |
|----|------------|----------|
| F7.1 | Implement 5 main screens: Dashboard, Equalizer, Presets, Insights, Settings. | P0 |
| F7.2 | Use Jetpack Compose with Material 3 components. | P0 |
| F7.3 | Support 8 accent colors: Amber (default), Red, Green, Blue, Cyan, Purple, Orange, Pink. | P1 |
| F7.4 | Support Monet dynamic theming on Android 12+ (auto-generate palette from wallpaper). | P2 |
| F7.5 | Optimize for dark theme. | P0 |
| F7.6 | Render an animated real-time audio visualizer. | P2 |
| F7.7 | Use edge-to-edge layout (full-screen, no status-bar padding). | P1 |
| F7.8 | Apply Fluent Design-inspired glass-morphism styling to card components. | P2 |

### 5.8 Settings

**Description:** User-configurable preferences stored in DataStore.

| ID | Requirement | Priority |
|----|------------|----------|
| F8.1 | Allow the user to select an accent color. | P1 |
| F8.2 | Allow the user to enter a Last.fm API key and shared secret. | P0 |
| F8.3 | Provide toggles for: AI EQ, AutoEQ, smooth transitions, safety limiter, auto-preset on track change. | P1 |
| F8.4 | Support Last.fm scrobbling (submit plays). | P2 |
| F8.5 | Prompt for Notification Listener permission on first launch. | P0 |

---

## 6. Architecture

### 6.1 High-Level Architecture

Sonara follows a layered **MVVM** architecture with four tiers:

```
┌──────────────────────────────────────────────────────────────────┐
│                   PRESENTATION LAYER (Jetpack Compose)           │
│  Dashboard │ Equalizer │ Presets │ Insights │ Settings           │
└──────────────────────────────────────────────────────────────────┘
                              ↕
┌──────────────────────────────────────────────────────────────────┐
│                     VIEWMODEL LAYER                              │
│  DashboardVM │ EqualizerVM │ PresetsVM │ InsightsVM │ SettingsVM │
│  TrackResolver │ AutoEqManager │ PresetManager │ HeadphoneDetector│
└──────────────────────────────────────────────────────────────────┘
                              ↕
┌──────────────────────────────────────────────────────────────────┐
│                    BUSINESS LOGIC LAYER                           │
│  AudioEngine │ ProfileComposer │ SafetyLimiter │ BandMapper      │
│  TrackResolver │ LastFmResolver │ LocalAudioAnalyzer │ TrackCache │
│  AiEqSuggestionEngine │ HeadphoneDetector │ AutoEqManager        │
│  PresetManager │ BuiltInPresets │ PresetRepository                │
└──────────────────────────────────────────────────────────────────┘
                              ↕
┌──────────────────────────────────────────────────────────────────┐
│                       DATA LAYER                                 │
│  Room Database (presets, track_cache) │ DataStore (preferences)   │
│  SonaraService │ SonaraNotificationListener │ Last.fm REST API   │
└──────────────────────────────────────────────────────────────────┘
```

### 6.2 Audio Processing Pipeline

When a new track is detected, the following pipeline executes:

```
Track Detected (NotificationListener)
       │
       ▼
TrackResolver.resolve(title, artist)
  ├─ 1. Check cache → hit? return cached result
  ├─ 2. Last.fm track.getInfo → parse tags → classify
  ├─ 3. Last.fm artist.getTopTags (fallback)
  └─ 4. LocalAudioAnalyzer (offline fallback)
       │
       ▼
AiEqSuggestionEngine.suggest(genre, mood, energy)
       │
       ▼
ProfileComposer.compose(presetBands, autoEqBands, aiBands)
       │
       ▼
SafetyLimiter.apply(composedBands)
       │
       ▼
AudioEngine.applyBands(finalBands)  →  Android System Equalizer
```

### 6.3 Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| Manual dependency injection (no Hilt/Dagger) | Keeps the dependency graph simple for a single-module app. |
| `NotificationListenerService` for track detection | Works across all media players without requiring a custom media player. |
| Foreground service for persistence | Ensures uninterrupted audio processing even when the app is backgrounded. |
| Room for structured data, DataStore for KV preferences | Room handles relational preset data; DataStore handles simple flags. |
| Last.fm as primary metadata source | Best free API for music genre/tag data with broad catalog coverage. |
| Local AI as fallback | Guarantees functionality when offline or when the API key is not configured. |
| 10-band EQ (not parametric) | Matches Android's system equalizer API band count for maximum compatibility. |
| Safety limiter at 10 dB combined gain | Prevents speaker/headphone damage and audio distortion. |

---

## 7. Data Model

### 7.1 Preset Entity

```
Table: presets
─────────────────────────────────────────────
Column          Type       Constraints
─────────────────────────────────────────────
id              Long       PK, auto-increment
name            String     Not null
bands           String     CSV of 10 float values
preamp          Float      -12.0 to +12.0
bassBoost       Int        0 to 1000
virtualizer     Int        0 to 1000
loudness        Int        0 to 2000
isBuiltIn       Boolean    true for stock presets
category        String     e.g., "genre", "mood"
headphoneId     String?    Nullable, linked device
genre           String?    Nullable
isFavorite      Boolean    default false
lastUsed        Long       Epoch millis
─────────────────────────────────────────────
```

### 7.2 Track Cache Entity

```
Table: track_cache
─────────────────────────────────────────────
Column          Type       Constraints
─────────────────────────────────────────────
cacheKey        String     PK (hash of title::artist)
title           String     Not null
artist          String     Not null
album           String     Not null
genre           String     Not null
mood            String     Not null
energy          Float      0.0 to 1.0
confidence      Float      0.0 to 1.0
source          String     "lastfm", "local-ai", etc.
timestamp       Long       Insert time for TTL
─────────────────────────────────────────────
TTL: 30 days
```

### 7.3 User Preferences (DataStore)

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `accent_color` | String | `"Amber"` | Selected UI accent color. |
| `lastfm_api_key` | String | `""` | Last.fm API key. |
| `lastfm_shared_secret` | String | `""` | Last.fm shared secret. |
| `lastfm_session_key` | String | `""` | Last.fm session key (OAuth). |
| `ai_enabled` | Boolean | `true` | Whether AI EQ suggestions are applied. |
| `autoeq_enabled` | Boolean | `true` | Whether AutoEQ headphone correction is applied. |
| `smooth_transitions` | Boolean | `true` | Animate transitions between EQ profiles. |
| `safety_limiter` | Boolean | `true` | Prevent clipping above 10 dB combined gain. |
| `scrobbling_enabled` | Boolean | `false` | Submit plays to Last.fm. |
| `auto_preset` | Boolean | `true` | Auto-apply presets on track change. |
| `notification_listener_prompted` | Boolean | `false` | Whether the first-run permission prompt was shown. |

---

## 8. Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 2.0.10 |
| UI Framework | Jetpack Compose (BOM 2024.06.00) |
| Design System | Material 3 |
| Navigation | Navigation Compose 2.7.7 |
| State Management | ViewModel + Kotlin Flow / StateFlow |
| Local Database | Room 2.6.1 |
| Preferences | Jetpack DataStore 1.1.1 |
| Networking | Retrofit 2.11.0 + OkHttp 4.12.0 |
| JSON Serialization | Gson 2.11.0 |
| Annotation Processing | KSP 2.0.10-1.0.24 |
| Async | Kotlin Coroutines 1.8.1 |
| Build System | Gradle 8.9 (Kotlin DSL) |
| CI/CD | GitHub Actions |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 34 (Android 14) |
| Java Target | 17 (Temurin) |

---

## 9. Android Permissions

| Permission | Reason |
|-----------|--------|
| `INTERNET` | Last.fm API calls for track metadata. |
| `MODIFY_AUDIO_SETTINGS` | Control the system equalizer and audio effects. |
| `BLUETOOTH` | Detect Bluetooth headphone connections. |
| `BLUETOOTH_CONNECT` | Read Bluetooth device names (Android 12+ runtime permission). |
| `FOREGROUND_SERVICE` | Run persistent background audio processing. |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Classify the foreground service type. |
| `POST_NOTIFICATIONS` | Display notifications (Android 13+ runtime permission). |

---

## 10. External Integrations

### Last.fm API

| Endpoint | Purpose |
|----------|---------|
| `track.getInfo` | Retrieve track metadata and top tags for genre/mood classification. |
| `artist.getTopTags` | Fallback: retrieve artist-level genre tags when track data is unavailable. |

**Authentication:** API Key (user-configurable in Settings).

**Fallback:** When the API is unavailable or no key is configured, the app falls back to `LocalAudioAnalyzer` for heuristic-based genre/mood detection. The app is fully functional offline.

---

## 11. Screens & Navigation

The app uses Jetpack Navigation Compose with a bottom navigation bar connecting five top-level screens:

| Screen | Route | Description |
|--------|-------|-------------|
| Dashboard | `/dashboard` | Now-playing display, headphone status, quick preset selector, audio visualizer. |
| Equalizer | `/equalizer` | 10-band EQ sliders, preamp, bass boost, virtualizer, loudness controls, frequency curve. |
| Presets | `/presets` | Browse, search, favorite, create, duplicate, delete, export/import presets. |
| Insights | `/insights` | Sound pipeline visualization, track analysis details, cache stats. |
| Settings | `/settings` | Accent color, Last.fm credentials, feature toggles. |

---

## 12. Build & CI/CD

### Local Build

```bash
gradle assembleDebug
```

Output: `app/build/outputs/apk/debug/*.apk`

### Release Build

- R8 minification and ProGuard obfuscation enabled.
- Configured in `app/build.gradle.kts`.

### CI/CD (GitHub Actions)

**Workflow:** `.github/workflows/build.yml`

**Triggers:** Push to `main`, Pull Requests to `main`.

**Pipeline:**
1. Checkout code (`actions/checkout@v4`).
2. Set up Java 17 (Temurin).
3. Set up Gradle 8.9.
4. Run `gradle assembleDebug --no-daemon`.
5. Upload `sonara-debug` APK artifact.

---

## 13. Non-Functional Requirements

| ID | Requirement | Target |
|----|------------|--------|
| NFR1 | The audio processing pipeline must complete within 100 ms of a track change to avoid audible delay. | P0 |
| NFR2 | EQ band transitions must be smooth (no audible clicks or pops). | P1 |
| NFR3 | The foreground service must not consume more than 1% battery per hour of idle monitoring. | P1 |
| NFR4 | The app must handle graceful degradation when Last.fm is unreachable (use local fallback). | P0 |
| NFR5 | The app must support Android 8.0 (API 26) through Android 14 (API 34). | P0 |
| NFR6 | All user data (presets, preferences, cache) must be stored locally on-device. | P0 |
| NFR7 | The safety limiter must never allow combined gain to exceed the configured threshold. | P0 |
| NFR8 | The UI must render at 60 fps on mid-range devices. | P1 |

---

## 14. Future Considerations

The following items are not in scope for v1.0 but are natural extensions of the architecture:

- **Parametric EQ** — Allow users to define custom center frequencies and Q factors.
- **Crossfade between presets** — Smoothly blend two presets over a configurable duration.
- **Widget support** — Home screen widget showing now-playing and quick preset toggle.
- **Wear OS companion** — Preset switching from a smartwatch.
- **Community presets** — Share and discover presets from other users via a cloud backend.
- **Hearing test integration** — Personalize EQ based on the user's audiogram.
- **Expanded headphone database** — Crowdsource headphone profiles from the community.
- **Multi-language support** — Localize the UI into additional languages.
- **Unit and instrumented tests** — Add comprehensive test coverage (JUnit, Espresso, MockK).

---

## 15. Glossary

| Term | Definition |
|------|-----------|
| **AutoEQ** | Automatic headphone equalization — applying correction curves to compensate for a headphone's frequency response. |
| **Band** | A frequency range in the equalizer (e.g., 1 kHz band). |
| **Confidence** | A 0–1 score indicating how reliable the track analysis result is. |
| **dB (Decibel)** | Unit of measurement for sound level / EQ gain. |
| **Energy** | A 0–1 score representing the perceived intensity of a track. |
| **EQ (Equalizer)** | A tool that adjusts the volume of specific frequency ranges. |
| **Genre** | The musical category of a track (e.g., Rock, Jazz, Electronic). |
| **Mood** | The emotional character of a track (e.g., Energetic, Chill, Melancholic). |
| **Monet** | Android 12's dynamic color system that generates a palette from the user's wallpaper. |
| **Preamp** | A master volume control applied before the equalizer bands. |
| **Preset** | A saved set of EQ band values, preamp, and effect settings. |
| **ProfileComposer** | The component that blends AutoEQ, preset, and AI bands into a final EQ curve. |
| **Safety Limiter** | A component that caps combined gain to prevent clipping and distortion. |
| **Scrobbling** | Submitting track play history to Last.fm. |
| **TTL (Time To Live)** | The duration a cached result remains valid before expiring. |
