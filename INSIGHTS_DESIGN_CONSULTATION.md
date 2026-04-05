# Sonara Insights Screen — Design Consultation

## Context
Sonara is an AI-powered EQ app for Android. The Insights screen shows the user's listening statistics and habits, similar to stats.fm, Spotify Wrapped, and Pano Scrobbler.

## Current Data Sources
We have access to the following data via Last.fm API:
- **user.getInfo**: total scrobbles, artist count, registration date
- **user.getTopArtists**: top artists with play counts (supports period: 7day, 1month, 3month, 6month, 12month, overall)
- **user.getTopTracks**: top tracks with play counts + album art (same periods)
- **user.getWeeklyTrackChart**: this week's top tracks
- **user.getRecentTracks**: last N recently played tracks with album art + now-playing indicator
- **Deezer API**: artist photos, track album art, artist details (fans, albums, top tracks)
- **Local AI data**: genre distribution, songs learned, confidence/accuracy stats, audio route info

## Current Layout (top to bottom)
1. Big scrobble count (display font)
2. Horizontal period selector (1 week / 1 month / 3 months / 6 months / 1 year / Overall)
3. Stats grid (artists / learned / cached / accuracy)
4. Now Playing card
5. Top Artists (8, with Deezer photos, clickable → detail dialog)
6. Top Tracks (8, with album art)
7. Recently Played (8, album art + now-playing dot)
8. Genre Distribution (percentage bars)
9. This Week (weekly chart)
10. Detection (genre · mood · energy · confidence)
11. Pipeline status

## What We Want
A polished, stats.fm / Pano Scrobbler quality Insights screen. Please suggest:

1. **Layout order**: What should come first? What's most impactful?
2. **Missing features**: What stats would make this screen "wow"?
   - Listening streaks?
   - Average daily scrobbles?
   - Most active day/hour?
   - Genre trends over time?
3. **Visual design**: Cards, grids, charts — what works best on mobile?
4. **Interactions**: What should tapping an artist/track do?
5. **What to remove**: Anything that's clutter?

## Available Resources
- **Pano Scrobbler source code** (Kotlin Multiplatform): Has charts, listening activity bars, tag clouds, artist collages, scrobble history
- **stats.fm screenshots**: Grid stats, genre bars, pie charts, daily/monthly/yearly stream charts
- **Trackify screenshots**: Pie chart genre distribution, recently played, listening stats overview

## Constraints
- Single-file Compose UI (no multi-module navigation)
- Data comes from Last.fm API (no Spotify API currently)
- Artist images from Deezer (free, no auth)
- Must work on Android 8+ (API 26+)

## Questions for You
1. Given the data sources above, what's the optimal card order for maximum visual impact?
2. Should we add a "Listening Activity" bar chart (scrobbles per day of week)?
3. Is a genre pie chart better than percentage bars?
4. What additional Last.fm endpoints should we use?
5. How should the period selector work — should it affect ALL cards or just top artists/tracks?
6. Any creative stats we can derive from the data we already have?

Please provide specific, actionable recommendations with UI descriptions I can implement directly.
