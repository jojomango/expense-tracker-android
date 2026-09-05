package com.jojomango.expensetracker.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jojomango.expensetracker.ui.category.CategoryEditScreen
import com.jojomango.expensetracker.ui.category.CategoryManagementScreen
import com.jojomango.expensetracker.ui.home.HomeScreen
import com.jojomango.expensetracker.ui.home.HomeViewModel
import com.jojomango.expensetracker.ui.navigation.Routes
import com.jojomango.expensetracker.ui.navigation.isBottomNavHiddenRoute
import com.jojomango.expensetracker.ui.settings.SettingsScreen
import com.jojomango.expensetracker.ui.stats.StatsScreen
import com.jojomango.expensetracker.ui.theme.ExpenseTrackerTheme
import com.jojomango.expensetracker.ui.transaction.AddEditTransactionScreen
import com.jojomango.expensetracker.ui.wallet.WalletEditScreen
import com.jojomango.expensetracker.ui.wallet.WalletManagementScreen
import com.jojomango.expensetracker.ui.wallet.WalletSwitcherSheet
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ExpenseTrackerTheme {
                ExpenseTrackerApp()
            }
        }
    }
}

@Composable
private fun ExpenseTrackerApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    var showWalletSwitcher by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 首頁的 ViewModel 在這一層取得，讓底部導覽的 FAB／錢包切換 sheet
    // 能拿到目前的錢包清單，跟 HomeScreen 共用同一個 HiltViewModel 實例。
    val homeViewModel: HomeViewModel = hiltViewModel()
    val homeState by homeViewModel.uiState.collectAsState()
    val walletBalanceTexts by homeViewModel.walletBalanceTexts.collectAsState()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (!isBottomNavHiddenRoute(currentRoute)) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    NavigationBar {
                        NavigationBarItem(
                            selected = currentRoute == Routes.HOME,
                            onClick = { navController.navigateSingleTopTo(Routes.HOME) },
                            icon = { Icon(Icons.Filled.Home, contentDescription = "首頁") },
                            label = { Text("首頁") },
                        )
                        // 中間留空給疊在上面的中央 FAB（UI-SPEC.md §3.1）。
                        NavigationBarItem(
                            selected = false,
                            onClick = {},
                            enabled = false,
                            icon = {},
                            label = {},
                        )
                        NavigationBarItem(
                            selected = currentRoute == Routes.STATS,
                            onClick = { navController.navigateSingleTopTo(Routes.STATS) },
                            icon = { Icon(Icons.Filled.BarChart, contentDescription = "統計") },
                            label = { Text("統計") },
                        )
                    }
                    FloatingActionButton(
                        onClick = { navController.navigate(Routes.ADD_TRANSACTION) },
                        modifier =
                            Modifier
                                .align(Alignment.TopCenter)
                                .offset(y = (-24).dp)
                                .size(58.dp),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "記帳")
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier =
                Modifier.padding(
                    bottom = if (isBottomNavHiddenRoute(currentRoute)) 0.dp else padding.calculateBottomPadding(),
                ),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onAddTransaction = { navController.navigate(Routes.ADD_TRANSACTION) },
                    onEditTransaction = { id -> navController.navigate(Routes.editTransaction(id)) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onSwitchWalletRequested = { showWalletSwitcher = true },
                    // 一定要把上面已經拿到的 homeViewModel 傳進去，不能讓 HomeScreen
                    // 用自己預設參數的 hiltViewModel() 另外生一個實例——那個實例的
                    // scope 是 "home" 這個 NavBackStackEntry，跟這裡（ExpenseTrackerApp
                    // 這層、scope 是整個 Activity）拿到的不是同一個物件，各自的
                    // selectedWalletId 互不相通，切換錢包會完全沒反應（真的踩過這個
                    // bug，見 TASKS.md Phase 5 交接筆記）。
                    viewModel = homeViewModel,
                )
            }
            composable(Routes.STATS) { StatsScreen() }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onManageWallets = { navController.navigate(Routes.WALLET_MANAGEMENT) },
                    onManageCategories = { navController.navigate(Routes.CATEGORY_MANAGEMENT) },
                )
            }
            composable(Routes.ADD_TRANSACTION) {
                AddEditTransactionScreen(
                    onDone = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() },
                )
            }
            composable(
                Routes.EDIT_TRANSACTION_PATTERN,
                arguments = listOf(navArgument("transactionId") { type = NavType.StringType }),
            ) {
                AddEditTransactionScreen(
                    onDone = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() },
                )
            }
            composable(Routes.WALLET_MANAGEMENT) {
                WalletManagementScreen(
                    onBack = { navController.popBackStack() },
                    onAddWallet = { navController.navigate(Routes.WALLET_NEW) },
                    onEditWallet = { id -> navController.navigate(Routes.editWallet(id)) },
                )
            }
            composable(Routes.WALLET_NEW) {
                WalletEditScreen(
                    onDone = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() },
                )
            }
            composable(
                Routes.WALLET_EDIT_PATTERN,
                arguments = listOf(navArgument("walletId") { type = NavType.StringType }),
            ) {
                WalletEditScreen(
                    onDone = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() },
                )
            }
            composable(Routes.CATEGORY_MANAGEMENT) {
                CategoryManagementScreen(
                    onBack = { navController.popBackStack() },
                    onAddCategory = { navController.navigate(Routes.CATEGORY_NEW) },
                    onEditCategory = { id -> navController.navigate(Routes.editCategory(id)) },
                )
            }
            composable(Routes.CATEGORY_NEW) {
                CategoryEditScreen(
                    onDone = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() },
                )
            }
            composable(
                Routes.CATEGORY_EDIT_PATTERN,
                arguments = listOf(navArgument("categoryId") { type = NavType.StringType }),
            ) {
                CategoryEditScreen(
                    onDone = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() },
                )
            }
        }
    }

    if (showWalletSwitcher) {
        WalletSwitcherSheet(
            wallets = homeState.wallets,
            currentWalletId = homeState.currentWallet?.id,
            walletBalanceTexts = walletBalanceTexts,
            onSelectWallet = { walletId ->
                homeViewModel.switchWallet(walletId)
                showWalletSwitcher = false
                // UI-SPEC.md §7：切換後用 Snackbar 顯示「已切換到 {名稱}」。
                val walletName = homeState.wallets.firstOrNull { it.id == walletId }?.name
                if (walletName != null) {
                    scope.launch { snackbarHostState.showSnackbar("已切換到 $walletName") }
                }
            },
            onManageWallets = {
                showWalletSwitcher = false
                navController.navigate(Routes.WALLET_MANAGEMENT)
            },
            onDismiss = { showWalletSwitcher = false },
        )
    }
}

private fun androidx.navigation.NavController.navigateSingleTopTo(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
