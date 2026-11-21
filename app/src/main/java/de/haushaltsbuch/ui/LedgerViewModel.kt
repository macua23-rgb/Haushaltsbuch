package de.haushaltsbuch.ui

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.haushaltsbuch.data.*
import de.haushaltsbuch.export.ExportRow
import de.haushaltsbuch.export.Exporter
import de.haushaltsbuch.widget.BalanceWidget
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class UiState(
    val accounts: List<AccountWithBalance> = emptyList(),
    val categories: Map<Long, Category> = emptyMap(),
    val bookings: List<Booking> = emptyList(),
    val totalBalance: Long = 0,
    val monthIncome: Long = 0,
    val monthExpense: Long = 0,
    val shares: List<CategoryShare> = emptyList(),
    val months: List<MonthSum> = emptyList(),
    val balanceSeries: List<Long> = emptyList(),
    val budgets: List<BudgetProgress> = emptyList(),
    val goals: List<SavingsGoal> = emptyList(),
    val recurring: List<RecurringRule> = emptyList(),
    val rates: List<Rate> = emptyList(),
    val settings: Settings = Settings(),
    val fx: Fx = Fx(),
    val period: Period = Period.MONTH
) {
    val base get() = settings.baseCurrency
    fun fmt(cents: Long) = cents.money(base)
    fun category(b: Booking) = b.categoryId?.let { categories[it] }
    fun account(id: Long?) = accounts.firstOrNull { it.account.id == id }?.account
    fun currencyOf(id: Long?) = account(id)?.currency ?: base
}

private class Core(
    val accounts: List<AccountWithBalance>, val cats: List<Category>, val bookings: List<Booking>,
    val budgets: List<Budget>, val goals: List<SavingsGoal>
)

private class Extra(val rates: List<Rate>, val recurring: List<RecurringRule>, val settings: Settings)

@HiltViewModel
class LedgerViewModel @Inject constructor(
    private val repo: LedgerRepository,
    val receipts: ReceiptStore,
    @ApplicationContext private val appCtx: Context
) : ViewModel() {
    private val period = MutableStateFlow(Period.MONTH)

    val ui: StateFlow<UiState> = combine(
        combine(repo.accounts, repo.categories, repo.bookings, repo.budgets, repo.goals, ::Core),
        combine(repo.rates, repo.recurring, repo.settingsFlow, ::Extra),
        period
    ) { c, e, p -> build(c, e, p) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    fun setPeriod(p: Period) { period.value = p }

    /** Führt eine Änderung aus und aktualisiert danach das Widget. */
    private fun run(block: suspend () -> Unit) = viewModelScope.launch {
        block()
        BalanceWidget().updateAll(appCtx)
    }

    fun add(b: Booking) = run { repo.add(b) }
    fun update(old: Booking, new: Booking) = run { repo.update(old, new) }
    fun delete(b: Booking) = run { repo.delete(b) }
    fun settle(card: Account) = run { repo.settleCard(card) }

    fun saveAccount(a: Account) = run { repo.saveAccount(a) }
    fun deleteAccount(a: Account, done: (Boolean) -> Unit) = run { done(repo.deleteAccount(a)) }
    fun saveCategory(c: Category) = run { repo.saveCategory(c) }
    fun deleteCategory(c: Category) = run { repo.deleteCategory(c) }
    fun saveBudget(b: Budget) = run { repo.saveBudget(b) }
    fun deleteBudget(categoryId: Long) = run { repo.deleteBudget(categoryId) }
    fun saveGoal(g: SavingsGoal) = run { repo.saveGoal(g) }
    fun deleteGoal(g: SavingsGoal) = run { repo.deleteGoal(g) }
    fun saveRule(r: RecurringRule) = run { repo.saveRule(r) }
    fun deleteRule(r: RecurringRule) = run { repo.deleteRule(r) }
    fun saveRate(r: Rate) = run { repo.saveRate(r) }
    fun changeBase(cur: String) = run { repo.changeBase(cur) }
    fun setAppLock(v: Boolean) = run { repo.settings.setAppLock(v) }
    fun setBudgetAlerts(v: Boolean) = run { repo.settings.setBudgetAlerts(v) }

    /** Zeilen für CSV/PDF; null = alle Buchungen. */
    fun exportRows(range: Pair<LocalDate, LocalDate>?): List<ExportRow> {
        val s = ui.value
        val list = if (range == null) s.bookings else s.bookings.filter { it.date.within(range) }
        return Exporter.rows(list, s.categories, s.accounts.associate { it.account.id to it.account }, s.fx)
    }

    private fun build(d: Core, e: Extra, p: Period): UiState {
        val today = LocalDate.now()
        val cats = d.cats.associateBy { it.id }
        val fx = Fx(e.settings.baseCurrency, e.rates.associate { it.currency to it.toBase }, d.accounts.associate { it.account.id to it.account.currency })
        val month = rangeOf(Period.MONTH, today)
        val monthB = d.bookings.filter { it.date.within(month) }
        val spendable = d.accounts.filter { it.account.type != AccountType.SAVINGS }
        return UiState(
            accounts = d.accounts,
            categories = cats,
            bookings = d.bookings,
            totalBalance = spendable.sumOf { fx.toBase(it.balanceCents, it.account.currency) },
            monthIncome = monthB.sumOfType(TxType.INCOME, fx),
            monthExpense = monthB.sumOfType(TxType.EXPENSE, fx),
            shares = expenseShares(d.bookings, cats, rangeOf(p, today), fx),
            months = monthlySums(d.bookings, today, fx),
            balanceSeries = balanceSeries(
                d.bookings, spendable.sumOf { fx.toBase(it.account.startBalanceCents, it.account.currency) }, today, fx
            ),
            budgets = d.budgets.mapNotNull { b ->
                cats[b.categoryId]?.let { c ->
                    BudgetProgress(c, b.limitCents,
                        monthB.filter { it.type == TxType.EXPENSE && it.categoryId == c.id }.sumOf { fx.toBase(it) })
                }
            },
            goals = d.goals,
            recurring = e.recurring,
            rates = e.rates,
            settings = e.settings,
            fx = fx,
            period = p
        )
    }
}
