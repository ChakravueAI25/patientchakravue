package com.org.patientchakravue.app

/**
 * Central legal / consent configuration.
 *
 * ⚠️ EDIT THESE WHEN THE HOSTING OR URLS CHANGE — this is the single place.
 * Both the doctor and patient apps currently point at the same chakravue.ai URLs;
 * switch to app-specific paths here when ready.
 */
object LegalConfig {
    /** Full Terms of Service (opened in the browser from the consent screen). */
    const val TERMS_URL = "https://www.chakravue.ai/terms"

    /** Full Privacy Policy. */
    const val PRIVACY_URL = "https://www.chakravue.ai/privacy-policy"

    /**
     * Bump this whenever the Terms/Privacy text changes — every user will then be
     * asked to re-accept the new version on their next login.
     */
    const val TERMS_VERSION = 1

    /** Short in-app summary (the binding full text lives at the URLs above). */
    const val SUMMARY = """ChakraVue Patient helps you stay connected with your healthcare provider.

• Medical disclaimer: This app is not a medical device and does not diagnose, treat, cure or prevent any condition. It is not a substitute for professional medical advice. In an emergency, contact your local emergency services immediately.

• Eligibility: You must be 18 or older, or use the app under the supervision and consent of a parent or legal guardian.

• The service: Appointment and medicine reminders, symptom check-ins, vision tests, access to reports, and secure messaging and video consultation with your provider.

• Your account: Provide accurate information and keep your login credentials confidential.

• Your data: You are responsible for the accuracy of what you submit; we process it only to provide your care.

• Third-party services: The app uses Google Firebase (notifications) and Agora (video/audio calls); your use of those features is also subject to their terms.

By continuing you agree to the full Terms of Service and Privacy Policy."""
}
