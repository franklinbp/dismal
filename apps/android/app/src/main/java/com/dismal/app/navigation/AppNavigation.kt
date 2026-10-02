package com.dismal.app.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dismal.app.ui.dashboard.DashboardScreen
import com.dismal.app.ui.dashboard.DashboardViewModel
import com.dismal.app.ui.login.LoginScreen
import com.dismal.app.ui.login.LoginViewModel
import com.dismal.app.ui.session.BootScreen
import com.dismal.app.ui.session.SessionViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "boot") {
        composable("boot") {
            val sessionViewModel: SessionViewModel = hiltViewModel()
            BootScreen()
            val destination = sessionViewModel.startDestination
            if (destination != null) {
                androidx.compose.runtime.LaunchedEffect(destination) {
                    navController.navigate(destination) {
                        popUpTo("boot") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        }
        composable("login") {
            val loginViewModel: LoginViewModel = hiltViewModel()
            LoginScreen(
                viewModel = loginViewModel,
                onLoginSuccess = {
                    navController.navigate("dashboard") {
                        popUpTo("login") { inclusive = true }
                    }
                },
            )
        }
        composable("dashboard") {
            val dashboardViewModel: DashboardViewModel = hiltViewModel()
            DashboardScreen(
                viewModel = dashboardViewModel,
                onLoggedOut = {
                    navController.navigate("login") {
                        popUpTo("dashboard") { inclusive = true }
                    }
                },
            )
        }
    }
}
