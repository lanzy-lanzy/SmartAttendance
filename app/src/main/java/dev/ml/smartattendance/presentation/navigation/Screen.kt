package dev.ml.smartattendance.presentation.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Attendance : Screen("attendance/{eventId}") {
        fun createRoute(eventId: String) = "attendance/$eventId"
    }
    object StudentManagement : Screen("student_management")
    object EventManagement : Screen("event_management")
    object EventDetail : Screen("event_detail/{eventId}") {
        fun createRoute(eventId: String) = "event_detail/$eventId"
    }
    object AdminDashboard : Screen("admin_dashboard")
    object Login : Screen("login")
    object Register : Screen("register")
}