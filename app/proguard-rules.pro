# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# Optimization and R8 keep rules for Velvet Music
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*

# Moshi and Models
-keep class com.example.model.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
}
-keep class com.squareup.moshi.** { *; }

# Room
-keep class androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Media3 & ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Coil Image Loading
-keep class coil.** { *; }
-dontwarn coil.**

