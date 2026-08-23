package com.randallengineering.finances.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.randallengineering.finances.ui.components.AiChatbotOverlay
import com.randallengineering.finances.ui.screens.ai.AiAdvisorScreen
import com.randallengineering.finances.ui.screens.budgets.BudgetsScreen
import com.randallengineering.finances.ui.screens.goals.GoalsAndWantsScreen
import com.randallengineering.finances.ui.screens.insights.InsightsScreen
import com.randallengineering.finances.ui.screens.onboarding.SimpleFinOnboardingScreen
import com.randallengineering.finances.ui.screens.settings.SettingsScreen
import com.randallengineering.finances.ui.screens.transactions.TransactionDetailScreen
import com.randallengineering.finances.ui.screens.transactions.TransactionListScreen

data class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
)

val BottomNavItems = listOf(
    BottomNavItem(Screen.Transactions.route, "Transactions", Icons.AutoMirrored.Filled.ReceiptLong),
    BottomNavItem(Screen.Budgets.route, "Budgets", Icons.Default.PieChart),
    BottomNavItem(Screen.Insights.route, "Insights", Icons.Default.BarChart),
    BottomNavItem(Screen.Goals.route, "Goals", Icons.Default.Savings),
    BottomNavItem(Screen.AiAdvisor.route, "AI Advisor", Icons.Default.AutoAwesome)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceNavHost(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val shouldShowBottomBar = BottomNavItems.any { it.route == currentRoute }
    val currentTitle = when (currentRoute) {
        Screen.Transactions.route -> "Transactions"
        Screen.Budgets.route -> "Budgets & Categories"
        Screen.Insights.route -> "Insights & Analytics"
        Screen.Goals.route -> "Savings & Goals"
        Screen.AiAdvisor.route -> "AI Advisor (Gemini)"
        else -> "Randall Finances"
    }

    Scaffold(
        topBar = {
            if (shouldShowBottomBar) {
                TopAppBar(
                    title = {
                        Text(
                            text = currentTitle,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    actions = {
                        IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Settings & SimpleFIN Sync",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(visible = shouldShowBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    BottomNavItems.forEach { item ->
                        val isSelected = currentRoute == item.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Transactions.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Onboarding.route) {
                SimpleFinOnboardingScreen(
                    onNavigateToTransactions = {
                        navController.navigate(Screen.Transactions.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Transactions.route) {
                TransactionListScreen(
                    onNavigateToDetail = { id ->
                        navController.navigate(Screen.TransactionDetail.createRoute(id))
                    }
                )
            }

            composable(
                route = Screen.TransactionDetail.route,
                arguments = listOf(navArgument("transactionId") { type = NavType.StringType })
            ) { backStackEntry ->
                val txId = backStackEntry.arguments?.getString("transactionId") ?: ""
                TransactionDetailScreen(
                    transactionId = txId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Budgets.route) {
                BudgetsScreen()
            }

            composable(Screen.Insights.route) {
                InsightsScreen()
            }

            composable(Screen.Goals.route) {
                GoalsAndWantsScreen()
            }

            composable(Screen.Rules.route) {
                BudgetsScreen() // Merged into BudgetsScreen
            }

            composable(Screen.AiAdvisor.route) {
                AiAdvisorScreen()
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        // Floating AI Copilot Assistant
        AiChatbotOverlay()
    }
}
