package de.haushaltsbuch

import de.haushaltsbuch.data.Booking
import de.haushaltsbuch.data.Category
import de.haushaltsbuch.data.TxType
import de.haushaltsbuch.scan.CategoryGuesser
import de.haushaltsbuch.scan.ReceiptParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import java.time.LocalDate
import org.junit.Test

class ReceiptScanTest {
    private val today = LocalDate.of(2026, 11, 24)
    private val bon = """
        REWE Markt GmbH
        Musterstraße 12
        Tel 0123 456
        Bio Milch 1L        1,29
        Brot                2,49
        Pfand               0,25
        Zwischensumme      14,43
        SUMME EUR          18,40
        Gegeben Bar        20,00
        Rückgeld            1,60
        MwSt 7% 1,20
        24.11.2026 17:42 Uhr
    """.trimIndent()

    @Test fun totalIsSummeNotChangeOrSubtotal() {
        assertEquals(1840L, ReceiptParser.parse(bon, today).amountCents)
    }

    @Test fun dateAndMerchant() {
        val r = ReceiptParser.parse(bon, today)
        assertEquals(LocalDate.of(2026, 11, 24), r.date)
        assertEquals("REWE Markt GmbH", r.merchant)
    }

    @Test fun amountOnNextLineAndThousands() {
        assertEquals(123456L, ReceiptParser.parse("Aral\nGesamtbetrag\n1.234,56 EUR", today).amountCents)
    }

    @Test fun fallbackIsLargestAmount() {
        assertEquals(4500L, ReceiptParser.parse("Laden\nA 12,00\nB 45,00", today).amountCents)
    }

    @Test fun futureAndOldDatesAreIgnored() {
        assertNull(ReceiptParser.parse("x\n01.01.2030", today).date)
        assertEquals(LocalDate.of(2026, 3, 5), ReceiptParser.parse("5.3.26", today).date)
    }

    @Test fun garbageGivesNothing() {
        val r = ReceiptParser.parse("", today)
        assertNull(r.amountCents); assertNull(r.date); assertNull(r.merchant)
    }

    private val cats = listOf(
        Category(1, "Wohnen", "", 0, TxType.EXPENSE), Category(2, "Lebensmittel", "", 0, TxType.EXPENSE),
        Category(3, "Mobilität", "", 0, TxType.EXPENSE), Category(4, "Gehalt", "", 0, TxType.INCOME)
    )

    @Test fun keywordGuess() {
        assertEquals(2L, CategoryGuesser.guess("REWE Markt", "", emptyList(), cats))
        assertEquals(3L, CategoryGuesser.guess("Aral Tankstelle", "", emptyList(), cats))
        assertNull(CategoryGuesser.guess("Unbekannt GmbH", "", emptyList(), cats))
    }

    @Test fun learnedMerchantBeatsKeywords() {
        val h = listOf(
            Booking(type = TxType.EXPENSE, amountCents = 100, date = today, accountId = 1, categoryId = 1, note = "Rewe Nachbarschaftsfest"),
            Booking(type = TxType.EXPENSE, amountCents = 100, date = today, accountId = 1, categoryId = 1, note = "rewe getränke")
        )
        assertEquals(1L, CategoryGuesser.guess("REWE Markt", "", h, cats))
    }
}
