package com.example.parentapp

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.parentapp.ui.screens.HistoryScreen
import com.example.parentapp.ui.screens.HomeScreen
import com.example.parentapp.ui.screens.LoginScreen
import com.example.parentapp.ui.theme.ParentAppTheme
import com.example.parentapp.ui.ParentViewModel

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ParentAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
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
    vm: ParentViewModel = viewModel()
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, vm) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> vm.startRealtime()
                Lifecycle.Event.ON_STOP -> vm.stopRealtime()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            vm.stopRealtime()
        }
    }
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Routes.Login

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
                },
                currentRoute = currentRoute,
                onNavigate = { route ->
                    if (route != currentRoute) {
                        navController.navigate(route) {
                            popUpTo(Routes.Home) {
                                inclusive = (route == Routes.Home)
                            }
                            launchSingleTop = true
                        }
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
