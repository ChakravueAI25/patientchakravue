# ProGuard rules for PatientChakraVue - Optimized for size

# ============== OPTIMIZATION SETTINGS ==============
-optimizationpasses 5
-allowaccessmodification
-dontpreverify
-repackageclasses ''
-flattenpackagehierarchy
-mergeinterfacesaggressively
-overloadaggressively

# ============== REMOVE DEBUG CODE ==============
# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}

# Remove println and System.out
-assumenosideeffects class java.io.PrintStream {
    public void println(...);
    public void print(...);
}

# Remove Kotlin debug info and checks
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void checkParameterIsNotNull(...);
    public static void checkNotNullParameter(...);
    public static void checkExpressionValueIsNotNull(...);
    public static void checkNotNullExpressionValue(...);
    public static void checkReturnedValueIsNotNull(...);
    public static void checkFieldIsNotNull(...);
    public static void throwUninitializedPropertyAccessException(...);
}

# ============== KEEP ESSENTIAL CLASSES ==============
# Keep serialization classes
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses

# Keep Kotlinx Serialization
-keepattributes RuntimeVisibleAnnotations
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep app model classes (required for serialization)
-keep,includedescriptorclasses class com.org.patientchakravue.model.**$$serializer { *; }
-keepclassmembers class com.org.patientchakravue.model.** {
    *** Companion;
    <fields>;
}
-keepclasseswithmembers class com.org.patientchakravue.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class com.org.patientchakravue.model.* { *; }

# Keep Ktor client (minimal rules)
-keep class io.ktor.client.** { *; }
-keep class io.ktor.serialization.** { *; }
-dontwarn io.ktor.**

# Keep Coil (minimal)
-keep class coil3.** { *; }
-dontwarn coil3.**

# Keep Multiplatform Settings
-keep class com.russhwolf.settings.** { *; }
-dontwarn com.russhwolf.settings.**

# Keep Firebase Messaging
-keep class com.google.firebase.messaging.** { *; }
-dontwarn com.google.firebase.messaging.**

# Keep Agora SDK (required for video calling)
-keep class io.agora.** { *; }
-dontwarn io.agora.**

# Keep Kotlinx Datetime & Coroutines
-keep class kotlinx.datetime.** { *; }
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.datetime.**
-dontwarn kotlinx.coroutines.**

# ============== COMPOSE OPTIMIZATION ==============
# Keep Compose runtime but allow shrinking of unused composables
-keep class androidx.compose.runtime.** { *; }
-dontwarn androidx.compose.**

# Suppress warnings
-dontwarn org.slf4j.**
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
