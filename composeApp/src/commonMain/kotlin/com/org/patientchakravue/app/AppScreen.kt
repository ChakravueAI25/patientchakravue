package com.org.patientchakravue.app

import com.org.patientchakravue.model.DoctorNote

sealed class Screen {
    data object Login : Screen()
    data object Dashboard : Screen()
    data object AfterCare : Screen()
    data object Vision : Screen()
    data object Notifications : Screen()
    data object Profile : Screen() // Added from previous context
    data object AdherenceGraph: Screen() // Added from previous context
    data object MedicineList: Screen() // Added from previous context

    data object AmslerGrid : Screen()
    data object TumblingE : Screen()

    data class Chat(val doctorId: String, val doctorName: String, val submissionIds: List<String>) : Screen() // Added from previous context
    data class FeedbackDetail(val note: DoctorNote) : Screen()
    data object VideoCallRequest : Screen()
}
