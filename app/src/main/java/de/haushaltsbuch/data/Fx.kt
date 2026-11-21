package de.haushaltsbuch.data

/** Rechnet Beträge aus Kontowährungen in die Basiswährung um. */
class Fx(
    val base: String = "EUR",
    private val rates: Map<String, Double> = emptyMap(),
    private val accountCurrency: Map<Long, String> = emptyMap()
) {
    fun rate(cur: String): Double = if (cur == base) 1.0 else rates[cur] ?: 1.0
    fun hasRate(cur: String): Boolean = cur == base || rates.containsKey(cur)
    fun toBase(cents: Long, cur: String): Long = Math.round(cents * rate(cur))
    fun currencyOf(accountId: Long): String = accountCurrency[accountId] ?: base
    fun toBase(b: Booking): Long = toBase(b.amountCents, currencyOf(b.accountId))
    /** Betrag von einer Währung in eine andere (über die Basiswährung). */
    fun convert(cents: Long, from: String, to: String): Long = Math.round(cents * rate(from) / rate(to))
}
