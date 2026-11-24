package ru.rufp.weather.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class TimeParsersTest {

    @Test
    fun openMeteoTimeAppliesUtcOffset() {
        val instant = TimeParsers.openMeteoTime("2026-11-14T12:00", utcOffsetSeconds = 3 * 3600)
        assertEquals(Instant.parse("2026-11-14T09:00:00Z"), instant)
    }

    @Test
    fun openMeteoTimeNegativeOffset() {
        val instant = TimeParsers.openMeteoTime("2026-11-14T12:00", utcOffsetSeconds = -5 * 3600)
        assertEquals(Instant.parse("2026-11-14T17:00:00Z"), instant)
    }

    @Test
    fun metNoTimeParsesIsoInstant() {
        assertEquals(
            Instant.parse("2026-11-14T12:00:00Z"),
            TimeParsers.metNoTime("2026-11-14T12:00:00Z")
        )
    }
}
