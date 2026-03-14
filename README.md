# Sonara 🎵

**Personal Sound Engine** — A next-generation mobile audio application that understands your music, recognizes your headphones, and optimizes sound in real-time.

## Features

### 🧠 Intelligent Sound Processing
- **Last.fm Integration** — Detects genre, mood, and energy from track metadata
- **Local AI Fallback** — When Last.fm is unavailable, analyzes track characteristics locally
- **Smart Caching** — Remembers analyzed tracks for instant processing
- **Layered EQ Pipeline** — AutoEQ → Preset → AI Adjustment → Safety Limiter

### 🎛️ 10-Band Equalizer
- True 10-band EQ (31Hz – 16kHz)
- Real-time frequency curve visualization
- Preamp control (-12dB to +12dB)
- Bass Boost, Virtualizer, Loudness effects
- Smooth transitions between profiles

### 🎧 AutoEQ
- Automatic headphone detection (Wired, Bluetooth, USB)
- 26+ built-in headphone correction profiles
- Fuzzy matching for device names
- Per-headphone preset assignment

### 📋 Preset System
- 23 built-in presets across 8 categories
- Custom preset creation and management
- Favorites and recent presets
- Export/Import presets as JSON
- AI-generated presets from track analysis

### 🔍 Insights
- Sound pipeline visualization
- Confidence scoring
- Energy level analysis
- Cache statistics
- AI reasoning display

### 🎨 Design
- Fluent Design-inspired UI
- 8 accent colors + Monet dynamic theming (Android 12+)
- Dark theme optimized
- Animated audio visualizer

## Architecture

```text
┌─────────────────────────────────────┐
│ UI Layer (Compose)                  │
├─────────────────────────────────────┤
│ Dashboard │ EQ │ Presets │ Insights │
├─────────────────────────────────────┤
│ ViewModel Layer                     │
├──────────┬──────────┬───────────────┤
│ Track    │ AutoEQ   │ Preset        │
│ Resolver │ Manager  │ Manager       │
├──────────┼──────────┼───────────────┤
│ Last.fm  │ Headphone│ Room DB       │
│ Local AI │ Detector │ DataStore     │
│ Cache    │ Profiles │ Export/Import │
├──────────┴──────────┴───────────────┤
│ Audio Engine (System EQ)            │
│ Band Mapper │ Safety │ Transitions  │
└─────────────────────────────────────┘
```

## Tech Stack
- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Architecture:** MVVM
- **Database:** Room
- **Preferences:** DataStore
- **Network:** Retrofit + OkHttp
- **Build:** Gradle KTS + GitHub Actions

## Build

```bash
gradle assembleDebug
```

## License
Copyright © 2024 Sonara. All rights reserved.
