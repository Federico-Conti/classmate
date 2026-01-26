package com.example.parentapp

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.parentapp.ui.MyAppViewModel
import com.example.parentapp.ui.screens.HistoryScreen
import com.example.parentapp.ui.screens.HomeScreen
import com.example.parentapp.ui.screens.LoginScreen
import com.example.parentapp.ui.theme.ParentAppTheme

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ParentAppTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    ParentAppNavHost()
                }
            }
        }
    }
}

private object Routes {
    const val Login = "login"
    const val Home = "home"
    const val History = "history/{childId}"

    fun history(childId: String) = "history/$childId"
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun ParentAppNavHost(
    vm: MyAppViewModel = viewModel()
) {
    val navController = rememberNavController()

    // saved parentId in DS =>  Home
    val startDestination by vm.startDestination.collectAsState()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.Login) {
            LoginScreen(
                uiState = vm.loginUiState.collectAsState().value,
                parentIdInput = vm.parentIdInput.collectAsState().value,
                onParentIdChange = vm::onParentIdChange,
                onLoginClick = { vm.login() },
                onLoginSuccessNavigate = {
                    navController.navigate(Routes.Home) {
                        popUpTo(Routes.Login) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.Home) {
            HomeScreen(
                vm = vm,
                onOpenHistory = { childId ->
                    navController.navigate(Routes.history(childId))
                },
                onLogout = {
                    vm.logout()
                    navController.navigate(Routes.Login) {
                        popUpTo(Routes.Home) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.History,
            arguments = listOf(navArgument("childId") { type = NavType.StringType })
        ) { backStackEntry ->
            val childId = backStackEntry.arguments?.getString("childId").orEmpty()

            HistoryScreen(
                childId = childId,
                vm = vm,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
