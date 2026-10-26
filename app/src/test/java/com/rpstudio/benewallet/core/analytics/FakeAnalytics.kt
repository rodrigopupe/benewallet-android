package com.rpstudio.benewallet.core.analytics

class FakeAnalytics : Analytics {
    val events = mutableListOf<AnalyticsEvent>()

    override fun track(event: AnalyticsEvent) {
        events.add(event)
    }
}
