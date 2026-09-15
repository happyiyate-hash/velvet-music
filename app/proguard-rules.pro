# Add project specific ProGuard rules here.
# Optimization and R8 keep rules for Velvet Music
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# ===================================================================
# 1. Jetpack Compose Stability & Runtime Metadata
# ===================================================================
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep Data Models Stable for Compose Recomposition
-keep class **.model.** { *; }
-keep class **.data.** { *; }
-keep class com.example.model.** { *; }

# Moshi and JSON serialization
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
}
-keep class com.squareup.moshi.** { *; }

# Room Database
-keep class androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# ===================================================================
# 2. Coil & Hardware Bitmap Pipeline
# ===================================================================
-keep class coil.** { *; }
-dontwarn coil.**
-keepclassmembers class * implements coil.request.Request { *; }

# Prevent R8 from stripping Android Palette color extraction
-keep class androidx.palette.graphics.** { *; }
-dontwarn androidx.palette.graphics.**

# ===================================================================
# 3. Media3 / ExoPlayer Session
# ===================================================================
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
