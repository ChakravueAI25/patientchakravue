package com.org.patientchakravue.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Define supported languages for the app.
 * The 'code' should match the folder suffix (e.g., values-hi for Hindi)
 */
enum class AppLanguage(val code: String, val displayName: String) {
    English("en", "English"),
    Hindi("hi", "हिन्दी"),
    Telugu("te", "తెలుగు")
}

/**
 * A simple manager class to hold the current language state.
 * This state can be observed throughout the app and triggers recomposition.
 */
class LanguageManager {
    var currentLanguage by mutableStateOf(AppLanguage.English)
        private set

    // This counter forces recomposition when language changes
    var languageChangeCounter by mutableStateOf(0)
        private set

    fun changeLanguage(language: AppLanguage) {
        if (currentLanguage != language) {
            currentLanguage = language
            languageChangeCounter++ // Force recomposition
            println("[Localization] Language changed to: ${language.displayName} (${language.code})")
        }
    }
}

/**
 * CompositionLocal provider that makes the LanguageManager accessible
 * anywhere in the composable tree without explicitly passing it.
 */
val LocalLanguageManager = compositionLocalOf<LanguageManager> {
    error("No LanguageManager provided. Did you forget to wrap with AppLocalizationProvider?")
}

/**
 * Wrapper composable that provides the LanguageManager to all children.
 * This should wrap your root App content.
 */
@Composable
fun AppLocalizationProvider(content: @Composable () -> Unit) {
    val languageManager = remember { LanguageManager() }

    CompositionLocalProvider(LocalLanguageManager provides languageManager) {
        content()
    }
}

/**
 * In-memory string maps for each language.
 * This is used for runtime language switching since Compose Multiplatform
 * stringResource() follows device locale, not app-internal state.
 */
object AppStrings {

