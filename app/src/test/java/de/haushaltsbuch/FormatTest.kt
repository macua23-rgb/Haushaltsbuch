package de.haushaltsbuch

import de.haushaltsbuch.ui.*
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {
    @Test fun parseCentsHandlesCommaDotAndShortInput() {
        assertEquals(1250, parseCents("12,5"))
        assertEquals(1250, parseCents("12.50"))
        assertEquals(1200, parseCents("12"))
        assertEquals(5, parseCents("0,05"))
        assertEquals(0, parseCents(""))
        assertEquals(1234, parseCents("12,345"))   // mehr als 2 Stellen werden abgeschnitten
    }

    @Test fun parseSignedCents() {
        assertEquals(-5050, parseSignedCents("-50,5"))
        assertEquals(5050, parseSignedCents("50,5"))
    }

    @Test fun centsToInputRoundTrips() {
        listOf(0L, 5L, 50L, 1200L, 1205L, 1250L, 99999L, -5050L, -5000L).forEach {
            assertEquals(it, parseSignedCents(centsToInput(it)))
        }
        assertEquals("12,5", centsToInput(1250))
        assertEquals("12", centsToInput(1200))
        assertEquals("-50,5", centsToInput(-5050))
    }

    @Test fun numpadRules() {
        assertEquals("0,", amountKey("", ","))
        assertEquals("1,", amountKey("1", ","))
        assertEquals("1,", amountKey("1,", ","))          // nur ein Komma
        assertEquals("1,25", amountKey("1,25", "7"))      // max. 2 Nachkommastellen
        assertEquals("12", amountKey("123", "⌫"))
        assertEquals("", amountKey("", "⌫"))
        assertEquals("123456789", amountKey("123456789", "1"))   // Längenbegrenzung
    }

    @Test fun moneyUsesGermanFormat() {
        val s = 123456L.money("EUR")
        assertEquals(true, s.contains("1.234,56"))
    }
}
