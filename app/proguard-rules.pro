# Sonara ProGuard Rules

# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Gson
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Last.fm Models
-keep class com.sonara.app.intelligence.lastfm.** { *; }

# Preset Export Models
-keep class com.sonara.app.preset.PresetExporter$ExportablePreset { *; }
-keep class com.sonara.app.preset.PresetExporter$ExportBundle { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# Compose
-dontwarn androidx.compose.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Keep data classes
-keepclassmembers class com.sonara.app.data.models.** { *; }
-keepclassmembers class com.sonara.app.media.NowPlayingInfo { *; }
-keepclassmembers class com.sonara.app.service.ListenerNowPlaying { *; }
