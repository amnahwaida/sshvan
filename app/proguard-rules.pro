# Netvan ProGuard Rules

# Keep JSch classes
-keep class com.jcraft.jsch.** { *; }
-dontwarn com.jcraft.jsch.**

# Keep ZeroTier classes and JNI
-keep class com.zerotier.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Gson serialization for Profile Data Classes
-keep class com.google.gson.** { *; }
-keep class com.sshvan.tunnelmanager.domain.model.** { *; }

# Keep Room entities
-keep class com.sshvan.tunnelmanager.data.local.entity.** { *; }

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }
