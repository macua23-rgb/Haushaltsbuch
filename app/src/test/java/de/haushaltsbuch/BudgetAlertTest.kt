package de.haushaltsbuch

import de.haushaltsbuch.notify.alertLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetAlertTest {
    @Test fun belowEightyPercentIsQuiet() = assertEquals(0, alertLevel(7_999, 10_000))
    @Test fun exactlyEightyPercentWarns() = assertEquals(80, alertLevel(8_000, 10_000))
    @Test fun justBelowLimitStaysAtEighty() = assertEquals(80, alertLevel(9_999, 10_000))
    @Test fun atLimitIsHundred() = assertEquals(100, alertLevel(10_000, 10_000))
    @Test fun overLimitIsHundred() = assertEquals(100, alertLevel(25_000, 10_000))
    @Test fun noLimitNeverAlerts() { assertEquals(0, alertLevel(5_000, 0)); assertEquals(0, alertLevel(5_000, -1)) }
    @Test fun noOverflowWithLargeAmounts() = assertEquals(80, alertLevel(8_000_000_000_000L, 10_000_000_000_000L))
}
