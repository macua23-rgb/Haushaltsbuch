package de.haushaltsbuch.ui

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import de.haushaltsbuch.data.Booking
import de.haushaltsbuch.data.TxType
import de.haushaltsbuch.ui.screens.*

enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    HOME("home", "Übersicht", Icons.Outlined.Home),
    TX("tx", "Buchungen", Icons.Outlined.Receipt),
    STATS("stats", "Statistik", Icons.Outlined.PieChart),
    BUDGETS("budgets", "Budgets", Icons.Outlined.Flag),
    MORE("more", "Mehr", Icons.Outlined.MoreHoriz)
}

private val SUB_ROUTES = setOf("accounts", "categories", "recurring", "settings")

private data class SheetState(val initial: Booking?, val type: TxType)

@Composable
fun AppRoot(
    vm: LedgerViewModel = hiltViewModel(),
    initialType: TxType? = null,
    onInitialConsumed: () -> Unit = {}
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    var sheet by remember { mutableStateOf<SheetState?>(null) }
    val ready = ui.accounts.isNotEmpty()

    // Widget-Schnelleingabe: erst öffnen, wenn die Konten geladen sind
    LaunchedEffect(initialType, ready) {
        if (initialType != null && ready) { sheet = SheetState(null, initialType); onInitialConsumed() }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = route == t.route || (t == Tab.MORE && route in SUB_ROUTES),
                        onClick = {
                            nav.navigate(t.route) {
                                popUpTo(nav.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(t.icon, null) },
                        label = { Text(t.label) }
                    )
                }
            }
        },
        floatingActionButton = {
            if (ready) FloatingActionButton(onClick = { sheet = SheetState(null, TxType.EXPENSE) }) {
                Icon(Icons.Default.Add, "Buchung erfassen")
            }
        }
    ) { pad ->
        val back: () -> Unit = { nav.popBackStack() }
        NavHost(nav, Tab.HOME.route, Modifier.padding(pad).consumeWindowInsets(pad)) {
            composable(Tab.HOME.route) { HomeScreen(ui) { sheet = SheetState(it, it.type) } }
            composable(Tab.TX.route) { TransactionsScreen(ui, { sheet = SheetState(it, it.type) }, vm::delete) }
            composable(Tab.STATS.route) { StatsScreen(ui, vm::setPeriod) }
            composable(Tab.BUDGETS.route) {
                BudgetsScreen(ui, vm::saveBudget, vm::deleteBudget, vm::saveGoal, vm::deleteGoal)
            }
            composable(Tab.MORE.route) { MoreScreen(ui, vm::settle) { nav.navigate(it) } }
            composable("accounts") { AccountsScreen(ui, vm::saveAccount, vm::deleteAccount, back) }
            composable("categories") { CategoriesScreen(ui, vm::saveCategory, vm::deleteCategory, back) }
            composable("recurring") { RecurringScreen(ui, vm::saveRule, vm::deleteRule, back) }
            composable("settings") { SettingsScreen(ui, vm, back) }
        }
    }

    sheet?.let { s ->
        BookingSheet(
            ui = ui, initial = s.initial, startType = s.type, receipts = vm.receipts,
            onDismiss = { sheet = null },
            onSave = { b -> if (s.initial == null) vm.add(b) else vm.update(s.initial, b); sheet = null },
            onDelete = s.initial?.let { b -> { vm.delete(b); sheet = null } },
            onCreateCategory = vm::saveCategory
        )
    }
}
