package br.com.oficjus.drive.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import br.com.oficjus.drive.ui.activeRoute.ActiveRouteScreen
import br.com.oficjus.drive.ui.login.LoginScreen
import br.com.oficjus.drive.ui.routebuild.RouteBuildScreen
import br.com.oficjus.drive.ui.wazegate.WazeGateScreen

@Composable
fun DriveNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.WazeGate.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.WazeGate.route) {
            WazeGateScreen(
                onWazeConfirmado = {
                    navController.navigate(Screen.RouteBuild.route) {
                        popUpTo(Screen.WazeGate.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.RouteBuild.route) {
            RouteBuildScreen(
                onRotaConfirmada = { rotaId ->
                    navController.navigate(Screen.RouteActive.createRoute(rotaId)) {
                        popUpTo(Screen.RouteBuild.route)
                    }
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.RouteBuild.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.RouteActive.route,
            arguments = listOf(navArgument("rotaId") { type = NavType.StringType })
        ) { backStackEntry ->
            val rotaId = backStackEntry.arguments?.getString("rotaId") ?: return@composable
            ActiveRouteScreen(
                rotaId = rotaId,
                onVoltar = {
                    navController.popBackStack(Screen.RouteBuild.route, inclusive = false)
                }
            )
        }
    }
}