-keepattributes Signature
-keepattributes *Annotation*

# Retrofit / Gson
-keep class com.sonara.app.intelligence.lastfm.** { *; }
-keep class com.sonara.app.preset.PresetExporter$ExportablePreset { *; }
-keep class com.sonara.app.preset.PresetExporter$ExportBundle { *; }

# Room
-keep class com.sonara.app.preset.Preset { *; }
-keep class com.sonara.app.intelligence.cache.TrackCacheEntity { *; }

# Data models
-keep class com.sonara.app.data.models.** { *; }
-keep class com.sonara.app.service.ListenerNowPlaying { *; }

# Compose
-dontwarn androidx.compose.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Gson
-keep class com.google.gson.** { *; }
-keepclassmembers class * { @com.google.gson.annotations.SerializedName <fields>; }
