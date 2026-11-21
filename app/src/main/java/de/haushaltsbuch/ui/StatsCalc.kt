package de.haushaltsbuch.ui

import de.haushaltsbuch.data.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

enum class Period(val label: String) { WEEK("Woche"), MONTH("Monat"), YEAR("Jahr") }

data class MonthSum(val month: YearMonth, val income: Long, val expense: Long)
data class CategoryShare(val category: Category, val cents: Long)
data class BudgetProgress(val category: Category, val limit: Long, val spent: Long) {
    val ratio: Float get() = if (limit == 0L) 0f else spent.toFloat() / limit
}

fun rangeOf(p: Period, today: LocalDate): Pair<LocalDate, LocalDate> = when (p) {
    Period.WEEK -> today.with(DayOfWeek.MONDAY) to today.with(DayOfWeek.SUNDAY)
    Period.MONTH -> today.withDayOfMonth(1) to today.with(TemporalAdjusters.lastDayOfMonth())
    Period.YEAR -> today.withDayOfYear(1) to today.with(TemporalAdjusters.lastDayOfYear())
}

fun LocalDate.within(r: Pair<LocalDate, LocalDate>) = !isBefore(r.first) && !isAfter(r.second)

/** Summe in Basiswährung. Umbuchungen (TRANSFER) zählen nie mit. */
fun List<Booking>.sumOfType(t: TxType, fx: Fx = Fx()) = filter { it.type == t }.sumOf { fx.toBase(it) }

fun expenseShares(b: List<Booking>, cats: Map<Long, Category>, r: Pair<LocalDate, LocalDate>, fx: Fx = Fx()) =
    b.filter { it.type == TxType.EXPENSE && it.date.within(r) }
        .groupBy { it.categoryId }
        .mapNotNull { (id, l) -> cats[id]?.let { CategoryShare(it, l.sumOf { x -> fx.toBase(x) }) } }
        .sortedByDescending { it.cents }

fun monthlySums(b: List<Booking>, today: LocalDate, fx: Fx = Fx(), n: Int = 6): List<MonthSum> =
    (n - 1 downTo 0).map { back ->
        val ym = YearMonth.from(today).minusMonths(back.toLong())
        val l = b.filter { YearMonth.from(it.date) == ym }
        MonthSum(ym, l.sumOfType(TxType.INCOME, fx), l.sumOfType(TxType.EXPENSE, fx))
    }

private fun Booking.net(fx: Fx) = when (type) {
    TxType.INCOME -> fx.toBase(this)
    TxType.EXPENSE -> -fx.toBase(this)
    TxType.TRANSFER -> 0L
}

/** Gesamtsaldo am Ende jedes Tages des laufenden Monats (Basiswährung). */
fun balanceSeries(b: List<Booking>, startTotal: Long, today: LocalDate, fx: Fx = Fx()): List<Long> {
    val first = today.withDayOfMonth(1)
    var run = startTotal + b.filter { it.date.isBefore(first) }.sumOf { it.net(fx) }
    return (1..today.dayOfMonth).map { day ->
        run += b.filter { it.date == first.withDayOfMonth(day) }.sumOf { it.net(fx) }
        run
    }
}
