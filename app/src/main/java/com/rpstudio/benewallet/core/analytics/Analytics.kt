package com.rpstudio.benewallet.core.analytics

interface Analytics {
    fun track(event: AnalyticsEvent)
}
