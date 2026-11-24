package de.haushaltsbuch.scan

import java.time.LocalDate

/** Ergebnis der Beleg-Erkennung. Alles ist optional: erkannt wird nur, was sicher genug aussieht. */
data class ParsedReceipt(val amountCents: Long?, val date: LocalDate?, val merchant: String?)

/** Liest Betrag, Datum und Händler aus dem Rohtext eines Kassenbons (reine Logik, ohne Android). */
object ReceiptParser {
    private val amountRe = Regex("""(?<![\d.,])(\d{1,3}(?:\.\d{3})+|\d{1,5})[,.](\d{2})(?!\d)""")
    private val dateRe = Regex("""(?<!\d)(\d{1,2})[./](\d{1,2})[./](\d{4}|\d{2})(?!\d)""")

    private val strongTotal = listOf("zu zahlen", "zahlbetrag", "endbetrag", "gesamtbetrag", "gesamt", "summe", "total", "betrag")
    private val weakTotal = listOf("bar", "ec-karte", "ec karte", "girocard", "kartenzahlung", "visa", "mastercard", "karte")
    private val skipLine = listOf("zwischensumme", "rückgeld", "rueckgeld", "gegeben", "mwst", "ust", "netto", "steuer", "pfand")
    private val skipMerchant = listOf("kassenbon", "beleg", "rechnung", "tel", "www", "http", "str.", "straße", "strasse", "kasse", "datum", "uhr")

    fun parse(text: String, today: LocalDate = LocalDate.now()): ParsedReceipt {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        return ParsedReceipt(findTotal(lines), findDate(text, today), findMerchant(lines))
    }

    private fun amountsIn(line: String): List<Long> =
        amountRe.findAll(line).mapNotNull { m ->
            val euros = m.groupValues[1].replace(".", "").toLongOrNull() ?: return@mapNotNull null
            euros * 100 + m.groupValues[2].toLong()
        }.toList()

    private fun findTotal(lines: List<String>): Long? {
        fun byKeywords(keys: List<String>): Long? {
            for (i in lines.indices) {
                val l = lines[i].lowercase()
                if (skipLine.any { it in l } || keys.none { it in l }) continue
                // Betrag steht auf derselben Zeile, sonst auf der nächsten
                (amountsIn(lines[i]).lastOrNull() ?: lines.getOrNull(i + 1)?.let { amountsIn(it).lastOrNull() })
                    ?.takeIf { it > 0 }?.let { return it }
            }
            return null
        }
        return byKeywords(strongTotal) ?: byKeywords(weakTotal) ?: lines.flatMap { amountsIn(it) }.maxOrNull()?.takeIf { it > 0 }
    }

    private fun findDate(text: String, today: LocalDate): LocalDate? {
        for (m in dateRe.findAll(text)) {
            val d = m.groupValues[1].toInt(); val mo = m.groupValues[2].toInt()
            var y = m.groupValues[3].toInt()
            if (y < 100) y += 2000
            val date = runCatching { LocalDate.of(y, mo, d) }.getOrNull() ?: continue
            // Belege aus der Zukunft oder uralte Zahlen sind meist Lesefehler
            if (date.isAfter(today.plusDays(1)) || date.isBefore(today.minusYears(2))) continue
            return date
        }
        return null
    }

    private fun findMerchant(lines: List<String>): String? =
        lines.take(6).firstOrNull { l ->
            val low = l.lowercase()
            l.count { it.isLetter() } >= 3 &&
                l.count { it.isDigit() } <= l.length / 3 &&
                skipMerchant.none { it in low }
        }?.take(40)
}
