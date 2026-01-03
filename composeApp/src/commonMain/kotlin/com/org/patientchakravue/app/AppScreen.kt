package com.org.patientchakravue.app

import com.org.patientchakravue.model.DoctorNote

sealed class Screen {
    data object Login : Screen()
    data object Dashboard : Screen()
    data object Profile : Screen()
    data object AdherenceGraph : Screen()
    data object MedicineList : Screen()
    data object AfterCare : Screen()
    data object Vision : Screen()
    data object Notifications : Screen()
    data object AmslerGrid : Screen()
    data object TumblingE : Screen()
    // New route for feedback detail
    data class FeedbackDetail(val note: DoctorNote) : Screen()
    data class Chat(val submissionId: String) : Screen()
}
