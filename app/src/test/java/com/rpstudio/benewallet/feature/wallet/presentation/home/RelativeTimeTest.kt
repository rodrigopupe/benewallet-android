package com.rpstudio.benewallet.feature.wallet.presentation.home

import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class RelativeTimeTest {
    private val now = Instant.parse("2026-10-26T15:00:00Z")

    private fun after(duration: Duration) = relativeTime(now.minus(duration), now)

    @Test
    fun `under a minute is just now`() {
        assertEquals(RelativeTime.JustNow, after(Duration.ZERO))
        assertEquals(RelativeTime.JustNow, after(Duration.ofSeconds(59)))
    }

    @Test
    fun `a minute is where minutes start`() {
        assertEquals(RelativeTime.Minutes(1), after(Duration.ofSeconds(60)))
        assertEquals(RelativeTime.Minutes(59), after(Duration.ofMinutes(59).plusSeconds(59)))
    }

    @Test
    fun `an hour is where hours start`() {
        assertEquals(RelativeTime.Hours(1), after(Duration.ofMinutes(60)))
        assertEquals(RelativeTime.Hours(23), after(Duration.ofHours(23).plusMinutes(59)))
    }

    @Test
    fun `a day is where days start`() {
        assertEquals(RelativeTime.Days(1), after(Duration.ofHours(24)))
        assertEquals(RelativeTime.Days(3), after(Duration.ofDays(3).plusHours(5)))
    }

    @Test
    fun `a moment in the future counts as just now`() {
        assertEquals(RelativeTime.JustNow, relativeTime(now.plusSeconds(90), now))
    }
}
