package br.com.oficjus.drive.ui.navigation

sealed class Screen(val route: String) {
    data object WazeGate : Screen("waze_gate")
    data object Login : Screen("login")
    data object RouteBuild : Screen("route_build")
    data object RouteActive : Screen("route_active/{rotaId}") {
        fun createRoute(rotaId: String) = "route_active/$rotaId"
    }
}