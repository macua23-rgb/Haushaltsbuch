package de.haushaltsbuch

import de.haushaltsbuch.data.Fx
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FxTest {
    private val fx = Fx("EUR", mapOf("USD" to 0.92, "GBP" to 1.17), mapOf(1L to "EUR", 2L to "USD"))

    @Test fun baseCurrencyIsIdentity() = assertEquals(1000, fx.toBase(1000, "EUR"))
    @Test fun convertsToBaseWithRounding() = assertEquals(920, fx.toBase(1000, "USD"))
    @Test fun roundsToNearestCent() = assertEquals(1, fx.toBase(1, "USD"))   // 0,92 -> 1
    @Test fun convertBetweenForeignCurrencies() = assertEquals(Math.round(1000 * 0.92 / 1.17), fx.convert(1000, "USD", "GBP"))
    @Test fun convertToBaseAndBack() = assertEquals(1000, fx.convert(fx.convert(1000, "EUR", "USD"), "USD", "EUR"))
    @Test fun knowsWhichRatesExist() {
        assertTrue(fx.hasRate("EUR")); assertTrue(fx.hasRate("USD")); assertFalse(fx.hasRate("CHF"))
    }
    @Test fun missingRateFallsBackToOne() = assertEquals(1000, fx.toBase(1000, "CHF"))
    @Test fun accountCurrencyLookup() {
        assertEquals("USD", fx.currencyOf(2L)); assertEquals("EUR", fx.currencyOf(99L))
    }
}
