package com.rpstudio.benewallet.core.analytics

import android.util.Log
import com.rpstudio.benewallet.BuildConfig

internal class LogcatAnalytics : Analytics {
    override fun track(event: AnalyticsEvent) {
        if (BuildConfig.DEBUG.not()) return

        Log.d(TAG, "event=${event.name} params=${event.params}")
    }

    private companion object {
        const val TAG = "Analytics"
    }
}
