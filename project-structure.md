# PatientChakraVue - Complete Folder Structure

```
E:\patientchakravue\
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
│   ├── 📄 google-services.json       # Firebase config
│   │
│   └── 📁 src/
│       │
│       ├── 📁 androidMain/           # 🤖 ANDROID-SPECIFIC CODE
│       │   ├── 📄 AndroidManifest.xml
│       │   ├── 📁 kotlin/com/org/patientchakravue/
│       │   │   ├── 📁 app/
│       │   │   │   └── 📄 AppBackHandler.android.kt
│       │   │   ├── 📁 firebase/
│       │   │   │   └── 📄 FirebaseService.kt        # FCM Service
│       │   │   ├── 📁 platform/
│       │   │   │   ├── 📄 BitmapCapture.android.kt
│       │   │   │   ├── 📄 Platform.android.kt
│       │   │   │   └── 📄 SystemTime.android.kt
│       │   │   └── 📄 MainActivity.kt               # Android Entry Point
│       │   │
│       │   └── 📁 res/                              # Android Resources
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
│       │   │   │   ├── 📄 compose-multiplatform.xml
│       │   │   │   └── 📄 Login_bg.jpeg
│       │   │   ├── 📁 values/
│       │   │   │   └── 📄 strings.xml               # English strings
│       │   │   ├── 📁 values-hi/
│       │   │   │   └── 📄 strings.xml               # Hindi strings
│       │   │   └── 📁 values-te/
│       │   │       └── 📄 strings.xml               # Telugu strings
│       │   │
│       │   └── 📁 kotlin/com/org/patientchakravue/
│       │       │
│       │       ├── 📁 app/                          # App Core
│       │       │   ├── 📄 App.kt                    # Main Composable
│       │       │   ├── 📄 AppBackHandler.kt         # Back navigation
│       │       │   ├── 📄 AppScreen.kt              # Screen definitions
│       │       │   ├── 📄 BackNavigation.kt
│       │       │   └── 📄 Navigator.kt              # Navigation logic
│       │       │
│       │       ├── 📁 data/                         # Data Layer
│       │       │   ├── 📄 ApiRepository.kt          # API calls
│       │       │   └── 📄 SessionManager.kt         # User session
│       │       │
│       │       ├── 📁 dose/                         # Dose Management
│       │       │   └── 📄 DoseRefreshBus.kt         # Event bus for dose updates
│       │       │
│       │       ├── 📁 model/                        # Data Models
│       │       │   └── 📄 Models.kt                 # All data classes
│       │       │
│       │       ├── 📁 platform/                     # Platform Abstractions
│       │       │   ├── 📄 BitmapCapture.kt
│       │       │   ├── 📄 Platform.kt
│       │       │   └── 📄 SystemTime.kt
│       │       │
│       │       └── 📁 ui/                           # 🎨 UI SCREENS
│       │           ├── 📄 AdherenceGraphScreen.kt   # Medicine adherence charts
│       │           ├── 📄 AfterCareScreen.kt        # Post-surgery care form
│       │           ├── 📄 AmslerTestScreen.kt       # Amsler Grid vision test
│       │           ├── 📄 ChatScreen.kt             # Doctor-Patient chat
│       │           ├── 📄 DashboardScreen.kt        # Main dashboard
│       │           ├── 📄 FeedbackDetailScreen.kt   # Feedback details
│       │           ├── 📄 LanguageSwitcherIcon.kt   # Language selector UI
│       │           ├── 📄 Localization.kt           # i18n support
│       │           ├── 📄 LoginScreen.kt            # Login page
│       │           ├── 📄 NotificationsScreen.kt    # Notifications list
│       │           ├── 📄 ProfileScreen.kt          # User profile
│       │           ├── 📄 TumblingETestScreen.kt    # Tumbling E vision test
│       │           └── 📄 VisionScreen.kt           # Vision tests hub
│       │
│       ├── 📁 commonTest/            # Shared Tests
│       │
│       ├── 📁 iosMain/               # 🍎 iOS-SPECIFIC CODE
│       │   └── 📁 kotlin/com/org/patientchakravue/
│       │       ├── 📁 app/
│       │       ├── 📁 platform/
│       │       └── 📄 MainViewController.kt
│       │
│       ├── 📁 iosArm64Main/          # iOS ARM64 specific
│       └── 📁 iosX64Main/            # iOS X64 specific
│
├── 📁 gradle/                        # Gradle Wrapper
│   ├── 📄 libs.versions.toml         # Version catalog
│   └── 📁 wrapper/
│       ├── 📄 gradle-wrapper.jar
│       └── 📄 gradle-wrapper.properties
│
├── 📁 iosApp/                        # 🍎 iOS NATIVE SHELL
│   ├── 📁 Configuration/
│   │   └── 📄 Config.xcconfig
│   ├── 📁 iosApp/
│   │   ├── 📄 ContentView.swift
│   │   ├── 📄 Info.plist
│   │   ├── 📄 iOSApp.swift
│   │   ├── 📁 Assets.xcassets/
│   │   └── 📁 Preview Content/
│   └── 📁 iosApp.xcodeproj/
│       ├── 📄 project.pbxproj
│       └── 📁 project.xcworkspace/
│
├── 📄 .gitignore                     # Git ignore rules
├── 📄 build.gradle.kts               # Root build config
├── 📄 gradle.properties              # Gradle properties
├── 📄 gradlew                        # Gradle wrapper (Unix)
├── 📄 gradlew.bat                    # Gradle wrapper (Windows)
├── 📄 local.properties               # Local SDK paths
├── 📄 main.py                        # Backend Python script
├── 📄 main (1).py                    # Backend Python script (backup)
├── 📄 README.md                      # Project readme
├── 📄 settings.gradle.kts            # Gradle settings
└── 📄 text.txt                       # Notes/scratch file
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