    private val englishStrings = mapOf(
        "app_name" to "Patient Chakravue",
        "error_connection" to "Connection Error",
        "loading" to "Loading...",
        "login_title" to "Login",
        "email_label" to "Email",
        "password_label" to "Password",
        "login_button" to "LOGIN",
        "login_error_empty" to "Fill all fields",
        "login_success" to "Welcome back!",
        "login_error_invalid" to "Invalid credentials",
        "dashboard_title" to "Dashboard",
        "section_medicines" to "MEDICINES",
        "doses_taken_today" to "Doses Taken Today",
        "section_prescription" to "PRESCRIPTION",
        "doctor_label" to "Doctor",
        "next_appointment" to "Next Appointment:",
        "not_scheduled" to "Not scheduled",
        "no_doses_today" to "No doses scheduled for today.",
        "check_back_tomorrow" to "Check back tomorrow!",
        "btn_mark_taken" to "Mark as Taken",
        "btn_taken" to "Taken",
        "daily_checkin_title" to "Daily Eye Check-In",
        "daily_checkin_subtitle" to "Tell us how your eye feels today",
        "symptom_blurred_vision" to "Blurred Vision",
        "symptom_pain" to "Pain",
        "symptom_redness" to "Redness",
        "symptom_watering" to "Watering",
        "symptom_itching" to "Itching/Irritation",
        "symptom_discharge" to "Discharge",
        "add_photo_btn" to "Add Eye Photo (Recommended)",
        "photo_added_btn" to "Photo Added ✓",
        "comment_hint" to "Describe anything unusual...",
        "error_no_photo" to "Please add a photo of your eye",
        "submit_success" to "Report submitted successfully!",
        "submit_failure" to "Failed to submit. Check your internet connection.",
        "submit_btn" to "SUBMIT",
        "level_none" to "None",
        "level_mild" to "Mild",
        "level_moderate" to "Moderate",
        "level_severe" to "Severe",
        "vision_title" to "Vision Tests",
        "vision_subtitle" to "Select a test to begin",
        "test_amsler" to "Amsler\nGrid",
        "test_tumbling" to "Tumbling E\nTest",
        "recent_results" to "Recent Results",
        "no_history" to "No test history found.",
        "eye_label" to "Eye:",
        "result_label" to "Result:",
        "amsler_title" to "Amsler Grid Test",
        "instructions_title" to "Instructions",
        "select_eye" to "Select Eye to Test:",
        "eye_right" to "Right Eye",
        "eye_left" to "Left Eye",
        "start_test_btn" to "Start Test",
        "draw_hint" to "Draw over areas that look wavy or blurry.",
        "clear_btn" to "Clear",
        "acuity_title" to "Visual Acuity (3m)",
        "visual_acuity_test" to "Visual Acuity Test",
        "tumbling_inst_1" to "1. Place phone exactly 3 meters away.",
        "tumbling_inst_2" to "2. Cover the EYE NOT being tested.",
        "tumbling_inst_3" to "3. Identify the direction of the E.",
        "tumbling_inst_4" to "4. Swipe in the direction the E is pointing.",
        "amsler_inst_1" to "1. Hold phone 12 inches (30cm) away.",
        "amsler_inst_2" to "2. Cover one eye.",
        "amsler_inst_3" to "3. Focus on the center black dot.",
        "amsler_inst_4" to "4. If lines look wavy, blurred, or missing, mark them on the next screen.",
        "mark_distortions" to "Mark Distortions",
        "swipe_hint" to "Swipe in the direction the 'E' is pointing",
        "test_complete" to "Test Complete",
        "tested_eye" to "Tested Eye:",
        "visual_acuity_label" to "Visual Acuity",
        "save_record_btn" to "Save to Record",
        "retake_btn" to "Retake Test",
        "results_saved" to "Results saved!",
        "submission_failed" to "Submission failed.",
        "summary_total" to "Total doses taken in this view:",
        "desc_day" to "This shows your intake pattern for today.",
        "desc_week" to "Consistency over the last 7 days.",
        "desc_medicine" to "Breakdown by medicine type.",
        "adherence_title" to "Adherence Analysis",
        "view_by" to "View By",
        "view_day" to "Day",
        "view_week" to "Week",
        "view_medicine" to "Medicine",
        "summary_title" to "Summary",
        "no_data" to "No data available",
        "profile_title" to "Profile",
        "basic_info" to "Basic Information",
        "contact_info" to "Contact Information",
        "emergency_contact" to "Emergency Contact",
        "system_info" to "System Information",
        "logout_btn" to "Logout",
        "notifications_title" to "Hospital Updates",
        "notifications_subtitle" to "View messages and notes from your doctor",
        "no_notifications" to "No new notifications from the hospital.",
        "label_name" to "Full Name",
        "label_age" to "Age",
        "label_sex" to "Sex",
        "label_blood" to "Blood Group",
        "label_phone" to "Phone Number",
        "label_email" to "Email Address",
        "label_address" to "Address",
        "label_patient_id" to "Patient ID",
        "label_registered" to "Registered On",
        "report_detail_title" to "Report Detail",
        "doctor_advice" to "Doctor's Advice",
        "submitted_symptoms" to "Submitted Symptoms",
        "your_comments" to "Your Comments:",
        // New keys used by NotificationsScreen
        "hospital_updates" to "Doctor Updates",
        "view_messages" to "View Messages",
        "dr_online" to "Online",
        "message_placeholder" to "Message",
        "report_summary" to "Report Summary",
        "login_caption" to "ChakraVue Healthcare"
    )

