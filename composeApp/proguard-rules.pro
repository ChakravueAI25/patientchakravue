# ProGuard rules for PatientChakraVue

# Keep all classes in the application package
-keep class com.org.patientchakravue.** { *; }
-dontwarn com.org.patientchakravue.**

# Keep Compose classes
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep annotations
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes SourceFile,LineNumberTable
-keepattributes InnerClasses,EnclosingMethod

# Keep Kotlinx Serialization
-keepattributes RuntimeVisibleAnnotations
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.org.patientchakravue.**$$serializer { *; }
-keepclassmembers class com.org.patientchakravue.** {
    *** Companion;
}
-keepclasseswithmembers class com.org.patientchakravue.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Ktor classes
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-keep class io.ktor.client.** { *; }
-keep class io.ktor.serialization.** { *; }

# Keep Coil classes
-keep class coil.** { *; }
-dontwarn coil.**

# Keep Multiplatform Settings classes
-keep class com.russhwolf.settings.** { *; }
-dontwarn com.russhwolf.settings.**

# Keep Firebase Messaging classes
-keep class com.google.firebase.messaging.** { *; }
-dontwarn com.google.firebase.messaging.**

# Keep Agora SDK classes (required for video calling)
-keep class io.agora.** { *; }
-dontwarn io.agora.**

# Keep Kotlinx Datetime classes
-keep class kotlinx.datetime.** { *; }
-dontwarn kotlinx.datetime.**

# Keep Kotlinx Coroutines classes
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Optimization settings
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# Remove Kotlin debug info
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void checkParameterIsNotNull(...);
    public static void checkNotNullParameter(...);
    public static void checkExpressionValueIsNotNull(...);
    public static void checkNotNullExpressionValue(...);
    public static void checkReturnedValueIsNotNull(...);
    public static void checkFieldIsNotNull(...);
    public static void throwUninitializedPropertyAccessException(...);
}
