# PatientChakraVue - Complete Folder Structure

```
D:\ChakraVue AI\patientchakravue\
│
├── 📁 .git/                          # Git repository
├── 📁 .gradle/                       # Gradle cache
├── 📁 .idea/                         # IDE settings
├── 📁 .kotlin/                       # Kotlin cache
├── 📁 build/                         # Build output
│
├── 📁 composeApp/                    # 🎯 MAIN APP MODULE
│   ├── 📁 build/                     # Module build output
│   ├── 📄 build.gradle.kts           # Module build config
│   ├── 📄 google-services.json       # Firebase config for Android
│   ├── 📄 proguard-rules.pro         # ProGuard rules for code shrinking
│   │
│   └── 📁 src/
│       │
│       ├── 📁 androidMain/           # 🤖 ANDROID-SPECIFIC CODE
│       │   ├── 📄 AndroidManifest.xml         # Android app manifest
│       │   ├── 📁 kotlin/com/org/patientchakravue/
│       │   │   ├── 📁 app/
│       │   │   │   └── 📄 AppBackHandler.android.kt   # Android back button handler
│       │   │   ├── 📁 firebase/
│       │   │   │   └── 📄 FirebaseService.kt          # FCM push notification service
│       │   │   ├── 📁 platform/
│       │   │   │   ├── 📄 BitmapCapture.android.kt    # Android bitmap capture
│       │   │   │   ├── 📄 Platform.android.kt         # Android platform utils
│       │   │   │   └── 📄 SystemTime.android.kt       # Android system time
│       │   │   ├── 📁 ui/
│       │   │   │   └── 📄 VideoCallScreen.kt          # Android-specific video call UI
│       │   │   └── 📄 MainActivity.kt                 # Android entry point
│       │   └── 📁 res/                               # Android resources (drawables, values, etc.)
│       │       ├── 📁 drawable/
│       │       ├── 📁 drawable-v24/
│       │       ├── 📁 mipmap-anydpi-v26/
│       │       ├── 📁 mipmap-hdpi/
│       │       ├── 📁 mipmap-mdpi/
│       │       ├── 📁 mipmap-xhdpi/
│       │       ├── 📁 mipmap-xxhdpi/
│       │       ├── 📁 mipmap-xxxhdpi/
│       │       └── 📁 values/
│       │
│       ├── 📁 commonMain/            # 🌐 SHARED CODE (Android + iOS)
│       │   ├── 📁 composeResources/
│       │   │   ├── 📁 drawable/
│       │   │   │   └── 📄 compose-multiplatform.xml   # Shared vector/image resources
│       │   │   ├── 📁 values/
│       │   │   │   └── 📄 strings.xml                 # English strings
│       │   │   ├── 📁 values-hi/
│       │   │   │   └── 📄 strings.xml                 # Hindi strings
│       │   │   └── 📁 values-te/
│       │   │       └── 📄 strings.xml                 # Telugu strings
│       │   └── 📁 kotlin/com/org/patientchakravue/
│       │       ├── 📁 app/                          # App Core
│       │       │   ├── 📄 App.kt                    # Main app composable
│       │       │   ├── 📄 AppBackHandler.kt         # Shared back handler
│       │       │   ├── 📄 AppScreen.kt              # Screen definitions
│       │       │   └── 📄 Navigator.kt              # Navigation logic
│       │       ├── 📁 data/                         # Data Layer
│       │       │   ├── 📄 ApiRepository.kt          # API calls
│       │       │   └── 📄 SessionManager.kt         # User session management
│       │       ├── 📁 dose/                         # Dose Management
│       │       │   └── 📄 DoseRefreshBus.kt         # Dose update event bus
│       │       ├── 📁 model/                        # Data Models
│       │       │   └── 📄 Models.kt                 # Data classes
│       │       ├── 📁 platform/                     # Platform Abstractions
│       │       │   ├── 📄 BitmapCapture.kt          # Shared bitmap capture
│       │       │   ├── 📄 Platform.kt               # Shared platform utils
│       │       │   └── 📄 SystemTime.kt             # Shared system time
│       │       └── 📁 ui/                           # 🎨 UI SCREENS
│       │           ├── 📄 AdherenceGraphScreen.kt   # Medicine adherence chart UI
│       │           ├── 📄 AfterCareScreen.kt        # Post-surgery care form UI
│       │           ├── 📄 AmslerTestScreen.kt       # Amsler grid vision test UI
│       │           ├── 📄 ChatScreen.kt             # Doctor-patient chat UI
│       │           ├── 📄 DashboardScreen.kt        # Main dashboard UI
│       │           ├── 📄 FeedbackDetailScreen.kt   # Feedback details UI
│       │           ├── 📄 LanguageSwitcherIcon.kt   # Language selector UI
│       │           ├── 📄 Localization.kt           # i18n support
│       │           ├── 📄 LoginScreen.kt            # Login page UI
│       │           ├── 📄 NotificationsScreen.kt    # Notifications list UI
│       │           ├── 📄 ProfileScreen.kt          # User profile UI
│       │           ├── 📄 Theme.kt                  # App theme and colors
│       │           ├── 📄 TumblingETestScreen.kt    # Tumbling E vision test UI
│       │           ├── 📄 VideoCallRequestScreen.kt # Video call request UI
│       │           ├── 📄 VideoCallScreen.kt        # Video call UI
│       │           └── 📄 VisionScreen.kt           # Vision tests hub UI
│       ├── 📁 commonTest/            # Shared tests
│       ├── 📁 iosMain/               # 🍎 iOS-SPECIFIC CODE
│       │   └── 📁 kotlin/com/org/patientchakravue/
│       │       ├── 📁 app/
│       │       ├── 📁 platform/
│       │       └── 📄 MainViewController.kt         # iOS entry point
│       ├── 📁 iosArm64Main/          # iOS ARM64-specific code
│       └── 📁 iosX64Main/            # iOS X64-specific code
│
├── 📁 gradle/                        # Gradle Wrapper and version catalog
│   ├── 📄 libs.versions.toml         # Dependency versions
│   └── 📁 wrapper/
│       ├── 📄 gradle-wrapper.jar     # Gradle wrapper binary
│       └── 📄 gradle-wrapper.properties # Gradle wrapper config
│
├── 📁 iosApp/                        # 🍎 iOS native shell project
│   ├── 📁 Configuration/
│   │   └── 📄 Config.xcconfig        # iOS build config
│   ├── 📁 iosApp/
│   │   ├── 📄 ContentView.swift      # iOS SwiftUI entry
│   │   ├── 📄 Info.plist             # iOS app info
│   │   ├── 📄 iOSApp.swift           # iOS app main
│   │   ├── 📁 Assets.xcassets/       # iOS image assets
│   │   └── 📁 Preview Content/       # SwiftUI previews
│   └── 📁 iosApp.xcodeproj/
│       ├── 📄 project.pbxproj        # Xcode project file
│       └── 📁 project.xcworkspace/   # Xcode workspace
│
├── 📄 .gitignore                     # Git ignore rules
├── 📄 backend.md                     # Backend API documentation/notes
├── 📄 backendpy.txt                  # Backend Python notes or code
├── 📄 build.gradle.kts               # Root Gradle build config
├── 📄 gradle.properties              # Gradle properties
├── 📄 gradlew                        # Gradle wrapper (Unix)
├── 📄 gradlew.bat                    # Gradle wrapper (Windows)
├── 📄 local.properties               # Local SDK paths
├── 📄 project-structure.md           # This file: project structure documentation
├── 📄 README.md                      # Project documentation
├── 📄 settings.gradle.kts            # Gradle settings
```

## Key Directories Summary

| Directory | Purpose |
|-----------|---------|
| `composeApp/src/commonMain/` | Shared Kotlin code (Android + iOS) |
| `composeApp/src/androidMain/` | Android-specific implementations |
| `composeApp/src/iosMain/` | iOS-specific implementations |
| `composeApp/src/commonMain/kotlin/.../ui/` | All UI screens |
| `composeApp/src/commonMain/kotlin/.../data/` | API & Session management |
| `composeApp/src/commonMain/kotlin/.../model/` | Data models |
| `composeApp/src/commonMain/composeResources/` | Shared resources (strings, images) |
| `iosApp/` | iOS native shell project |
| `gradle/` | Dependency version management |

