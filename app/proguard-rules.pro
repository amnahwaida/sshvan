# Netvan ProGuard Rules

# Keep JSch classes
-keep class com.jcraft.jsch.** { *; }
-dontwarn com.jcraft.jsch.**

# Keep Room entities
-keep class com.sshvan.tunnelmanager.data.local.entity.** { *; }

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }
