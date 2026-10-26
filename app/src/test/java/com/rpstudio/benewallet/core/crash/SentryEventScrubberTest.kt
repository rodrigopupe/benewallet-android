package com.rpstudio.benewallet.core.crash

import io.sentry.SentryEvent
import io.sentry.protocol.SentryException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SentryEventScrubberTest {
    private fun exception(type: String, value: String) = SentryException().apply {
        this.type = type
        this.value = value
    }

    @Test
    fun `exception messages are replaced by the exception type`() {
        val event = SentryEvent().apply {
            exceptions = listOf(
                exception("SerializationException", """Unexpected JSON token, JSON input: {"balanceCents": 123456}"""),
                exception("IllegalStateException", "balance of user 42")
            )
        }

        val scrubbed = event.scrubbed()

        assertEquals(
            listOf("SerializationException", "IllegalStateException"),
            scrubbed.exceptions?.map { it.value }
        )
        assertFalse(
            "no fragment of the payload may survive",
            scrubbed.exceptions.orEmpty().any { it.value.orEmpty().contains("123456") }
        )
    }

    @Test
    fun `the stack trace data is kept`() {
        val stacktrace = io.sentry.protocol.SentryStackTrace()
        val event = SentryEvent().apply {
            exceptions = listOf(exception("IOException", "secret").apply { this.stacktrace = stacktrace })
        }

        assertNotNull(event.scrubbed().exceptions?.single()?.stacktrace)
    }

    @Test
    fun `an event without exceptions is returned as it is`() {
        assertNull(SentryEvent().scrubbed().exceptions)
    }
}
