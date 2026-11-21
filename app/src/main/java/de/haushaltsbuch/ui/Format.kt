package de.haushaltsbuch.ui

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

/** Nur Währungen mit 2 Nachkommastellen, weil Beträge als Cent gespeichert werden. */
val CURRENCIES = listOf("EUR", "USD", "GBP", "CHF", "PLN", "SEK", "NOK", "DKK", "CZK", "CAD", "AUD")

private val formats = HashMap<String, NumberFormat>()
private val dayFmt = DateTimeFormatter.ofPattern("EEE, d. MMM", Locale.GERMAN)

fun Long.money(cur: String = "EUR"): String =
    formats.getOrPut(cur) {
        NumberFormat.getCurrencyInstance(Locale.GERMANY).apply { currency = Currency.getInstance(cur) }
    }.format(this / 100.0)

fun LocalDate.label(today: LocalDate = LocalDate.now()): String = when (this) {
    today -> "Heute"
    today.minusDays(1) -> "Gestern"
    else -> format(dayFmt)
}

/** "12,5" oder "12.5" -> 1250 */
fun parseCents(s: String): Long {
    val t = s.trim().replace('.', ',')
    val euros = t.substringBefore(',').toLongOrNull() ?: 0L
    val cents = t.substringAfter(',', "").padEnd(2, '0').take(2).toLongOrNull() ?: 0L
    return euros * 100 + cents
}

/** Cent -> Eingabetext: 1250 -> "12,5", 1200 -> "12", 1205 -> "12,05", -5050 -> "-50,5" */
fun centsToInput(c: Long): String {
    val a = kotlin.math.abs(c)
    val frac = (a % 100).toString().padStart(2, '0').trimEnd('0')
    val s = if (frac.isEmpty()) "${a / 100}" else "${a / 100},$frac"
    return if (c < 0) "-$s" else s
}

/** Tastatureingabe für den Zahlenblock: max. 2 Nachkommastellen. */
fun amountKey(cur: String, k: String): String = when {
    k == "⌫" -> cur.dropLast(1)
    k == "," -> if (',' in cur) cur else if (cur.isEmpty()) "0," else "$cur,"
    cur.length >= 9 -> cur
    ',' in cur && cur.substringAfter(',').length >= 2 -> cur
    else -> cur + k
}

/** Wie parseCents, aber mit optionalem Minus (für Startsalden). */
fun parseSignedCents(s: String): Long {
    val t = s.trim()
    return if (t.startsWith("-")) -parseCents(t.drop(1)) else parseCents(t)
}

fun symbolOf(cur: String): String = Currency.getInstance(cur).getSymbol(Locale.GERMANY)
