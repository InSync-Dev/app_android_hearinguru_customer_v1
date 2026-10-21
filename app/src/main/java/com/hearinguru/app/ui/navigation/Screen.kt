package com.hearinguru.app.ui.navigation

sealed class Screen(val route: String) {
    data object Tools          : Screen("tools")
    data object HearingTest    : Screen("hearing_test")
    data object TestActive     : Screen("test_active")
    data object TestResult     : Screen("test_result")
    data object SoundAmplifier : Screen("sound_amplifier")
    data object NoiseMeter     : Screen("noise_meter")
    data object Tinnitus       : Screen("tinnitus")
    data object Learn          : Screen("learn")
    data object HearingAids    : Screen("hearing_aids")
    data object SeeADoctor     : Screen("see_a_doctor")
    data object Notifications  : Screen("notifications")
    data object Profile        : Screen("profile")
    data object ArticleDetail  : Screen("article/{index}") {
        fun route(index: Int) = "article/$index"
    }
    data object LearningPath   : Screen("learning_path/{pathIndex}") {
        fun route(pathIndex: Int) = "learning_path/$pathIndex"
    }
}

val bottomNavItems = listOf(
    BottomNavItem(Screen.Tools,       "Tools",   "build"),
    BottomNavItem(Screen.Learn,       "Learn",   "menu_book"),
    BottomNavItem(Screen.HearingAids, "Aids",    "hearing_aid"),
    BottomNavItem(Screen.SeeADoctor,  "Doctor",  "medical_services"),
)

data class BottomNavItem(val screen: Screen, val label: String, val iconName: String)
