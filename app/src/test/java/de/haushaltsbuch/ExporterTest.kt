package de.haushaltsbuch

import de.haushaltsbuch.data.*
import de.haushaltsbuch.export.ExportRow
import de.haushaltsbuch.export.Exporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ExporterTest {
    private fun row(type: TxType, cents: Long, note: String = "", cat: String = "Essen", cur: String = "EUR", base: Long = cents) =
        ExportRow(LocalDate.of(2026, 11, 5), type, cat, "Giro", "", note, "", cents, cur, base)

    @Test fun decimalFormatting() {
        assertEquals("12,50", Exporter.decimal(1250)); assertEquals("-0,05", Exporter.decimal(-5)); assertEquals("0,00", Exporter.decimal(0))
    }

    @Test fun cellEscapesSeparatorsQuotesAndNewlines() {
        assertEquals("plain", Exporter.cell("plain"))
        assertEquals("\"a;b\"", Exporter.cell("a;b"))
        assertEquals("\"say \"\"hi\"\"\"", Exporter.cell("say \"hi\""))
        assertEquals("\"a\nb\"", Exporter.cell("a\nb"))
    }

    @Test fun cellNeutralizesSpreadsheetFormulas() {
        assertEquals("'=SUM(A1)", Exporter.cell("=SUM(A1)"))
        assertEquals("'+1", Exporter.cell("+1")); assertEquals("'-1", Exporter.cell("-1")); assertEquals("'@x", Exporter.cell("@x"))
    }

    @Test fun expensesAreNegativeInCsv() {
        val csv = Exporter.csv(listOf(row(TxType.EXPENSE, 1250, "Markt"), row(TxType.INCOME, 5000)))
        val lines = csv.trimEnd().split("\r\n")
        assertEquals(3, lines.size)
        assertTrue(lines[0].startsWith("Datum;Typ;Kategorie"))
        assertEquals("2026-11-05;Ausgabe;Essen;Giro;;Markt;;-12,50;EUR;-12,50", lines[1])
        assertEquals("2026-11-05;Einnahme;Essen;Giro;;;;50,00;EUR;50,00", lines[2])
    }

    @Test fun csvKeepsForeignCurrencyAndBaseAmount() {
        val line = Exporter.csv(listOf(row(TxType.EXPENSE, 1000, cur = "USD", base = 920))).trimEnd().split("\r\n")[1]
        assertTrue(line.endsWith("-10,00;USD;-9,20"))
    }

    @Test fun csvStartsWithUtf8BomWhenWritten() {
        val out = java.io.ByteArrayOutputStream()
        Exporter.writeCsv(listOf(row(TxType.INCOME, 100, cat = "Gehalt über")), out)
        val bytes = out.toByteArray()
        assertEquals(0xEF.toByte(), bytes[0]); assertEquals(0xBB.toByte(), bytes[1]); assertEquals(0xBF.toByte(), bytes[2])
        assertTrue(String(bytes, 3, bytes.size - 3, Charsets.UTF_8).contains("Gehalt über"))
    }

    @Test fun rowsAreSortedAndResolveNames() {
        val acc = mapOf(1L to Account(1, "Giro", AccountType.CHECKING, "🏦", currency = "USD"), 2L to Account(2, "Karte", AccountType.CREDIT_CARD, "💳"))
        val cats = mapOf(7L to Category(7, "Essen", "🍽️", 0, TxType.EXPENSE))
        val fx = Fx("EUR", mapOf("USD" to 0.5), mapOf(1L to "USD", 2L to "EUR"))
        val b1 = Booking(id = 2, type = TxType.EXPENSE, amountCents = 200, date = LocalDate.of(2026, 1, 2), accountId = 1, categoryId = 7)
        val b2 = Booking(id = 1, type = TxType.TRANSFER, amountCents = 100, date = LocalDate.of(2026, 1, 1), accountId = 1, toAccountId = 2)
        val r = Exporter.rows(listOf(b1, b2), cats, acc, fx)
        assertEquals(listOf(TxType.TRANSFER, TxType.EXPENSE), r.map { it.type })
        assertEquals("Karte", r[0].toAccount)
        assertEquals("Essen", r[1].category)
        assertEquals(100, r[1].baseCents)     // 200 USD-Cent * 0,5
        assertEquals("USD", r[1].currency)
    }
}
