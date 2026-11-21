package de.haushaltsbuch

import de.haushaltsbuch.data.*
import de.haushaltsbuch.ui.*
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class StatsCalcTest {
    private val today = LocalDate.of(2026, 11, 21)   // Samstag
    private val food = Category(1, "Lebensmittel", "🛒", 0xFFE0A030, TxType.EXPENSE)
    private val rent = Category(2, "Wohnen", "🏠", 0xFF4F7CAC, TxType.EXPENSE)
    private val cats = listOf(food, rent).associateBy { it.id }

    private fun b(type: TxType, cents: Long, d: LocalDate, cat: Long? = null, acc: Long = 1, to: Long? = null) =
        Booking(type = type, amountCents = cents, date = d, accountId = acc, toAccountId = to, categoryId = cat)

    @Test fun transfersNeverCountAsIncomeOrExpense() {
        val l = listOf(
            b(TxType.INCOME, 100_000, today), b(TxType.EXPENSE, 30_000, today, 1),
            b(TxType.TRANSFER, 50_000, today, null, 1, 2)
        )
        assertEquals(100_000, l.sumOfType(TxType.INCOME))
        assertEquals(30_000, l.sumOfType(TxType.EXPENSE))
        assertEquals(0, l.sumOfType(TxType.EXPENSE) + l.sumOfType(TxType.INCOME) - 130_000)
    }

    @Test fun sumsConvertForeignCurrencyToBase() {
        val fx = Fx("EUR", mapOf("USD" to 0.5), mapOf(1L to "EUR", 2L to "USD"))
        val l = listOf(b(TxType.EXPENSE, 1000, today, 1, acc = 1), b(TxType.EXPENSE, 1000, today, 1, acc = 2))
        assertEquals(1500, l.sumOfType(TxType.EXPENSE, fx))
    }

    @Test fun sharesAreSortedAndFilteredByPeriod() {
        val l = listOf(
            b(TxType.EXPENSE, 4_000, today, 1), b(TxType.EXPENSE, 85_000, today.withDayOfMonth(3), 2),
            b(TxType.EXPENSE, 1_000, today.withDayOfMonth(1).minusDays(1), 1),   // Vormonat
            b(TxType.INCOME, 9_999, today, 1)
        )
        val month = expenseShares(l, cats, rangeOf(Period.MONTH, today))
        assertEquals(listOf(rent, food), month.map { it.category })
        assertEquals(listOf(85_000L, 4_000L), month.map { it.cents })
        val week = expenseShares(l, cats, rangeOf(Period.WEEK, today))
        assertEquals(listOf(food), week.map { it.category })
    }

    @Test fun weekRangeIsMondayToSunday() {
        val (from, to) = rangeOf(Period.WEEK, today)
        assertEquals(LocalDate.of(2026, 11, 16), from)
        assertEquals(LocalDate.of(2026, 11, 22), to)
    }

    @Test fun yearRange() {
        val (from, to) = rangeOf(Period.YEAR, today)
        assertEquals(LocalDate.of(2026, 1, 1), from); assertEquals(LocalDate.of(2026, 12, 31), to)
    }

    @Test fun monthlySumsCoverSixMonthsAcrossYearBoundary() {
        val jan = LocalDate.of(2026, 1, 15)
        val l = listOf(b(TxType.INCOME, 100, LocalDate.of(2025, 12, 5)), b(TxType.EXPENSE, 40, LocalDate.of(2026, 1, 3), 1))
        val m = monthlySums(l, jan)
        assertEquals(6, m.size)
        assertEquals(YearMonth.of(2025, 8), m.first().month)
        assertEquals(YearMonth.of(2026, 1), m.last().month)
        assertEquals(100, m[4].income); assertEquals(40, m[5].expense)
    }

    @Test fun balanceSeriesIgnoresTransfersAndCarriesOver() {
        val l = listOf(
            b(TxType.INCOME, 1_000, today.withDayOfMonth(1)),
            b(TxType.EXPENSE, 300, today.withDayOfMonth(3), 1),
            b(TxType.TRANSFER, 500, today.withDayOfMonth(3), null, 1, 2),
            b(TxType.EXPENSE, 50, today.withDayOfMonth(1).minusDays(2), 1)      // vor dem Monat
        )
        val s = balanceSeries(l, startTotal = 10_000, today = today)
        assertEquals(21, s.size)
        assertEquals(10_000 - 50 + 1_000, s[0])
        assertEquals(10_000 - 50 + 1_000, s[1])
        assertEquals(10_000 - 50 + 1_000 - 300, s[2])
        assertEquals(s[2], s.last())
    }

    @Test fun budgetRatio() {
        assertEquals(0.5f, BudgetProgress(food, 10_000, 5_000).ratio, 0.0001f)
        assertEquals(0f, BudgetProgress(food, 0, 5_000).ratio, 0.0001f)
    }
}
