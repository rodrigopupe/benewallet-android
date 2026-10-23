package com.rpstudio.benewallet.core.crash

import android.content.Context
import android.util.Log
import com.rpstudio.benewallet.BuildConfig
import io.sentry.Sentry
import io.sentry.android.core.SentryAndroid

internal class SentryCrashReporter(
    private val context: Context,
    private val dsn: String,
) : CrashReporter {
    override fun initialize() {
        if (!dsn.startsWith("https://")) return

        runCatching {
            SentryAndroid.init(context) { options ->
                options.dsn = dsn
                options.environment = if (BuildConfig.DEBUG) DEBUG_ENVIRONMENT else RELEASE_ENVIRONMENT
            }
        }.onFailure { error ->
            Log.w("CrashReporter", "Sentry initialization failed", error)
        }
    }

    override fun recordException(throwable: Throwable) {
        Sentry.captureException(throwable)
    }

    override fun log(message: String) {
        Sentry.addBreadcrumb(message)
    }

    private companion object {
        const val DEBUG_ENVIRONMENT = "debug"
        const val RELEASE_ENVIRONMENT = "release"
    }
}