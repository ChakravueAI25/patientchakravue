package com.org.patientchakravue.app

sealed class Screen {
    data object Login : Screen()
    data object Dashboard : Screen()
    data object Profile : Screen()
    data object AdherenceGraph : Screen()
    data object MedicineList : Screen()
    data object AfterCare : Screen()
    data object Vision : Screen()
    data object Notifications : Screen()
}
