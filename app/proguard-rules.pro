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
