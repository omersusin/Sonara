-keepattributes Signature
-keepattributes *Annotation*

# Retrofit / Gson
-keep class com.sonara.app.intelligence.lastfm.** { *; }
-keep class com.sonara.app.preset.PresetExporter$ExportablePreset { *; }
-keep class com.sonara.app.preset.PresetExporter$ExportBundle { *; }
-keep class com.sonara.app.data.models.** { *; }
-keep class com.sonara.app.service.ListenerNowPlaying { *; }
-keep class com.sonara.app.intelligence.cache.TrackCacheEntity { *; }
-keep class com.sonara.app.preset.Preset { *; }

-keepclassmembers class * { @com.google.gson.annotations.SerializedName <fields>; }

-keep class com.google.gson.** { *; }
-dontwarn androidx.compose.**
-dontwarn okhttp3.**
-dontwarn okio.**

# Sonara data classes (added by bug bounty)
-keep class com.sonara.app.intelligence.deezer.DeezerImageResolver$* { *; }
-keep class com.sonara.app.intelligence.provider.InsightResult { *; }
-keep class com.sonara.app.intelligence.provider.InsightRequest { *; }
-keep class com.sonara.app.ui.screens.insights.InsightsViewModel$TopTrackItem { *; }
-keep class com.sonara.app.ui.screens.insights.InsightsViewModel$RecentTrackItem { *; }
-keep class com.sonara.app.backup.BackupManager$* { *; }
-keep class com.sonara.app.intelligence.lastfm.LastFmRecentTracksResponse { *; }
-keep class com.sonara.app.intelligence.lastfm.LastFmRecentTracks { *; }
-keep class com.sonara.app.intelligence.lastfm.LastFmRecentTrack { *; }
-keep class com.sonara.app.intelligence.lastfm.LastFmRecentArtist { *; }
-keep class com.sonara.app.intelligence.lastfm.LastFmRecentAlbum { *; }
-keep class com.sonara.app.intelligence.lastfm.LastFmDate { *; }
-keep class com.sonara.app.intelligence.lastfm.LastFmNowPlaying { *; }
