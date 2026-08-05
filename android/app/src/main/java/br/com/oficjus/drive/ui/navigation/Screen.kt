package br.com.oficjus.drive.ui.navigation

sealed class Screen(val route: String) {
    data object WazeGate : Screen("waze_gate")
    data object Login : Screen("login")
    data object Home : Screen("home")
    data object RouteBuild : Screen("route_build")
    data object RouteActive : Screen("route_active/{rotaId}") {
        fun createRoute(rotaId: String) = "route_active/$rotaId"
    }
    data object Navigation : Screen("navigation/{rotaId}/{paradaIndex}") {
        fun createRoute(rotaId: String, paradaIndex: Int) = "navigation/$rotaId/$paradaIndex"
    }
    data object PauseRoute : Screen("pause_route/{rotaId}") {
        fun createRoute(rotaId: String) = "pause_route/$rotaId"
    }
    data object FinalSummary : Screen("final_summary/{rotaId}") {
        fun createRoute(rotaId: String) = "final_summary/$rotaId"
    }
}