    private val hindiStrings = mapOf(
        "app_name" to "रोगी चक्रव्यूह",
        "error_connection" to "कनेक्शन त्रुटि",
        "loading" to "लोड हो रहा है...",
        "login_title" to "लॉगिन",
        "email_label" to "ईमेल",
        "password_label" to "पासवर्ड",
        "login_button" to "लॉगिन",
        "login_error_empty" to "सभी फ़ील्ड भरें",
        "login_success" to "वापसी पर स्वागत है!",
        "login_error_invalid" to "अमान्य क्रेडेंशियल",
        "dashboard_title" to "डैशबोर्ड",
        "section_medicines" to "दवाइयाँ",
        "doses_taken_today" to "आज ली गई खुराक",
        "section_prescription" to "पर्चा (प्रिस्क्रिप्शन)",
        "doctor_label" to "डॉक्टर",
        "next_appointment" to "अगली नियुक्ति:",
        "not_scheduled" to "तय नहीं है",
        "no_doses_today" to "आज के लिए कोई खुराक निर्धारित नहीं है।",
        "check_back_tomorrow" to "कल वापस चेक करें!",
        "btn_mark_taken" to "लिया हुआ मार्क करें",
        "btn_taken" to "लिया हुआ",
        "daily_checkin_title" to "दैनिक नेत्र जांच",
        "daily_checkin_subtitle" to "हमें बताएं कि आज आपकी आंख कैसी है",
        "symptom_blurred_vision" to "धुंधली दृष्टि",
        "symptom_pain" to "दर्द",
        "symptom_redness" to "लालिमा",
        "symptom_watering" to "पानी आना",
        "symptom_itching" to "खुजली / जलन",
        "symptom_discharge" to "रिसाव (Discharge)",
        "add_photo_btn" to "आंख की फोटो जोड़ें (अनुशंसित)",
        "photo_added_btn" to "फोटो जोड़ी गई ✓",
        "comment_hint" to "कुछ भी असामान्य बताएं...",
        "error_no_photo" to "कृपया अपनी आंख की एक फोटो जोड़ें",
        "submit_success" to "रिपोर्ट सफलतापूर्वक जमा हो गई!",
        "submit_failure" to "जमा करने में विफल। अपना इंटरनेट चेक करें।",
        "submit_btn" to "जमा करें",
        "level_none" to "कुछ नहीं",
        "level_mild" to "हल्का",
        "level_moderate" to "मध्यम",
        "level_severe" to "गंभीर",
        "vision_title" to "दृष्टि परीक्षण",
        "vision_subtitle" to "शुरू करने के लिए एक परीक्षण चुनें",
        "test_amsler" to "एम्सलर\nग्रिड",
        "test_tumbling" to "टंबलिंग ई\nटेस्ट",
        "recent_results" to "हाल के परिणाम",
        "no_history" to "कोई पिछला इतिहास नहीं मिला।",
        "eye_label" to "आंख:",
        "result_label" to "परिणाम:",
        "amsler_title" to "एम्सलर ग्रिड टेस्ट",
        "instructions_title" to "निर्देश",
        "select_eye" to "परीक्षण के लिए आंख चुनें:",
        "eye_right" to "दाहिनी आंख",
        "eye_left" to "बाईं आंख",
        "start_test_btn" to "टेस्ट शुरू करें",
        "draw_hint" to "उन क्षेत्रों पर ड्रा करें जो लहरदार या धुंधले दिखते हैं।",
        "clear_btn" to "साफ़ करें",
        "acuity_title" to "विजुअल एक्युटी (3 मी)",
        "visual_acuity_test" to "विजुअल एक्युटी टेस्ट",
        "tumbling_inst_1" to "1. फोन को ठीक 3 मीटर दूर रखें।",
        "tumbling_inst_2" to "2. उस आंख को ढकें जिसका परीक्षण नहीं हो रहा है।",
        "tumbling_inst_3" to "3. 'E' की दिशा को पहचानें।",
        "tumbling_inst_4" to "4. जिस दिशा में 'E' इशारा कर रहा है, उस ओर स्वाइप करें।",
        "amsler_inst_1" to "1. फोन को 12 इंच (30 सेमी) दूर रखें।",
        "amsler_inst_2" to "2. एक आंख को ढक लें।",
        "amsler_inst_3" to "3. केंद्र में काले बिंदु पर ध्यान केंद्रित करें।",
        "amsler_inst_4" to "4. यदि रेखाएं लहरदार, धुंधली या गायब दिखती हैं, तो उन्हें अगली स्क्रीन पर चिह्नित करें।",
        "mark_distortions" to "विकृतियों को चिह्नित करें",
        "swipe_hint" to "जिस दिशा में 'E' है, उस ओर स्वाइप करें",
        "test_complete" to "टेस्ट पूरा हुआ",
        "tested_eye" to "परीक्षण की गई आंख:",
        "visual_acuity_label" to "विजुअल एक्युटी",
        "save_record_btn" to "रिकॉर्ड में सहेजें",
        "retake_btn" to "फिर से टेस्ट करें",
        "results_saved" to "परिणाम सहेजे गए!",
        "submission_failed" to "जमा करने में विफल।",
        "summary_total" to "इस दृश्य में ली गई कुल खुराक:",
        "desc_day" to "यह आज के लिए आपकी खुराक का पैटर्न दिखाता है।",
        "desc_week" to "पिछले 7 दिनों में निरंतरता।",
        "desc_medicine" to "दवा के प्रकार के अनुसार विवरण।",
        "adherence_title" to "अनुपालन विश्लेषण",
        "view_by" to "देखें द्वारा",
        "view_day" to "दिन",
        "view_week" to "सप्ताह",
        "view_medicine" to "दवा",
        "summary_title" to "सारांश",
        "no_data" to "कोई डेटा उपलब्ध नहीं है",
        "profile_title" to "प्रोफ़ाइल",
        "basic_info" to "मूल जानकारी",
        "contact_info" to "संपर्क जानकारी",
        "emergency_contact" to "आपातकालीन संपर्क",
        "system_info" to "सिस्टम जानकारी",
        "logout_btn" to "लॉग आउट",
        "notifications_title" to "अस्पताल अपडेट",
        "notifications_subtitle" to "अपने डॉक्टर के संदेश और नोट्स देखें",
        "no_notifications" to "अस्पताल से कोई नई सूचना नहीं है।",
        "label_name" to "पूरा नाम",
        "label_age" to "उम्र",
        "label_sex" to "लिंग",
        "label_blood" to "ब्लड ग्रुप",
        "label_phone" to "फ़ोन नंबर",
        "label_email" to "ईमेल पता",
        "label_address" to "पता",
        "label_patient_id" to "रोगी आईडी",
        "label_registered" to "पंजीकृत तिथि",
        "report_detail_title" to "रिपोर्ट विवरण",
        "doctor_advice" to "डॉक्टर की सलाह",
        "submitted_symptoms" to "जमा किए गए लक्षण",
        "your_comments" to "आपकी टिप्पणियाँ:",
        "hospital_updates" to "डॉक्टर अपडेट्स",
        "view_messages" to "संदेश देखें",
        "dr_online" to "ऑनलाइन",
        "message_placeholder" to "संदेश",
        "report_summary" to "रिपोर्ट सारांश",
        "login_caption" to "चक्रव्यू हेल्थकेयर"
    )

