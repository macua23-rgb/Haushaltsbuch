package de.haushaltsbuch.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class TxType { INCOME, EXPENSE, TRANSFER }
enum class AccountType { CHECKING, CASH, CREDIT_CARD, SAVINGS }

enum class Interval(val label: String) {
    WEEKLY("Wöchentlich"), MONTHLY("Monatlich"), YEARLY("Jährlich");

    /** [anchorDay] hält den Monatstag fest: 31. Jan -> 28. Feb -> 31. Mär (statt dauerhaft 28.). */
    fun next(d: LocalDate, anchorDay: Int = d.dayOfMonth): LocalDate = when (this) {
        WEEKLY -> d.plusWeeks(1)
        MONTHLY -> d.plusMonths(1).let { it.withDayOfMonth(minOf(anchorDay, it.lengthOfMonth())) }
        YEARLY -> d.plusYears(1).let { it.withDayOfMonth(minOf(anchorDay, it.lengthOfMonth())) }
    }
}

/** Beträge immer in Cent (Long), nie als Double. */
@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: AccountType,
    val icon: String,
    val startBalanceCents: Long = 0,
    val currency: String = "EUR",
    val settlementDay: Int? = null,          // nur Kreditkarte: Abrechnungstag
    val settlementAccountId: Long? = null    // nur Kreditkarte: Konto, von dem abgebucht wird
)

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String,
    val colorArgb: Long,
    val type: TxType,                        // INCOME oder EXPENSE
    val parentId: Long? = null               // Unterkategorien
)

/**
 * Eine Buchung. Bei TRANSFER ist accountId das Quell- und toAccountId das Zielkonto
 * (z. B. Girokonto -> Kreditkarte). Umbuchungen zählen nicht als Einnahme/Ausgabe.
 */
@Entity(tableName = "bookings", indices = [Index("date"), Index("accountId"), Index("categoryId")])
data class Booking(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TxType,
    val amountCents: Long,                   // immer positiv
    val date: LocalDate,
    val accountId: Long,
    val toAccountId: Long? = null,
    val toAmountCents: Long? = null,         // nur Umbuchung zwischen Währungen: Betrag im Zielkonto
    val categoryId: Long? = null,
    val note: String = "",
    val tags: String = "",                   // kommagetrennt
    val receiptUri: String? = null,
    val recurringId: Long? = null
)

@Entity(tableName = "recurring_rules")
data class RecurringRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TxType,
    val amountCents: Long,
    val accountId: Long,
    val categoryId: Long?,
    val note: String,
    val interval: Interval,
    val nextDue: LocalDate,
    val active: Boolean = true,
    val anchorDay: Int = 0                   // 0 = Tag von nextDue
)

@Entity(tableName = "budgets")
data class Budget(@PrimaryKey val categoryId: Long, val limitCents: Long)

@Entity(tableName = "goals")
data class SavingsGoal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String,
    val targetCents: Long,
    val savedCents: Long
)

/** 1 Einheit [currency] = [toBase] Einheiten der Basiswährung (manuell pflegbar). */
@Entity(tableName = "rates")
data class Rate(@PrimaryKey val currency: String, val toBase: Double)

data class AccountWithBalance(@Embedded val account: Account, val balanceCents: Long)
