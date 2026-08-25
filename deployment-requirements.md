# PatientChakraVue — Deployment Requirements
> Complete checklist of changes required before uploading the AAB to Google Play Store.
> Every item below must be resolved. Items marked 🔴 are hard blockers — the app will fail or be rejected without them.

---

## TABLE OF CONTENTS
1. [Critical Code Changes](#1-critical-code-changes)
2. [Security Hardening](#2-security-hardening)
3. [Build & Signing Configuration](#3-build--signing-configuration)
4. [AndroidManifest Fixes](#4-androidmanifest-fixes)
5. [Login & Authentication](#5-login--authentication)
6. [Bug Fixes](#6-bug-fixes)
7. [Firebase & Push Notifications](#7-firebase--push-notifications)
8. [Play Store Metadata & Assets](#8-play-store-metadata--assets)
9. [Backend Requirements](#9-backend-requirements)
10. [Final Release Checklist](#10-final-release-checklist)

---

## 1. Critical Code Changes

### 🔴 1.1 Replace Cloudflare Tunnel URL with Production API URL
**File:** `composeApp/src/commonMain/kotlin/com/org/patientchakravue/data/ApiRepository.kt` — Line 42

**Current (broken in production):**
```kotlin
const val BASE_URL = "https://question-richard-where-ending.trycloudflare.com"
```

**Required:**
```kotlin
const val BASE_URL = "https://api.yourproductiondomain.com"  // replace with your actual domain
```

> The Cloudflare free tunnel is temporary. Every time the tunnel restarts, the URL changes.
> The app will show "Invalid Credentials" for ALL users if this is not replaced.

---

### 🔴 1.2 Add HTTP Timeouts to Ktor Client
**File:** `composeApp/src/commonMain/kotlin/com/org/patientchakravue/data/ApiRepository.kt` — Line 23

**Current (hangs forever on slow/dead server):**
```kotlin
val client = HttpClient {
    install(ContentNegotiation) { ... }
}
```

**Required — add HttpTimeout plugin:**
```kotlin
import io.ktor.client.plugins.*

val client = HttpClient {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true })
    }
    install(HttpTimeout) {
        connectTimeoutMillis = 10_000   // 10 seconds to connect
        requestTimeoutMillis = 30_000   // 30 seconds for full request
        socketTimeoutMillis  = 30_000   // 30 seconds socket idle
    }
}
```

> Without this, if the server is slow, `isLoading` stays `true` forever with no way out.

---

### 🔴 1.3 Add Authentication Token to All API Requests
**File:** `composeApp/src/commonMain/kotlin/com/org/patientchakravue/data/ApiRepository.kt`

**Current (all requests are unauthenticated):**
```kotlin
val response = NetworkClient.client.get("$baseUrl/patients/$patientId/doses/today")
```

**Required — attach Bearer token on every request:**
```kotlin
// Store token in SessionManager after login
fun saveToken(token: String) {
    settings.putString("auth_token", token)
}
fun getToken(): String? = settings.getStringOrNull("auth_token")

// Attach to every request
val token = SessionManager().getToken()
val response = NetworkClient.client.get("$baseUrl/patients/$patientId/doses/today") {
    if (token != null) header(HttpHeaders.Authorization, "Bearer $token")
}
```

> Without this, any person who knows a patient ID can fetch another patient's medical records.
> The backend must also validate the token matches the patient ID.

---

### 🔴 1.4 Fix Medicine List Screen — Currently a Placeholder
**File:** `composeApp/src/commonMain/kotlin/com/org/patientchakravue/app/App.kt` — Line 270

**Current (broken — shows raw text to user):**
```kotlin
is Screen.MedicineList -> Text(
    "Medicine List Screen",
    modifier = Modifier.padding(paddingValues)
)
```

**Required:** Implement the full MedicineList screen or remove the navigation to it from the Dashboard entirely before release.

---

### 🔴 1.5 Fix Locale Filter — Telugu is Stripped from Release APK
**File:** `composeApp/build.gradle.kts` — Line 113

**Current (wrong — "ta" is Tamil, not Telugu):**
```kotlin
androidResources {
    localeFilters += listOf("en", "hi", "ta")
}
```

**Required:**
```kotlin
androidResources {
    localeFilters += listOf("en", "hi", "te")  // "te" = Telugu
}
```

> With this bug, all Telugu (`values-te/strings.xml`) translations are stripped from the release APK.
> Telugu-speaking users will see English text instead of their language.

---

### 🟠 1.6 Fix Blank Screen When Session is Null on Protected Screens
**File:** `composeApp/src/commonMain/kotlin/com/org/patientchakravue/app/App.kt` — Lines 183, 195, 213, 230

**Current (silent blank screen — no redirect, no message):**
```kotlin
is Screen.Vision -> {
    val patient = sessionManager.getPatient()
    if (patient != null) {
        VisionScreen(...)
    }
    // no else — user sees empty screen
}
```

**Required — add else branch to every protected screen:**
```kotlin
is Screen.Vision -> {
    val patient = sessionManager.getPatient()
    if (patient != null) {
        VisionScreen(...)
    } else {
        sessionManager.clearSession()
        navigator.navigateAsPillar(Screen.Login)
    }
}
```

> Apply to: `Screen.Vision`, `Screen.AfterCare`, `Screen.AmslerGrid`,
> `Screen.TumblingE`, `Screen.Notifications`

---

### 🟠 1.7 Fix `getCallToken` — Channel Name Sent as Query Param on POST
**File:** `composeApp/src/commonMain/kotlin/com/org/patientchakravue/data/ApiRepository.kt` — Line 355

**Current (query param on a POST body endpoint):**
```kotlin
val response = NetworkClient.client.post("$baseUrl/call/token") {
    parameter("channel_name", channelName)  // appended as ?channel_name=... in URL
}
```

**Required — move to JSON body:**
```kotlin
val response = NetworkClient.client.post("$baseUrl/call/token") {
    contentType(ContentType.Application.Json)
    setBody(mapOf("channel_name" to channelName))
}
```

---

### 🟠 1.8 Remove Debug println Statements
**File:** `composeApp/src/commonMain/kotlin/com/org/patientchakravue/ui/DashboardScreen.kt` — Lines 389–413

**Current:**
```kotlin
println("[DEBUG] Surgery date string: $surgeryDateStr")
println("[DEBUG] Patient visits count: ${visits?.size ?: 0}")
println("[DEBUG] Last visit - stages?.doctor?.stageCompletedAt: ...")
// 8+ more println statements
```

**Required:** Remove all `println("[DEBUG]...")` calls before release.
ProGuard will strip them in release builds via the `assumenosideeffects` rule,
but they should not be in production source code.

---

### 🟠 1.9 Fix FCM Token — Do Not Delete on Every App Launch
**File:** `composeApp/src/androidMain/kotlin/com/org/patientchakravue/MainActivity.kt` — Line 221

**Current (deletes + re-fetches token on every launch — causes missed notifications):**
```kotlin
private fun initializeFirebaseMessaging() {
    FirebaseMessaging.getInstance().deleteToken().addOnCompleteListener { ... }
```

**Required — only get token if not already registered:**
```kotlin
private fun initializeFirebaseMessaging() {
    val session = SessionManager()
    val patient = session.getPatient() ?: return  // no patient, skip

    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
        if (!task.isSuccessful) return@addOnCompleteListener
        val token = task.result
        CoroutineScope(Dispatchers.IO).launch {
            ApiRepository().registerFcmToken(patient.id, token)
        }
    }
}
```

> Deleting the token on every launch creates a window where push notifications
> (medicine reminders, incoming calls) are silently dropped.

---

### 🟠 1.10 Reset `liveCallData` After Consuming the Call Intent
**File:** `composeApp/src/androidMain/kotlin/com/org/patientchakravue/MainActivity.kt` — Line 258

**Current (stale call data persists across screen rotations):**
```kotlin
companion object {
    val liveCallData = mutableStateOf<Pair<String, String>?>(null)
}
```

**Required — reset to null after navigating:**
In `App.kt`, after the `LaunchedEffect(liveCallData)` navigates to the video call:
```kotlin
LaunchedEffect(liveCallData) {
    if (liveCallData != null && sessionManager.getPatient() != null) {
        navigator.navigateAsPillar(Screen.VideoCall(liveCallData!!.first, liveCallData!!.second))
        MainActivity.liveCallData.value = null  // ← consume and reset
    }
}
```

---

## 2. Security Hardening

### 🔴 2.1 Encrypt Patient Data in Storage
**File:** `composeApp/src/commonMain/kotlin/com/org/patientchakravue/data/SessionManager.kt`

**Current (plain-text SharedPreferences — readable with ADB backup):**
```kotlin
private val settings: Settings = Settings()
```

**Required — use Encrypted SharedPreferences on Android:**

Add dependency to `composeApp/build.gradle.kts`:
```kotlin
androidMain.dependencies {
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
}
```

Create `EncryptedSessionManager.android.kt`:
```kotlin
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()

val encryptedPrefs = EncryptedSharedPreferences.create(
    context,
    "patient_secure_prefs",
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)
```

> Patient name, age, sex, address, doctor ID, and visit history are currently
> stored in plain-text XML. ADB backup (`adb backup`) can extract this without root.

---

### 🔴 2.2 Add Network Security Configuration
**Create file:** `composeApp/src/androidMain/res/xml/network_security_config.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <domain-config cleartextTrafficPermitted="false">
        <domain includeSubdomains="true">api.yourproductiondomain.com</domain>
        <pin-set expiration="2026-12-01">
            <!-- Get this from: openssl s_client -connect api.yourdomain.com:443 | openssl x509 -pubkey -noout | openssl pkey -pubin -outform der | openssl dgst -sha256 -binary | base64 -->
            <pin digest="SHA-256">YOUR_CERTIFICATE_PIN_BASE64_HERE</pin>
            <pin digest="SHA-256">YOUR_BACKUP_PIN_BASE64_HERE</pin>
        </pin-set>
    </domain-config>
</network-security-config>
```

Then reference in `AndroidManifest.xml`:
```xml
<application
    android:networkSecurityConfig="@xml/network_security_config"
    ...>
```

> Without this, a MITM attack on the same Wi-Fi (common in hospitals/clinics)
> can intercept all patient data and login credentials.

---

### 🟠 2.3 Add Login Rate Limiting on Client Side
**File:** `composeApp/src/commonMain/kotlin/com/org/patientchakravue/ui/LoginScreen.kt`

Add a failed attempt counter with a lockout period:
```kotlin
var failedAttempts by remember { mutableStateOf(0) }
var lockoutUntil by remember { mutableStateOf(0L) }

// In the login onClick, before calling api.login():
val now = System.currentTimeMillis()
if (failedAttempts >= 5 && now < lockoutUntil) {
    val remaining = ((lockoutUntil - now) / 1000).toInt()
    showSnackbar("Too many attempts. Try again in ${remaining}s")
    return@Button
}

// On failed login:
failedAttempts++
if (failedAttempts >= 5) {
    lockoutUntil = now + 5 * 60 * 1000 // 5 minute lockout
}
```

---

## 3. Build & Signing Configuration

### 🔴 3.1 Add Release Signing Configuration
**File:** `composeApp/build.gradle.kts`

**Required — add before `buildTypes` block:**
```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile      = file(System.getenv("KEYSTORE_PATH") ?: "release.keystore")
            storePassword  = System.getenv("KEYSTORE_PASSWORD") ?: ""
            keyAlias       = System.getenv("KEY_ALIAS")         ?: ""
            keyPassword    = System.getenv("KEY_PASSWORD")      ?: ""
        }
    }
    buildTypes {
        getByName("release") {
            signingConfig  = signingConfigs.getByName("release")
            isMinifyEnabled   = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}
```

**Generate the keystore (run once, keep the file safe forever):**
```bash
keytool -genkey -v \
  -keystore release.keystore \
  -alias patientchakravue \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

> ⚠️ NEVER commit `release.keystore` to git.
> Add `*.keystore` and `*.jks` to your `.gitignore`.
> If you lose this keystore, you CANNOT update the app on Play Store — ever.

---

### 🔴 3.2 Increment Version Code and Version Name
**File:** `composeApp/build.gradle.kts` — Lines 102-103

```kotlin
defaultConfig {
    versionCode = 1        // ← increment by 1 for EVERY upload to Play Store
    versionName = "1.0"    // ← update to reflect your release (e.g. "1.0.0")
}
```

> Play Store rejects an upload if `versionCode` is not strictly greater than the previous upload.

---

### 🟠 3.3 Enable BuildConfig for Debug/Release Detection
**File:** `composeApp/build.gradle.kts` — Line 172

**Current:**
```kotlin
buildFeatures {
    buildConfig = false   // ← prevents BuildConfig.DEBUG usage
}
```

**Required:**
```kotlin
buildFeatures {
    buildConfig = true    // ← enables BuildConfig.DEBUG, BuildConfig.VERSION_NAME etc.
}
```

---

### 🟠 3.4 Build the App as AAB (not APK)
Play Store requires an Android App Bundle, not an APK.

**Command to generate release AAB:**
```bash
./gradlew :composeApp:bundleRelease
```

Output location:
```
composeApp/build/outputs/bundle/release/composeApp-release.aab
```

---

## 4. AndroidManifest Fixes

### 🔴 4.1 Disable Backup for Patient Data
**File:** `composeApp/src/androidMain/AndroidManifest.xml` — Line 16

**Current:**
```xml
android:allowBackup="true"
```

**Required:**
```xml
android:allowBackup="false"
```

> With `allowBackup="true"`, anyone with a USB cable can run `adb backup`
> and extract the patient's full session data without root access.

---

### 🔴 4.2 Remove `showOnLockScreen` from Main Activity
**File:** `composeApp/src/androidMain/AndroidManifest.xml` — Line 28

**Current:**
```xml
android:showOnLockScreen="true"
```

**Required:** Remove this attribute from `MainActivity`.
If you need the video call screen to appear on the lock screen, apply it only
to a dedicated `CallActivity`, not the main activity.

> With this enabled, a patient's dashboard (doses, names, medical data)
> is visible to anyone who picks up the phone — without unlocking it.

---

### 🟠 4.3 Add `usesCleartextTraffic="false"` Explicitly
**File:** `composeApp/src/androidMain/AndroidManifest.xml`

```xml
<application
    android:usesCleartextTraffic="false"
    ...>
```

---

## 5. Login & Authentication

### 🟠 5.1 Add Email Format Validation
**File:** `composeApp/src/commonMain/kotlin/com/org/patientchakravue/ui/LoginScreen.kt` — Line 174

**Add before calling API:**
```kotlin
if (!email.contains("@") || !email.contains(".")) {
    showSnackbar("Please enter a valid email address")
    return@Button
}
if (password.trim().length < 6) {
    showSnackbar("Password must be at least 6 characters")
    return@Button
}
```

---

### 🟠 5.2 Fix Password Trim — Do Not Alter the Password
**File:** `composeApp/src/commonMain/kotlin/com/org/patientchakravue/ui/LoginScreen.kt` — Line 182

**Current:**
```kotlin
val patient = api.login(email.trim(), password.trim())
```

**Required:**
```kotlin
val patient = api.login(email.trim(), password)  // trim email only, not password
```

> `password.trim()` silently alters passwords that start or end with a space.

---

### 🟠 5.3 Set Correct Keyboard Types on Login Fields
**File:** `composeApp/src/commonMain/kotlin/com/org/patientchakravue/ui/LoginScreen.kt`

**Add to email field:**
```kotlin
OutlinedTextField(
    ...
    keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Email,
        imeAction = ImeAction.Next
    )
)
```

**Add to password field:**
```kotlin
OutlinedTextField(
    ...
    keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Password,
        imeAction = ImeAction.Done
    ),
    keyboardActions = KeyboardActions(
        onDone = { /* trigger login */ }
    )
)
```

---

### 🟠 5.4 Add "Forgot Password" Flow
Currently there is no way for a patient to recover their account if they forget
their password. Add a "Forgot Password?" text button below the login button that
navigates to a password reset screen or opens a support contact.

---

### 🟠 5.5 Show Distinct Error Messages
**File:** `composeApp/src/commonMain/kotlin/com/org/patientchakravue/data/ApiRepository.kt`

Currently both "wrong password" and "server down/tunnel dead" show the same
snackbar. Distinguish them:
```kotlin
} catch (e: java.net.UnknownHostException) {
    throw Exception("NO_NETWORK")   // caller shows "Cannot reach server"
} catch (e: Exception) {
    throw Exception("LOGIN_FAILED") // caller shows "Invalid credentials"
}
```

---

## 6. Bug Fixes

### 🟠 6.1 Complete or Remove MedicineList Screen
**File:** `composeApp/src/commonMain/kotlin/com/org/patientchakravue/app/App.kt` — Line 270

Either implement `MedicineListScreen` composable with real content,
or comment out the `onNavigateToMedicineList` callback in `DashboardScreen`
so the button does not appear until the screen is ready.

---

### 🟠 6.2 Handle All API Errors with User Feedback
All screens currently show a blank/empty state when the API returns an error.
Add explicit error states:

```kotlin
var errorMessage by remember { mutableStateOf<String?>(null) }

// In LaunchedEffect:
val result = apiRepository.getTodayDoses(patient.id)
if (result.isEmpty()) {
    errorMessage = "Could not load doses. Pull down to retry."
}

// In UI:
errorMessage?.let {
    Text(it, color = Color.Red, modifier = Modifier.padding(16.dp))
}
```

---

## 7. Firebase & Push Notifications

### 🟠 7.1 Register Firebase SHA-1 for Release Keystore
After generating your release keystore, get its SHA-1 fingerprint:
```bash
keytool -list -v -keystore release.keystore -alias patientchakravue
```

Then add this SHA-1 to your Firebase project:
- Go to **Firebase Console → Project Settings → Your Android App → Add Fingerprint**

> Without this, FCM will not deliver notifications to release builds.

---

### 🟠 7.2 Do Not Commit google-services.json to a Public Repository
**File:** `composeApp/google-services.json`

Add to `.gitignore`:
```
composeApp/google-services.json
```

Use CI/CD environment secrets to inject this file during the build pipeline.

---

### 🟠 7.3 Use a Custom App Icon for Notifications
**File:** `composeApp/src/androidMain/kotlin/com/org/patientchakravue/firebase/FirebaseService.kt` — Line 136

**Current (uses generic Android system icon):**
```kotlin
.setSmallIcon(android.R.drawable.ic_dialog_info)
```

**Required:**
```kotlin
.setSmallIcon(R.drawable.ic_notification)  // your own white/transparent icon
```

> Play Store review guidelines require a properly branded notification icon.
> The system `ic_dialog_info` icon will be rejected or look unprofessional.

---

## 8. Play Store Metadata & Assets

### 🔴 8.1 Privacy Policy (Mandatory for Healthcare Apps)
A Privacy Policy URL is **required** by Google Play for any app that collects
personal or medical data. This app collects:
- Name, age, sex, email, phone
- Medical history and prescriptions
- Vision test results
- Camera/microphone data (video calls)

**Action required:**
1. Draft a Privacy Policy (consult a legal professional)
2. Host it at a public URL (e.g. `https://chakravue.in/privacy`)
3. Add the URL in Play Console → **App Content → Privacy Policy**

---

### 🔴 8.2 Data Safety Form (Mandatory)
In Play Console → **App Content → Data Safety**, declare:

| Data Type | Collected | Shared | Purpose |
|-----------|-----------|--------|---------|
| Name | Yes | No | App functionality |
| Email | Yes | No | Authentication |
| Phone number | Yes | No | App functionality |
| Health info (doses, vision) | Yes | No | App functionality |
| Device or other IDs (FCM token) | Yes | Yes (Firebase) | Push notifications |
| Photos/videos | Yes | Yes (backend) | Eye image uploads |
| Camera | Yes | No | Video calls & eye tests |
| Microphone | Yes | No | Video calls |

---

### 🔴 8.3 Required Assets for Play Store Listing

| Asset | Size | Status |
|-------|------|--------|
| App icon | 512×512 PNG | ✅ Have adaptive icons |
| Feature graphic | 1024×500 PNG | ❌ Missing |
| Phone screenshots | Min 2, max 8 (16:9 or 9:16) | ❌ Missing |
| Short description | Max 80 characters | ❌ Missing |
| Full description | Max 4000 characters | ❌ Missing |
| App category | Medical | ❌ Not set |
| Content rating | Must complete questionnaire | ❌ Not done |

---

### 🟠 8.4 Content Rating Questionnaire
In Play Console → **App Content → Content Rating**, complete the IARC questionnaire.
For a medical app with:
- Video calling (camera + microphone)
- No user-generated public content
- No violence/mature content

Expected rating: **Everyone (PEGI 3)**

---

### 🟠 8.5 Target Audience
In Play Console → **App Content → Target Audience**:
- Set to **18 and over** (medical application, not for children)
- This avoids COPPA/Children's Online Privacy requirements

---

## 9. Backend Requirements

### 🔴 9.1 Deploy to a Stable Production Server
Stop using the Cloudflare free tunnel. Deploy the Python backend to one of:
- **Render** (free tier available, stable URLs)
- **Railway**
- **AWS EC2 / Lightsail**
- **Google Cloud Run**
- **DigitalOcean App Platform**

The production URL must:
- Be HTTPS with a valid SSL certificate
- Have a fixed domain (not a random tunnel subdomain)
- Have uptime monitoring configured

---

### 🔴 9.2 Implement JWT Authentication on Backend
The backend `/login` endpoint must return a token:
```json
{
  "_id": "patient123",
  "name": "John Doe",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

All other endpoints must:
1. Require `Authorization: Bearer <token>` header
2. Validate the token
3. Confirm the `patient_id` in the request matches the token's subject
4. Return `401 Unauthorized` for missing/invalid tokens

---

### 🔴 9.3 Rate Limit the Login Endpoint
```
POST /login → max 5 attempts per IP per 15 minutes
```
Return `429 Too Many Requests` when exceeded.

---

### 🟠 9.4 Add CORS Headers for Security
Restrict CORS to only your app's origins (not `*`).

---

## 10. Final Release Checklist

Run through every item below before clicking **"Submit for Review"** in Play Console.

### Code
- [ ] `BASE_URL` replaced with production API URL
- [ ] HTTP timeouts added to Ktor client
- [ ] Auth token attached to all API requests
- [ ] MedicineList screen implemented or removed
- [ ] Locale filter fixed (`"te"` not `"ta"`)
- [ ] Blank screen null-session fallbacks added
- [ ] `getCallToken` uses POST body not query param
- [ ] Debug `println` statements removed
- [ ] FCM token logic — no delete on every launch
- [ ] `liveCallData` reset after consuming

### Security
- [ ] Patient data stored in `EncryptedSharedPreferences`
- [ ] `network_security_config.xml` created and referenced in manifest
- [ ] Certificate pinning configured for production domain
- [ ] `allowBackup="false"` in manifest
- [ ] `showOnLockScreen` removed from MainActivity
- [ ] `usesCleartextTraffic="false"` in manifest

### Build
- [ ] Release keystore generated and stored safely (offline backup)
- [ ] Signing config added to `build.gradle.kts`
- [ ] `versionCode` incremented
- [ ] `versionName` updated (e.g. `"1.0.0"`)
- [ ] `buildConfig = true` enabled
- [ ] `./gradlew :composeApp:bundleRelease` runs without errors
- [ ] AAB file tested on a physical device (not emulator)

### Firebase
- [ ] Release keystore SHA-1 added to Firebase Console
- [ ] FCM notifications tested on release build
- [ ] `google-services.json` removed from public git history

### Play Store
- [ ] Privacy Policy hosted and URL added
- [ ] Data Safety form completed
- [ ] Feature graphic (1024×500) uploaded
- [ ] Minimum 2 phone screenshots uploaded
- [ ] Short description written (max 80 chars)
- [ ] Full description written (max 4000 chars)
- [ ] Content rating questionnaire completed
- [ ] Target audience set to 18+
- [ ] App category set to Medical

### Backend
- [ ] Deployed to stable production server (not Cloudflare tunnel)
- [ ] HTTPS certificate valid and not self-signed
- [ ] JWT authentication implemented on all endpoints
- [ ] Login rate limiting active (5 attempts / 15 min)
- [ ] Backend tested with release APK before submission

---

*Generated by production readiness audit of PatientChakraVue — 2026-06-01*
