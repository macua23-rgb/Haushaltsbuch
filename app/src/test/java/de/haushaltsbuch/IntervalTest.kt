package de.haushaltsbuch

import de.haushaltsbuch.data.Interval
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class IntervalTest {
    @Test fun weekly() = assertEquals(LocalDate.of(2026, 11, 28), Interval.WEEKLY.next(LocalDate.of(2026, 11, 21)))

    @Test fun monthlyOnThe31stDoesNotDriftToThe28th() {
        var d = LocalDate.of(2026, 1, 31)
        val seen = mutableListOf<LocalDate>()
        repeat(4) { d = Interval.MONTHLY.next(d, 31); seen += d }
        assertEquals(
            listOf(LocalDate.of(2026, 2, 28), LocalDate.of(2026, 3, 31), LocalDate.of(2026, 4, 30), LocalDate.of(2026, 5, 31)),
            seen
        )
    }

    @Test fun monthlyAcrossYearEnd() = assertEquals(LocalDate.of(2027, 1, 15), Interval.MONTHLY.next(LocalDate.of(2026, 12, 15)))

    @Test fun yearlyOnLeapDayReturnsToFeb29() {
        var d = LocalDate.of(2028, 2, 29)
        d = Interval.YEARLY.next(d, 29); assertEquals(LocalDate.of(2029, 2, 28), d)
        repeat(3) { d = Interval.YEARLY.next(d, 29) }
        assertEquals(LocalDate.of(2032, 2, 29), d)
    }
}
