package com.rpstudio.benewallet.core.analytics

import org.junit.Assert.assertEquals
import org.junit.Test

class AnalyticsEventTest {

    @Test
    fun `statement filter event exposes category and period`() {
        val event = AnalyticsEvent.StatementFilterApplied(category = "restaurant", period = "month")

        assertEquals("statement_filter_applied", event.name)
        assertEquals(mapOf("category" to "restaurant", "period" to "month"), event.params)
    }

    @Test
    fun `statement filter event defaults category to all`() {
        val event = AnalyticsEvent.StatementFilterApplied(category = null, period = "week")

        assertEquals("all", event.params["category"])
    }
}
