package de.haushaltsbuch.data

import androidx.room.withTransaction
import de.haushaltsbuch.notify.BudgetNotifier
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LedgerRepository @Inject constructor(
    private val db: AppDatabase,
    private val notifier: BudgetNotifier,
    private val receipts: ReceiptStore,
    val settings: SettingsRepository
) {
    private val acc = db.accounts()
    private val cat = db.categories()
    private val bk = db.bookings()
    private val rec = db.recurring()
    private val bud = db.budgets()
    private val goal = db.goals()
    private val fx = db.fx()

    val accounts = acc.observeWithBalance()
    val categories = cat.observeAll()
    val bookings = bk.observeAll()
    val budgets = bud.observeAll()
    val goals = goal.observeAll()
    val rates = fx.observeAll()
    val recurring = rec.observeAll()
    val settingsFlow = settings.flow

    // ---- Buchungen ----
    suspend fun add(b: Booking) { bk.insert(b); notifier.onBooking(b) }

    suspend fun update(old: Booking, new: Booking) {
        bk.update(new)
        if (old.receiptUri != null && old.receiptUri != new.receiptUri) receipts.delete(old.receiptUri)
        notifier.onBooking(new)
    }

    suspend fun delete(b: Booking) { bk.delete(b); b.receiptUri?.let(receipts::delete) }

    // ---- Konten ----
    suspend fun saveAccount(a: Account) { if (a.id == 0L) acc.insert(a) else acc.update(a) }

    /** Löschen nur, wenn das Konto nirgends verwendet wird; sonst false. */
    suspend fun deleteAccount(a: Account): Boolean {
        if (bk.countForAccount(a.id) > 0 || rec.countForAccount(a.id) > 0) return false
        acc.clearSettlementRef(a.id)
        acc.delete(a)
        return true
    }

    // ---- Kategorien, Budgets, Sparziele ----
    suspend fun saveCategory(c: Category) { if (c.id == 0L) cat.insert(c) else cat.update(c) }

    suspend fun deleteCategory(c: Category) = db.withTransaction {
        bk.clearCategory(c.id); rec.clearCategory(c.id); bud.deleteFor(c.id); cat.delete(c)
    }

    suspend fun saveBudget(b: Budget) = bud.upsert(b)
    suspend fun deleteBudget(categoryId: Long) = bud.deleteFor(categoryId)
    suspend fun saveGoal(g: SavingsGoal) = goal.upsert(g)
    suspend fun deleteGoal(g: SavingsGoal) = goal.delete(g)

    // ---- Wiederkehrende Buchungen ----
    suspend fun saveRule(r: RecurringRule) = rec.upsert(r)
    suspend fun deleteRule(r: RecurringRule) = rec.delete(r)

    /** Legt fällige wiederkehrende Buchungen an (auch verpasste Termine). */
    suspend fun applyRecurring(today: LocalDate) {
        rec.due(today).forEach { r ->
            var d = r.nextDue
            while (!d.isAfter(today)) {
                val b = Booking(
                    type = r.type, amountCents = r.amountCents, date = d,
                    accountId = r.accountId, categoryId = r.categoryId,
                    note = r.note, recurringId = r.id
                )
                bk.insert(b)
                notifier.onBooking(b)
                d = r.interval.next(d, r.anchorDay.takeIf { it > 0 } ?: d.dayOfMonth)
            }
            rec.upsert(r.copy(nextDue = d))
        }
    }

    // ---- Kreditkarte ----
    /** Bucht den offenen Betrag der Kreditkarte vom Abrechnungskonto ab (Umbuchung, ggf. mit Umrechnung). */
    suspend fun settleCard(card: Account, date: LocalDate = LocalDate.now()): Boolean {
        val fromId = card.settlementAccountId ?: return false
        val debt = -acc.balanceOf(card.id)
        if (debt <= 0) return false
        val from = acc.all().firstOrNull { it.id == fromId } ?: return false
        var amount = debt
        var toAmount: Long? = null
        if (from.currency != card.currency) {            // Betrag in Währung des Abrechnungskontos
            val s = settings.flow.first()
            val f = Fx(s.baseCurrency, fx.all().associate { it.currency to it.toBase })
            amount = f.convert(debt, card.currency, from.currency)
            toAmount = debt
        }
        bk.insert(
            Booking(
                type = TxType.TRANSFER, amountCents = amount, toAmountCents = toAmount, date = date,
                accountId = fromId, toAccountId = card.id, note = "Kreditkartenabrechnung"
            )
        )
        return true
    }

    suspend fun runDailyJobs(today: LocalDate) {
        applyRecurring(today)
        acc.all().filter { it.type == AccountType.CREDIT_CARD && it.settlementDay != null }
            .filter { minOf(it.settlementDay!!, today.lengthOfMonth()) == today.dayOfMonth }
            .forEach { settleCard(it, today) }
    }

    // ---- Währungen ----
    suspend fun saveRate(r: Rate) = fx.upsert(r)

    /** Wechselt die Basiswährung und rechnet die gespeicherten Kurse passend um. */
    suspend fun changeBase(new: String) {
        val old = settings.flow.first().baseCurrency
        if (old == new) return
        db.withTransaction {
            val m = fx.all().associate { it.currency to it.toBase }.toMutableMap()
            m[old] = 1.0
            val f = m[new] ?: 1.0
            m.remove(new); fx.delete(new)
            m.forEach { (c, r) -> fx.upsert(Rate(c, r / f)) }
        }
        settings.setBaseCurrency(new)
    }

    // ---- Beispieldaten ----
    suspend fun seedIfEmpty() {
        db.withTransaction {
            if (fx.count() == 0) {  // Beispielkurse, bitte in der App anpassen
                listOf("USD" to 0.92, "GBP" to 1.17, "CHF" to 1.06).forEach { fx.upsert(Rate(it.first, it.second)) }
            }
            if (acc.count() > 0) return@withTransaction
            val giro = acc.insert(Account(name = "Girokonto", type = AccountType.CHECKING, icon = "🏦", startBalanceCents = 215_000))
            acc.insert(Account(name = "Bargeld", type = AccountType.CASH, icon = "👛", startBalanceCents = 8_500))
            acc.insert(Account(name = "Kreditkarte", type = AccountType.CREDIT_CARD, icon = "💳", settlementDay = 28, settlementAccountId = giro))
            acc.insert(Account(name = "Sparkonto", type = AccountType.SAVINGS, icon = "🐷", startBalanceCents = 470_000))

            fun exp(n: String, i: String, c: Long) = Category(name = n, icon = i, colorArgb = c, type = TxType.EXPENSE)
            fun inc(n: String, i: String) = Category(name = n, icon = i, colorArgb = 0xFF14804F, type = TxType.INCOME)
            val ids = cat.insertAll(
                listOf(
                    exp("Wohnen", "🏠", 0xFF4F7CAC), exp("Lebensmittel", "🛒", 0xFFE0A030),
                    exp("Mobilität", "🚗", 0xFF8E6BBF), exp("Freizeit", "🎬", 0xFFD4472F),
                    exp("Abos", "📺", 0xFF2A9D8F), exp("Gesundheit", "💊", 0xFFE07A9A),
                    inc("Gehalt", "💼"), inc("Kindergeld", "👶"), inc("Nebenjob", "🧾")
                )
            )
            listOf(90_000L, 30_000L, 15_000L, 10_000L, 4_000L, 6_000L).forEachIndexed { i, limit ->
                bud.upsert(Budget(ids[i], limit))
            }
            goal.upsert(SavingsGoal(name = "Urlaub", icon = "🏖️", targetCents = 200_000, savedCents = 120_000))
            goal.upsert(SavingsGoal(name = "Notgroschen", icon = "🛟", targetCents = 600_000, savedCents = 350_000))
            val today = LocalDate.now()
            rec.upsert(RecurringRule(type = TxType.EXPENSE, amountCents = 85_000, accountId = giro, categoryId = ids[0],
                note = "Miete", interval = Interval.MONTHLY, nextDue = today.withDayOfMonth(1).plusMonths(1)))
            rec.upsert(RecurringRule(type = TxType.INCOME, amountCents = 25_900, accountId = giro, categoryId = ids[7],
                note = "Kindergeld", interval = Interval.MONTHLY, nextDue = today.withDayOfMonth(5).plusMonths(1)))
        }
    }
}
