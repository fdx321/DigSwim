package com.digswim.app.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.digswim.app.ui.home.HomeOverviewScreen
import com.digswim.app.ui.onboarding.UserRegistrationScreen
import com.digswim.app.ui.profile.ProfileScreen
import com.digswim.app.ui.profile.ProfileViewModel
import com.digswim.app.ui.garmin.GarminTestScreen
import com.digswim.app.ui.report.MonthlyReportScreen
import com.digswim.app.ui.detail.ActivityDetailScreen
import com.digswim.app.ui.theme.NeonGreen
import java.time.YearMonth

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home : Screen("home", "记录", Icons.Default.Home)
    object Profile : Screen("profile", "我的", Icons.Default.Person)
    // Garmin object removed from sealed class but keeping route string if needed elsewhere (unlikely)
}

@Composable
fun MainScreen(
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val userProfile by viewModel.userProfile.collectAsState()

    // Check loading state
    if (userProfile.nickname == "加载中...") {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = NeonGreen)
        }
        return
    }

    // Check if registration needed
    if (userProfile.nickname.isEmpty()) {
        UserRegistrationScreen(
            onRegistrationComplete = {
                // The viewmodel update will trigger recomposition and show the main app
            }
        )
        return
    }

    val navController = rememberNavController()
    val screens = listOf(Screen.Home, Screen.Profile) // Removed Garmin Screen

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.Black,
                contentColor = Color.Gray
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                screens.forEach { screen ->
                    val selected = currentRoute == screen.route
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.label,
                                modifier = Modifier.size(30.dp),
                                tint = if (selected) NeonGreen else Color.Gray
                            )
                        },
                        label = { 
                           // Optional: Hide label if you want pure icon look like screenshot
                           // Text(screen.label) 
                        },
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonGreen,
                            unselectedIconColor = Color.Gray,
                            indicatorColor = Color.Transparent // Remove the pill background
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeOverviewScreen(
                    onNavigateToMonthlyReport = { yearMonth ->
                        Log.d("MainScreen", "Navigating to monthly_report/$yearMonth")
                        navController.navigate("monthly_report/$yearMonth")
                    },
                    onNavigateToActivityDetail = { activityId ->
                        navController.navigate("activity_detail/$activityId")
                    }
                )
            }
            composable(Screen.Profile.route) {
                ProfileScreen()
            }
            
            composable(
                route = "activity_detail/{activityId}",
                arguments = listOf(navArgument("activityId") { type = NavType.StringType })
            ) { backStackEntry ->
                val activityId = backStackEntry.arguments?.getString("activityId") ?: return@composable
                ActivityDetailScreen(
                    activityId = activityId,
                    onBackClick = { navController.popBackStack() }
                )
            }

            // Garmin route removed
            composable(
                route = "monthly_report/{yearMonth}",
                arguments = listOf(navArgument("yearMonth") { type = NavType.StringType })
            ) { backStackEntry ->
                val yearMonthStr = backStackEntry.arguments?.getString("yearMonth")
                Log.d("MainScreen", "Received navigation arg yearMonth: $yearMonthStr")
                
                val yearMonth = try {
                    if (yearMonthStr != null) YearMonth.parse(yearMonthStr) else YearMonth.now()
                } catch (e: Exception) {
                    Log.e("MainScreen", "Error parsing YearMonth: $yearMonthStr", e)
                    YearMonth.now()
                }
                
                MonthlyReportScreen(
                    yearMonth = yearMonth,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
