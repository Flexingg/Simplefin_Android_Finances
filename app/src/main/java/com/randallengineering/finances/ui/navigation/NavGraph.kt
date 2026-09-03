package com.randallengineering.finances.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.randallengineering.finances.ui.screens.dashboard.DashboardScreen
import com.randallengineering.finances.ui.screens.goals.GoalsAndWantsScreen
import com.randallengineering.finances.ui.screens.insights.InsightsScreen
import com.randallengineering.finances.ui.screens.onboarding.SimpleFinOnboardingScreen
import com.randallengineering.finances.ui.screens.queue.ActionQueueScreen
import com.randallengineering.finances.ui.screens.rules.RulesManagementScreen
import com.randallengineering.finances.ui.screens.settings.SettingsScreen
import com.randallengineering.finances.ui.screens.transactions.TransactionDetailScreen
import com.randallengineering.finances.ui.screens.transactions.TransactionListScreen

data class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
)

val BottomNavItems = listOf(
    BottomNavItem(Screen.Dashboard.route, "Home", Icons.Default.Home),
    BottomNavItem(Screen.Transactions.route, "History", Icons.AutoMirrored.Filled.ReceiptLong),
    BottomNavItem(Screen.Budgets.route, "Budgets", Icons.Default.PieChart),
    BottomNavItem(Screen.Insights.route, "Insights", Icons.Default.BarChart),
    BottomNavItem(Screen.ActionQueue.route, "Queue", Icons.Default.Style)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceNavHost(
    navController: NavHostController = rememberNavController(),
    deepLinkRoute: String? = null
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Navigate to the requested screen once, after the NavHost graph is ready
    // (e.g. the user tapped a notification that deep-links to a screen).
    var deepLinkHandled by remember { mutableStateOf(false) }
    LaunchedEffect(navController, deepLinkRoute) {
        if (deepLinkRoute != null && !deepLinkHandled) {
            try {
                navController.navigate(deepLinkRoute) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                }
                deepLinkHandled = true
            } catch (_: Exception) { }
        }
    }

    val shouldShowBottomBar = BottomNavItems.any { it.route == currentRoute }

    Scaffold(
        topBar = {
            if (shouldShowBottomBar && currentRoute != Screen.Budgets.route) {
                TopAppBar(
                    title = {
                        val currentTitle = when (currentRoute) {
                            Screen.Dashboard.route -> "Randall Finances"
                            Screen.Transactions.route -> "Transaction History"
                            Screen.Budgets.route -> "Budgets & Categories"
                            Screen.Insights.route -> "Insights & Analytics"
                            Screen.ActionQueue.route -> "Review Queue"
                            else -> "Randall Finances"
                        }
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
            if (shouldShowBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    BottomNavItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onNavigateToTransactions = { navController.navigate(Screen.Transactions.route) },
                    onNavigateToBudgets = { navController.navigate(Screen.Budgets.route) },
                    onNavigateToInsights = { navController.navigate(Screen.Insights.route) },
                    onNavigateToQueue = { navController.navigate(Screen.ActionQueue.route) },
                    onNavigateToAi = { navController.navigate(Screen.AiAdvisor.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }
            composable(Screen.ActionQueue.route) {
                ActionQueueScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Onboarding.route) {
                SimpleFinOnboardingScreen(
                    onNavigateToTransactions = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Transactions.route) {
                TransactionListScreen(
                    onNavigateToDetail = { txId ->
                        navController.navigate(Screen.TransactionDetail.createRoute(txId))
                    }
                )
            }
            composable(
                route = Screen.TransactionDetail.route,
                arguments = listOf(navArgument("transactionId") { type = NavType.StringType })
            ) { backStackEntry ->
                val txId = backStackEntry.arguments?.getString("transactionId").orEmpty()
                TransactionDetailScreen(
                    transactionId = txId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Budgets.route) {
                BudgetsScreen()
            }
            composable(Screen.Rules.route) {
                RulesManagementScreen()
            }
            composable(Screen.Insights.route) {
                InsightsScreen()
            }
            composable(Screen.Goals.route) {
                BudgetsScreen(initialTab = 1)
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
    }
}
