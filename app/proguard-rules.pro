# Keep all data models and API DTOs from obfuscation/stripping
-keep class com.beouttv.app.data.model.** { *; }
-keep class com.beouttv.app.data.api.** { *; }
-keep class com.beouttv.app.data.config.** { *; }

# Preserve JSON / GSON serialization
-keepattributes Signature
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Media3 & ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Coil Image Loader
-keep class coil.** { *; }
-dontwarn coil.**

# OkHttp & Okio
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# Debug & Crash Activity
-keep class com.beouttv.app.ui.screens.debug.** { *; }
-keep class com.beouttv.app.data.debug.** { *; }

# Preserve Ostora JNI Native Bridge & Stream Resolvers
-keep class info.t4w.vp.** { *; }
-keepclasseswithmembernames class info.t4w.vp.** {
    native <methods>;
}
-keep class com.beouttv.app.data.source.stream.** { *; }

# Beout TV Military-Grade Native Security Vault
-keep class com.beouttv.app.data.security.NativeSecurity {
    native <methods>;
}
-keepclasseswithmembernames class com.beouttv.app.data.security.** {
    native <methods>;
}

# Advanced R8 bytecode shrinking and optimization
-allowaccessmodification
-repackageclasses ''

# Strip verbose and debug logging in official release build
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}

# Coroutines and Protobuf warnings suppression
-dontwarn kotlinx.coroutines.**
-dontwarn com.google.protobuf.**