    private val teluguStrings = mapOf(
        "app_name" to "పేషెంట్ చక్రవ్యూ",
        "error_connection" to "కనెక్షన్ లోపం",
        "loading" to "లోడ్ అవుతోంది...",
        "login_title" to "లాగిన్",
        "email_label" to "ఈమెయిల్",
        "password_label" to "పాస్‌వర్డ్",
        "login_button" to "లాగిన్",
        "login_error_empty" to "అన్ని వివరాలను పూరించండి",
        "login_success" to "స్వాగతం!",
        "login_error_invalid" to "సరికాని వివరాలు",
        "dashboard_title" to "డ్యాష్‌బోర్డ్",
        "section_medicines" to "మందులు",
        "doses_taken_today" to "ఈ రోజు తీసుకున్న మోతాదులు",
        "section_prescription" to "మందుల చీటి (Prescription)",
        "doctor_label" to "డాక్టర్",
        "next_appointment" to "తదుపరి అపాయింట్‌మెంట్:",
        "not_scheduled" to "షెడ్యూల్ చేయలేదు",
        "no_doses_today" to "ఈ రోజు మందులు షెడ్యూల్ చేయలేదు.",
        "check_back_tomorrow" to "రేపు మళ్ళీ చూడండి!",
        "btn_mark_taken" to "వాడినట్లు గుర్తించు",
        "btn_taken" to "వాడారు",
        "daily_checkin_title" to "రోజువారీ కంటి తనిఖీ",
        "daily_checkin_subtitle" to "ఈ రోజు మీ కన్ను ఎలా ఉందో మాకు చెప్పండి",
        "symptom_blurred_vision" to "మసకబారిన దృష్టి",
        "symptom_pain" to "నొప్పి",
        "symptom_redness" to "ఎరుపు",
        "symptom_watering" to "నీరు కారడం",
        "symptom_itching" to "దురద / మంట",
        "symptom_discharge" to "కంటి నుండి స్రావం (Discharge)",
        "add_photo_btn" to "కంటి ఫోటోను జోడించండి (సిఫార్సు చేయబడింది)",
        "photo_added_btn" to "ఫోటో జోడించబడింది ✓",
        "comment_hint" to "ఏదైనా అసాధారణంగా ఉంటే వివరించండి...",
        "error_no_photo" to "దయచేసి మీ కంటి ఫోటోను జోడించండి",
        "submit_success" to "రిపోర్ట్ విజయవంతంగా సమర్పించబడింది!",
        "submit_failure" to "సమర్పించడం విఫలమైంది. మీ ఇంటర్నెట్‌ని తనిఖీ చేయండి.",
        "submit_btn" to "సమర్పించు",
        "level_none" to "ఏమీ లేదు",
        "level_mild" to "కొంచెం",
        "level_moderate" to "మధ్యస్థం",
        "level_severe" to "తీవ్రమైన",
        "vision_title" to "దృష్టి పరీక్షలు",
        "vision_subtitle" to "ప్రారంభించడానికి ఒక పరీక్షను ఎంచుకోండి",
        "test_amsler" to "ఆమ్స్లర్\nగ్రిడ్",
        "test_tumbling" to "టంబ్లింగ్ E\nటెస్ట్",
        "recent_results" to "ఇటీవలి ఫలితాలు",
        "no_history" to "పాత రికార్డులు ఏవీ కనుగొనబడలేదు.",
        "eye_label" to "కన్ను:",
        "result_label" to "ఫలితం:",
        "amsler_title" to "ఆమ్స్లర్ గ్రిడ్ టెస్ట్",
        "instructions_title" to "సూచనలు",
        "select_eye" to "పరీక్షించాల్సిన కన్ను ఎంచుకోండి:",
        "eye_right" to "కుడి కన్ను",
        "eye_left" to "ఎడమ కన్ను",
        "start_test_btn" to "టెస్ట్ ప్రారంభించు",
        "draw_hint" to "వంకరగా లేదా మసకగా కనిపించే ప్రాంతాలపై గీయండి.",
        "clear_btn" to "క్లియర్",
        "acuity_title" to "దృష్టి తీక్షణత (3 మీ)",
        "visual_acuity_test" to "విజువల్ ఎక్యుటీ టెస్ట్",
        "tumbling_inst_1" to "1. ఫోన్‌ను సరిగ్గా 3 మీటర్ల దూరంలో ఉంచండి.",
        "tumbling_inst_2" to "2. పరీక్షించని కన్నును మూసుకోండి.",
        "tumbling_inst_3" to "3. 'E' ఏ దిశలో ఉందో గుర్తించండి.",
        "tumbling_inst_4" to "4. 'E' చూపిస్తున్న దిశలో స్వైప్ చేయండి.",
        "amsler_inst_1" to "1. ఫోన్‌ను 12 అంగుళాల (30 సెం.మీ) దూరంలో ఉంచండి.",
        "amsler_inst_2" to "2. ఒక కన్ను మూసుకోండి.",
        "amsler_inst_3" to "3. మధ్యలో ఉన్న నల్లని చుక్కపై దృష్టి పెట్టండి.",
        "amsler_inst_4" to "4. గీతలు వంకరగా లేదా మసకగా కనిపిస్తే, తదుపరి స్క్రీన్‌లో గుర్తించండి.",
        "mark_distortions" to "లోపాలను గుర్తించండి",
        "swipe_hint" to "'E' ఉన్న దిశలో స్వైప్ చేయండి",
        "test_complete" to "పరీక్ష పూర్తయింది",
        "tested_eye" to "పరీక్షించిన కన్ను:",
        "visual_acuity_label" to "దృష్టి తీక్షణత (Visual Acuity)",
        "save_record_btn" to "రికార్డులో సేవ్ చేయండి",
        "retake_btn" to "మళ్ళీ పరీక్షించు",
        "results_saved" to "ఫలితాలు సేవ్ చేయబడ్డాయి!",
        "submission_failed" to "సమర్పించడం విఫలమైంది.",
        "summary_total" to "మొత్తం తీసుకున్న మోతాదులు:",
        "desc_day" to "ఇది ఈ రోజు మీరు మందులు తీసుకున్న విధానాన్ని చూపిస్తుంది.",
        "desc_week" to "గత 7 రోజుల్లో మందుల వాడకం.",
        "desc_medicine" to "మందుల రకాన్ని బట్టి వివరాలు.",
        "adherence_title" to "మందుల వినియోగ విశ్లేషణ",
        "view_by" to "వీక్షణ విధానం",
        "view_day" to "రోజు",
        "view_week" to "వారం",
        "view_medicine" to "మందులు",
        "summary_title" to "సారాంశం",
        "no_data" to "డేటా అందుబాటులో లేదు",
        "profile_title" to "ప్రొఫైల్",
        "basic_info" to "ప్రాథమిక సమాచారం",
        "contact_info" to "సంప్రదింపు సమాచారం",
        "emergency_contact" to "అత్యవసర సంప్రదింపు",
        "system_info" to "సిస్టమ్ సమాచారం",
        "logout_btn" to "లాగ్ అవుట్",
        "notifications_title" to "ఆసుపత్రి అప్‌డేట్స్",
        "notifications_subtitle" to "మీ డాక్టర్ సందేశాలు మరియు నోట్స్ చూడండి",
        "no_notifications" to "ఆసుపత్రి నుండి కొత్త సందేశాలు ఏవీ లేవు.",
        "label_name" to "పూర్తి పేరు",
        "label_age" to "వయస్సు",
        "label_sex" to "లింగం",
        "label_blood" to "రక్త వర్గం",
        "label_phone" to "ఫోన్ నంబర్",
        "label_email" to "ఈమెయిల్ చిరునామా",
        "label_address" to "చిరునామా",
        "label_patient_id" to "పేషెంట్ ఐడి",
        "label_registered" to "నమోదు తేదీ",
        "report_detail_title" to "రిపోర్ట్ వివరాలు",
        "doctor_advice" to "డాక్టర్ సలహా",
        "submitted_symptoms" to "సమర్పించిన లక్షణాలు",
        "your_comments" to "మీ వ్యాఖ్యలు:",
        "hospital_updates" to "డాక్టర్ అప్డేట్స్",
        "view_messages" to "సందేశాలు చూడండి",
        "dr_online" to "ఆన్‌లైన్",
        "message_placeholder" to "సందేశం",
        "report_summary" to "నివేదిక సారాంశం",
        "login_caption" to "చక్రవ్యూ హెల్త్‌కేర్"
    )

    /**
     * Get a string for the given key based on the current language.
     */
    fun get(key: String, language: AppLanguage): String {
        val stringMap = when (language) {
            AppLanguage.English -> englishStrings
            AppLanguage.Hindi -> hindiStrings
            AppLanguage.Telugu -> teluguStrings
        }
        return stringMap[key] ?: englishStrings[key] ?: key
    }
}

/**
 * Composable helper function to get localized strings.
 * This reads from the in-memory AppStrings based on current language selection.
 */
@Composable
fun localizedString(key: String): String {
    val languageManager = LocalLanguageManager.current
    // Reading currentLanguage triggers recomposition when it changes
    return AppStrings.get(key, languageManager.currentLanguage)
}
