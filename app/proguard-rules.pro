# ── Recallly ProGuard / R8 Rules ──────────────────────────────────────────────

# Preserve line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Kotlin Serialization ─────────────────────────────────────────────────────
# Keep serializable classes and their generated serializers
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep @Serializable classes in the project
-keep,includedescriptorclasses class com.at.recallly.**$$serializer { *; }
-keepclassmembers class com.at.recallly.** {
    *** Companion;
}
-keepclasseswithmembers class com.at.recallly.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ── Navigation Compose (@Serializable routes) ───────────────────────────────
-keep class com.at.recallly.presentation.navigation.Route { *; }
-keep class com.at.recallly.presentation.navigation.Route$* { *; }

# ── whisper.cpp JNI ──────────────────────────────────────────────────────────
-keep class com.at.recallly.data.whisper.WhisperContext { *; }
-keep class com.at.recallly.data.whisper.WhisperContext$* { *; }
# WhisperLib is private but has JNI native methods
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# ── Firebase Auth ────────────────────────────────────────────────────────────
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# ── Google API Client (Calendar, Drive) ──────────────────────────────────────
-keep class com.google.api.client.** { *; }
-keep class com.google.api.services.** { *; }
-dontwarn com.google.api.client.**
-dontwarn com.google.api.services.**

# Google API uses reflection for JSON parsing
-keepclassmembers class * {
    @com.google.api.client.util.Key <fields>;
}

# ── Google Play Billing ──────────────────────────────────────────────────────
-keep class com.android.vending.billing.** { *; }

# ── Google Mobile Ads (AdMob) ────────────────────────────────────────────────
-keep class com.google.android.gms.ads.** { *; }

# ── Gemini AI (Google Generative AI) ─────────────────────────────────────────
-keep class com.google.ai.client.generativeai.** { *; }
-dontwarn com.google.ai.client.generativeai.**

# ── Credential Manager ──────────────────────────────────────────────────────
-keep class androidx.credentials.** { *; }
-dontwarn androidx.credentials.**

# ── Room (runtime, no compiler) ─────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# ── WorkManager ──────────────────────────────────────────────────────────────
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }

# ── Koin ─────────────────────────────────────────────────────────────────────
-keep class org.koin.** { *; }
-dontwarn org.koin.**

# ── BroadcastReceivers (reminders) ───────────────────────────────────────────
-keep class com.at.recallly.data.notification.ReminderReceiver { *; }
-keep class com.at.recallly.data.notification.BootReceiver { *; }

# ── Suppress common warnings ────────────────────────────────────────────────
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
-dontwarn javax.annotation.**
-dontwarn sun.misc.Unsafe
