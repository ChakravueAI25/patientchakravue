# PatientChakraVue - Project File Structure

## Overview
This is a Kotlin Multiplatform (KMP) project for a patient-facing healthcare application with vision testing, medication adherence tracking, and telemedicine features. The structure follows a clean architecture pattern with separation of concerns across app flow, data, models, platform-specific code, and UI.

## Complete Project Structure

```
patientchakravue/
├── README.md
├── project-structure.md
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
├── local.properties
├── backend.md                                 # Backend API documentation
├── backendpy.txt                              # Backend Python notes
│
├── build/                                     # Gradle build output
│   └── reports/
│       ├── configuration-cache/
│       └── problems/
│
├── composeApp/
│   ├── build.gradle.kts
│   ├── google-services.json                   # Firebase config for Android
│   ├── proguard-rules.pro                     # Code shrinking rules
│   │
│   ├── build/                                 # Build artifacts
��   │   ├── generated/
│   │   ├── intermediates/
│   │   ├── kotlin/
│   │   ├── outputs/
│   │   └── tmp/
│   │
│   └── src/
│       ├── commonMain/                        # Shared Kotlin code (Android + iOS)
│       │   ├── composeResources/
│       │   │   ├── drawable/
│       │   │   │   └── compose-multiplatform.xml     # Shared resources
│       │   │   ├── values/
│       │   │   │   └── strings.xml                   # English strings
│       │   │   ├── values-hi/
│       │   │   │   └── strings.xml                   # Hindi strings
│       │   │   └── values-te/
│       │   │       └── strings.xml                   # Telugu strings
│       │   │
│       │   └── kotlin/com/org/patientchakravue/
│       │       ├── app/                      # 🎯 App flow & navigation
│       │       │   ├── App.kt                        # Main app entry with NavHost
│       │       │   ├── AppBackHandler.kt             # expect/actual back handler
│       │       │   ├── AppScreen.kt                  # Screen route definitions
│       │       │   └── Navigator.kt                  # Navigation utilities
│       │       │
│       │       ├── data/                     # 🔌 Backend & repositories
│       │       │   ├── ApiRepository.kt              # API communication (HTTP client)
│       │       │   └── SessionManager.kt             # Session/login state management
│       │       │
│       │       ├── dose/                     # 💊 Dose Management
│       │       │   └── DoseRefreshBus.kt             # Dose update event bus
│       │       │
│       │       ├── model/                    # 📦 Data contracts
│       │       │   └── Models.kt                     # All @Serializable data classes
│       │       │       ├── LoginResponse
│       │       │       ├── UserProfile
│       │       │       ├── PatientRecord
│       │       │       ├── DoseInfo
│       │       │       ├── AdhHistory
│       │       │       ├── AmslerTest
│       │       │       ├── VideoCallRequest
│       │       │       ├── NotificationItem
│       │       │       └── ... (15+ more)
│       │       │
│       │       ├── platform/                # 🔧 Platform-specific code
│       │       │   ├── Platform.kt                   # expect Platform interface
│       ���       │   ├── SystemTime.kt                # expect time utilities
│       │       │   └── BitmapCapture.kt             # expect screenshot/bitmap capture
│       │       │
│       │       └── ui/                      # 🎨 User Interface (all screens)
│       │           ├── language/                     # Localization utilities
│       │           │   ├── LanguageSwitcherIcon.kt  # Language selector UI
│       │           │   └── Localization.kt          # i18n support
│       │           │
│       │           ├── theme/                       # Theme and styling
│       │           │   └── Theme.kt                 # MaterialTheme + colors
│       │           │
│       │           ├── DashboardScreen.kt           # Home screen (patient overview)
│       │           ├── LoginScreen.kt               # Login form
│       │           ├── ProfileScreen.kt             # Patient profile + settings
│       │           ├── NotificationsScreen.kt       # Notification feed
│       │           │
│       │           ├── VisionScreen.kt              # Vision tests hub
│       │           ├── AmslerTestScreen.kt          # Amsler grid vision test UI
│       │           ├── TumblingETestScreen.kt       # Tumbling E vision test UI
│       │           │
│       │           ├── AfterCareScreen.kt           # Post-surgery care form UI
│       │           │
│       │           ├── AdherenceGraphScreen.kt      # Medicine adherence chart UI
│       │           │
│       │           ├── ChatScreen.kt                # Doctor-patient chat UI
│       │           │
│       │           ├── VideoCallRequestScreen.kt    # Video call request UI
│       │           ├── VideoCallScreen.kt           # Active video call UI
│       │           │
│       │           └── FeedbackDetailScreen.kt      # Feedback details UI
│       │
│       ├── androidMain/                     # 🤖 Android-specific code
│       │   ├── AndroidManifest.xml
│       │   └── kotlin/com/org/patientchakravue/
│       │       ├── MainActivity.kt                  # App entry point
│       │       ├── app/
│       │       │   └── AppBackHandler.android.kt    # actual BackHandler using AndroidX
│       │       ├── firebase/
│       │       │   └── FirebaseService.kt           # FCM push notification service
│       │       ├── ui/
│       │       │   └── VideoCallScreen.kt           # Android-specific video call UI
│       │       └── platform/
│       │           ├── Platform.android.kt          # actual getPlatform() returns AndroidPlatform
│       │           ├── SystemTime.android.kt        # actual System.currentTimeMillis()
│       │           └── BitmapCapture.android.kt     # actual bitmap capture impl
│       │
│       ├── commonTest/                      # Shared tests
│       │   └── kotlin/...
│       │
│       ├── iosMain/                         # 🍎 iOS-specific code
│       │   └── kotlin/com/org/patientchakravue/
│       │       ├── MainViewController.kt            # iOS app entry
│       │       ├── app/
│       │       │   └── AppBackHandler.ios.kt        # actual no-op (uses gesture nav)
│       │       └── platform/
│       │           ├── Platform.ios.kt              # actual getPlatform() returns IOSPlatform
│       │           ├── SystemTime.ios.kt            # actual using NSDate
│       │           └── BitmapCapture.ios.kt         # actual bitmap capture impl
��       │
│       ├── iosArm64Main/                    # iOS ARM64-specific code
│       │   └── kotlin/...
│       │
│       ├── iosX64Main/                      # iOS X64-specific code
│       │   └── kotlin/...
│       │
│       └── main/                            # Android main resources (multiplatform)
│           └── res/
│               ├── mipmap-anydpi-v26/
│               ├── mipmap-hdpi/
│               ├── mipmap-mdpi/
│               ├── mipmap-xhdpi/
│               ├── mipmap-xxhdpi/
│               └── mipmap-xxxhdpi/
│
├── gradle/
│   ├── libs.versions.toml                    # Dependency versions (centralized)
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
│
└── iosApp/                                   # 🍎 iOS native wrapper
    ├── Configuration/
    │   └── Config.xcconfig
    ├── iosApp/
    │   ├── ContentView.swift
    │   ├── Info.plist
    │   ├── iOSApp.swift
    │   ├── Assets.xcassets/
    │   └── Preview Content/
    └── iosApp.xcodeproj/
        ├── project.pbxproj
        └── project.xcworkspace/
```

