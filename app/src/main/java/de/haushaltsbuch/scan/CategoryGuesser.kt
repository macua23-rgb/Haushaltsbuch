package de.haushaltsbuch.scan

import de.haushaltsbuch.data.Booking
import de.haushaltsbuch.data.Category
import de.haushaltsbuch.data.TxType

/** Rät die Kategorie eines Belegs: erst aus früheren Buchungen desselben Händlers, dann über Stichwörter. */
object CategoryGuesser {
    /** Stichwörter im Belegtext -> Teile von Kategorienamen, die dazu passen (alles klein geschrieben). */
    private val rules: List<Pair<List<String>, List<String>>> = listOf(
        listOf("rewe", "edeka", "aldi", "lidl", "penny", "netto", "kaufland", "denn's", "tegut", "bäckerei", "baeckerei", "metzger", "supermarkt", "markt")
            to listOf("lebensmittel", "einkauf", "essen"),
        listOf("aral", "shell", "esso", "jet ", "tankstelle", "total energies", "db ", "bahn", "parkhaus", "parken", "taxi", "bvg", "mvv", "fahrkarte")
            to listOf("mobil", "auto", "verkehr", "fahr"),
        listOf("apotheke", "arzt", "praxis", "zahnarzt", "optiker", "sanitätshaus")
            to listOf("gesund", "apotheke", "arzt"),
        listOf("kino", "theater", "konzert", "restaurant", "pizzeria", "café", "cafe", "bar ", "bowling", "schwimmbad", "eintritt")
            to listOf("freizeit", "unterhaltung", "restaurant", "gastro"),
        listOf("netflix", "spotify", "abo", "disney", "streaming")
            to listOf("abo")
    )

    fun guess(merchant: String?, fullText: String, history: List<Booking>, categories: Collection<Category>): Long? {
        val expense = categories.filter { it.type == TxType.EXPENSE }
        if (expense.isEmpty()) return null
        val ids = expense.map { it.id }.toSet()

        // 1. Gelernt: erstes brauchbares Wort des Händlers kommt in Notizen früherer Ausgaben vor
        val key = merchant?.lowercase()?.split(Regex("[^\\p{L}0-9]+"))?.firstOrNull { it.length >= 3 }
        if (key != null) {
            history.filter { it.type == TxType.EXPENSE && it.categoryId in ids && key in it.note.lowercase() }
                .groupingBy { it.categoryId }.eachCount().maxByOrNull { it.value }?.key
                ?.let { return it }
        }

        // 2. Stichwörter
        val hay = " ${(merchant.orEmpty() + " " + fullText).lowercase()} "
        for ((words, names) in rules) {
            if (words.none { it in hay }) continue
            expense.firstOrNull { c -> names.any { it in c.name.lowercase() } }?.let { return it.id }
        }
        return null
    }
}
