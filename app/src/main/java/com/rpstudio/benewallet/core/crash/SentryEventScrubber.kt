package com.rpstudio.benewallet.core.crash

import io.sentry.SentryEvent

/**
 * Exception messages can carry user data (e.g. a serialization error quotes the JSON it failed to parse, which here
 * is balance and transactions). The stack trace is what makes an event actionable, so only the message is dropped.
 */
internal fun SentryEvent.scrubbed(): SentryEvent {
    exceptions?.forEach { it.value = it.type }
    return this
}