## Key Features by Layer

### App Layer (`app/`)
- **App.kt**: NavHost with composable routes for all screens
- **Navigator.kt**: Utils for bottom nav visibility and screen management
- **AppScreen.kt**: Sealed class for type-safe routing
- **AppBackHandler.kt**: Platform-specific back button handling

### Data Layer (`data/`)
- **ApiRepository.kt**: 
  - Ktor HTTP client for API calls
  - Methods: login, getUserProfile, getNotifications, submitAmslerTest, submitAfterCareForm, etc.
  - Base URL: Backend server endpoint
- **SessionManager.kt**: 
  - Stores patient ID, profile in local settings
  - Handles login/logout state

### Model Layer (`model/`)
- **15+ data classes** (all @Serializable for JSON)
- Key models:
  - `UserProfile` (patient details)
  - `DoseInfo` (medication schedule)
  - `AdhHistory` (adherence tracking)
  - `AmslerTest` (vision test submission)
  - `VideoCallRequest` (call metadata)

### Platform Layer (`platform/`)
- **Platform.kt**: Android/iOS device info
- **SystemTime.kt**: Current time in milliseconds
- **BitmapCapture.kt**: Screenshot/image capture placeholder
- Expect/actual pattern for multiplatform support

### UI Layer (`ui/`)
**Main Screens (13 total):**
1. `LoginScreen` - Email + password form
2. `DashboardScreen` - Home with health overview, notifications, quick actions
3. `ProfileScreen` - Patient profile + settings
4. `VisionScreen` - Vision tests hub (Amsler, Tumbling E)
5. `AmslerTestScreen` - Amsler grid vision test UI
6. `TumblingETestScreen` - Tumbling E vision test UI
7. `AfterCareScreen` - Post-surgery care form submission
8. `AdherenceGraphScreen` - Medicine adherence tracking & charts
9. `ChatScreen` - Doctor-patient messaging
10. `VideoCallRequestScreen` - Incoming video call requests
11. `VideoCallScreen` - Active video call UI
12. `NotificationsScreen` - Notification feed
13. `FeedbackDetailScreen` - Feedback viewing/submission

**Navigation Routes (approx):**
```
login → dashboard ─┬─ profile
                  ├─ notifications
                  ├─ vision → amsler_test, tumbling_e_test
                  ├─ aftercare
                  ├─ adherence
                  ├─ chat
                  └─ video_call_request → video_call
```

**Localization Support:**
- English (default)
- Hindi
- Telugu

## Build & Dependencies

### Gradle Build System
- **composeApp/build.gradle.kts**: Main module configuration
- **settings.gradle.kts**: Project settings
- **gradle/libs.versions.toml**: Centralized dependency versions

### Key Dependencies
- **Compose**: Latest Material3
- **Ktor**: HTTP client
- **kotlinx.serialization**: JSON serialization
- **Coil3**: Image loading
- **Russhwolf.settings**: Multiplatform preferences
- **Firebase**: Android push notifications (FCM)

### Build Variants
- **Android**: Debug + Release (with Gradle variants)
- **iOS**: Simulator + Device (via Xcode)

## Architecture Decisions

1. **Flat UI Structure**: All screens in `ui/` folder (not feature-based nested)
2. **Consolidated Models**: All data classes in single `Models.kt`
3. **Expect/Actual Pattern**: Platform code in `platform/` folder
4. **Session Management**: Extracted to dedicated `SessionManager.kt`
5. **Dose Bus Pattern**: Reactive updates via `DoseRefreshBus` for medication adherence
6. **Localization**: Centralized i18n in `language/` subfolder

## File Counts
- **Kotlin source files**: ~25 (commonMain)
- **Android-specific files**: ~5
- **iOS-specific files**: ~4
- **Total Composables**: 40+
- **Data classes**: 15+

## Build Status
✅ **Successful** - Android debug compilation passes
✅ **Multiplatform** - iOS compilation with KMP support
⚠️ **Warnings**: 
- Expect/actual classes in Beta
- KMP/AGP compatibility warnings (expected for current AGP version)

---

*Last Updated: January 20, 2026*
*Project Type: Kotlin Multiplatform (KMP) for Android & iOS*
*Domain: Patient Healthcare Application with Vision Testing & Medication Tracking*
