package shop.devosify.riya.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import shop.devosify.riya.ui.screens.ConversationHistoryScreen
import shop.devosify.riya.ui.screens.HomeScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object History : Screen("history")
}

@Composable
fun RiyaNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onViewHistory = {
                    navController.navigate(Screen.History.route)
                }
            )
        }
        
        composable(Screen.History.route) {
            ConversationHistoryScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
} 