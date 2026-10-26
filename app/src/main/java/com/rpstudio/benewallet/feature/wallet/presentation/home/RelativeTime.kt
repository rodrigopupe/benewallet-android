package com.rpstudio.benewallet.feature.wallet.presentation.home

import java.time.Duration
import java.time.Instant

private val ONE_MINUTE: Duration = Duration.ofMinutes(1)
private val ONE_HOUR: Duration = Duration.ofHours(1)
private val ONE_DAY: Duration = Duration.ofDays(1)

/** How long ago something happened, without any wording: the wording lives in the string resources. */
internal sealed interface RelativeTime {
    object JustNow : RelativeTime

    data class Minutes(val count: Int) : RelativeTime

    data class Hours(val count: Int) : RelativeTime

    data class Days(val count: Int) : RelativeTime
}

internal fun relativeTime(updatedAt: Instant, now: Instant): RelativeTime {
    val elapsed = Duration.between(updatedAt, now)
    return when {
        elapsed < ONE_MINUTE -> RelativeTime.JustNow
        elapsed < ONE_HOUR -> RelativeTime.Minutes(elapsed.toMinutes().toInt())
        elapsed < ONE_DAY -> RelativeTime.Hours(elapsed.toHours().toInt())
        else -> RelativeTime.Days(elapsed.toDays().toInt())
    }
}
